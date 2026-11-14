"""Ejecuta los cinco fallos con los clientes Android reales y guarda evidencia sin JWT.
Requiere APK debug instalada, emulador y API compilada. Restaura la red al terminar.
"""
import json
import re
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path
from smoke_android_crud import adb, android_login, fill, nodes, tap, tap_node, snap

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/evidencias'
PACKAGE = 'com.melvinpena.comparativa'
server = None
rows = []

def start_api():
    global server
    with (OUT/'api-desarrollo.log').open('a') as log:
        server = subprocess.Popen(['dotnet','run','--no-build','--project','backend/Bitacora.Api','--',
            '--urls','http://localhost:5000','--environment','Development'], cwd=ROOT,
            stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    for _ in range(100):
        if server.poll() is not None: raise RuntimeError('No arranca la API; revisar log local.')
        try:
            urllib.request.urlopen('http://localhost:5000/api/registros', timeout=1)
        except urllib.error.HTTPError as e:
            if e.code == 401: return
        except urllib.error.URLError: pass
        time.sleep(.2)
    raise RuntimeError('API no disponible')

def scroll_top():
    for _ in range(3): adb('shell','input','swipe','700','900','700','2600','150')

def center(n):
    b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
    return ((b[0]+b[2])//2,(b[1]+b[3])//2)

def visible(n):
    b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
    return len(b)==4 and b[2]>b[0] and b[3]>b[1] and b[1]>100 and b[3]<2940

def select_scenario(index):
    scroll_top()
    for _ in range(5):
        found=[n for n in nodes() if n.get('class')=='android.widget.Spinner' and visible(n)]
        if found: break
        adb('shell','input','swipe','700','2400','700','1300','200')
    else: raise AssertionError('Selector no visible')
    tap_node(found[0])
    labels=['Listado normal','Tiempo de espera (demora','Error interno (HTTP 500)']
    tap(labels[index], exact=False)

def press(client):
    for _ in range(7):
        ns=nodes()
        label=next((n for n in ns if n.get('text')==client and visible(n)),None)
        buttons=[n for n in ns if n.get('text','').casefold()=='probar' and visible(n)]
        if label is not None and buttons:
            button=min(buttons,key=lambda n: abs(center(n)[1]-center(label)[1]))
            if abs(center(button)[1]-center(label)[1])<200:
                tap_node(button); return
        adb('shell','input','swipe','700','2500','700','1200','200')
    raise AssertionError('Botón no visible: '+client)

def execute(scenario, client, expected):
    adb('logcat','-c')
    press(client)
    deadline=time.monotonic()+35
    line=None
    while time.monotonic()<deadline:
        logs=adb('logcat','-d','-s','ComparativaHTTP:I','*:S').decode()
        line=next((s for s in logs.splitlines() if ' | '+client+':' in s),None)
        if line: break
        time.sleep(.4)
    assert line and expected in line, f'{scenario}/{client}: resultado inesperado: {line}'
    for _ in range(5):
        if any(n.get('text','').startswith(client+':') for n in nodes()): break
        adb('shell','input','swipe','700','2500','700','1100','200')
    else: raise AssertionError('Resultado no visible')
    resumed=adb('shell','dumpsys','activity','activities').decode()
    assert PACKAGE+'/.ComparativaActivity' in resumed
    assert adb('shell','pidof',PACKAGE).strip()==app_pid, 'El proceso cambió durante la prueba'
    name='lab-'+scenario+'-'+client.lower()
    snap(name)
    (OUT/(name+'.txt')).write_text(line+'\nProceso Android conservado; resultado visible en laboratorio.\n')
    message=line.split(' | ',2)[-1]
    rows.append({'escenario':scenario,'cliente':client,'logcat':line,'mensaje':message,'captura':name+'.png','sin_cierre':True})
    (OUT/'laboratorio-fallos.json').write_text(json.dumps(rows,indent=2,ensure_ascii=False))
    print('PASS:',scenario,client,message,flush=True)

def write_report():
    labels = {'error-500': 'HTTP 500', 'timeout': 'Timeout', 'api-detenida': 'API detenida',
              'modo-avion': 'Modo avión', 'sin-token-401': 'JWT ausente (401)'}
    text = ["# Pruebas de fallos del laboratorio — Tarea 2.5", "",
        "Ejecución: 12 de noviembre de 2026. Pixel 4 XL emulado, APK debug y API local Development.",
        "Timeout configurado: 2000 ms; demora del servidor: 15 segundos con await Task.Delay.",
        "JWT válido de sesión en todos los escenarios salvo el caso de token ausente.",
        "Los tiempos son de solicitudes fallidas; no se utilizan como promedios de rendimiento.",
        "La hora de Logcat corresponde al reloj del emulador.", "",
        "| Escenario | Cliente | Error técnico | Mensaje visible y duración | Sin cierre | Evidencia |",
        "| --- | --- | --- | --- | --- | --- |"]
    for row in rows:
        diagnostic = row['logcat'].split(' | ')[1]
        name = row['captura'][:-4]
        text.append(f"| {labels[row['escenario']]} | {row['cliente']} | {diagnostic} | {row['mensaje']} | Sí | [Captura]({row['captura']}), [Logcat]({name}.txt) |")
    text += ["", "Se comprobó resultado visible, actividad de laboratorio y mismo PID durante las pruebas.",
        "API detenida: se detuvo únicamente el proceso iniciado por el script y se reinició después.",
        "Modo avión: Wi-Fi y datos desactivados; se esperó hasta no tener red predeterminada activa.",
        "Ver [estado de red](laboratorio-modo-avion-red.txt). La conexión fue restaurada al terminar.",
        "Los tres casos 401 tras restaurar la red también verifican que la API vuelve a ser accesible.",
        "No se crearon, modificaron ni eliminaron registros del usuario.", "",
        "Reproducción: con API compilada, puerto 5000 libre, APK debug instalada y emulador activo,",
        "ejecutar `python3 scripts/verify_lab_failures.py`. El script deja su API activa."]
    (OUT/'LABORATORIO_FALLOS.md').write_text('\n'.join(text)+'\n')

if __name__=='__main__':
    # El script solo detiene el proceso de API que él mismo inicia.
    import os
    os.chdir(ROOT)
    if subprocess.run(['lsof','-t','-iTCP:5000','-sTCP:LISTEN'],capture_output=True).stdout.strip():
        raise RuntimeError('Puerto 5000 ocupado: no se detiene un proceso ajeno a esta ejecución.')
    airplane=adb('shell','settings','get','global','airplane_mode_on').decode().strip()
    wifi=adb('shell','settings','get','global','wifi_on').decode().strip()
    mobile=adb('shell','settings','get','global','mobile_data').decode().strip()
    try:
        start_api()
        adb('shell','am','force-stop',PACKAGE)
        adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
        android_login()
        tap('Comenzar comparación   →')
        app_pid=adb('shell','pidof',PACKAGE).strip()
        scroll_top()
        fill(2,'2000')
        for scenario,index,expected in [('error-500',2,'(500)'),('timeout',1,'Tiempo de espera agotado')]:
            select_scenario(index)
            for client in ['Volley','OkHttp','Retrofit']: execute(scenario,client,expected)
        select_scenario(0)
        server.terminate(); server.wait(timeout=15)
        for client in ['Volley','OkHttp','Retrofit']: execute('api-detenida',client,'API')
        start_api()
        adb('shell','cmd','connectivity','airplane-mode','enable')
        adb('shell','svc','wifi','disable')
        adb('shell','svc','data','disable')
        assert adb('shell','settings','get','global','airplane_mode_on').strip()==b'1'
        for _ in range(30):
            network=adb('shell','dumpsys','connectivity').decode()
            if 'Active default network: none' in network: break
            time.sleep(.5)
        else: raise AssertionError('Todavía existe una red activa en modo avión')
        (OUT/'laboratorio-modo-avion-red.txt').write_text(network)
        for client in ['Volley','OkHttp','Retrofit']: execute('modo-avion',client,'API')
        adb('shell','cmd','connectivity','airplane-mode','disable')
        adb('shell','svc','wifi','enable')
        adb('shell','svc','data','enable')
        time.sleep(3)
        scroll_top(); fill(1,'')
        for client in ['Volley','OkHttp','Retrofit']: execute('sin-token-401',client,'(401)')
    finally:
        adb('shell','cmd','connectivity','airplane-mode','enable' if airplane=='1' else 'disable')
        adb('shell','svc','wifi','enable' if wifi in ('1','2') else 'disable')
        adb('shell','svc','data','enable' if mobile=='1' else 'disable')
        print('Red restaurada. API iniciada por el script queda activa.',flush=True)

    write_report()

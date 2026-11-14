"""Cinco GET exitosos por cliente Android; no cambia datos ni imprime JWT."""
import hashlib
import json
import os
import re
import time
from pathlib import Path
from statistics import mean
from smoke_android_crud import adb, android_login, fill, nodes, records, snap, tap
from verify_lab_failures import press, scroll_top, select_scenario

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/evidencias'
PACKAGE = 'com.melvinpena.comparativa'
CLIENTS = ['Volley', 'OkHttp', 'Retrofit']

def fingerprint(data):
    return hashlib.sha256(json.dumps(data, sort_keys=True).encode()).hexdigest()

def main():
    os.chdir(ROOT)
    assert adb('shell','settings','get','global','airplane_mode_on').strip()==b'0'
    before=records()
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
    android_login()
    tap('Comenzar comparación   →')
    pid=adb('shell','pidof',PACKAGE).strip()
    scroll_top(); fill(2,'2000'); select_scenario(0)
    samples=[]
    for iteration in range(1,6):
        for client in CLIENTS:
            adb('logcat','-c')
            press(client)
            deadline=time.monotonic()+20
            line=None
            while time.monotonic()<deadline:
                logs=adb('logcat','-d','-s','ComparativaHTTP:I','*:S').decode()
                line=next((s for s in logs.splitlines() if ' | '+client+':' in s),None)
                if line: break
                time.sleep(.2)
            assert line and 'NORMAL | success |' in line, f'{client}: consulta fallida, medición descartada'
            match=re.search(r': (\d+) registros recibidos en (\d+) ms\.',line)
            assert match and int(match[1])==len(before),'Cantidad inesperada'
            assert adb('shell','pidof',PACKAGE).strip()==pid,'Proceso Android cambió'
            for _ in range(4):
                if any(n.get('text','').startswith(client+':') for n in nodes()): break
                adb('shell','input','swipe','700','2500','700','1100','200')
            else: raise AssertionError('No se ve el resultado')
            name=f'lab-medicion-{client.lower()}-{iteration}'
            snap(name)
            (OUT/(name+'.txt')).write_text(line+'\n')
            samples.append(dict(orden=len(samples)+1,ejecucion=iteration,cliente=client,
                registros=int(match[1]),duracion_ms=int(match[2]),logcat=line,captura=name+'.png'))
            (OUT/'laboratorio-mediciones.json').write_text(json.dumps(samples,ensure_ascii=False,indent=2))
            print(f'PASS {len(samples)}/15: {client} #{iteration}: {match[1]} registros, {match[2]} ms',flush=True)
    after=records()
    assert fingerprint(before)==fingerprint(after),'Los datos cambiaron durante las mediciones'
    report=['# Mediciones exitosas — Tarea 2.5','',
        'Fecha: 12 de noviembre de 2026. Pixel 4 XL emulado; APK debug; API local Development.',
        'GET /api/registros en http://10.0.2.2:5000/, JWT válido de sesión y timeout de 2000 ms.',
        f'Conjunto de datos: {len(before)} registros, contenido idéntico antes y después (verificación SHA-256 en memoria).',
        'No se añadieron, editaron ni eliminaron registros para estas mediciones.',
        'Un listado vacío mide la respuesta y conversión del arreglo vacío; no permite evaluar el costo de convertir volúmenes de objetos.',
        'Wi-Fi del emulador activo y modo avión desactivado; no se alteró la red durante la serie.',
        'Orden: cinco rondas Volley → OkHttp → Retrofit, una solicitud a la vez.',
        'Sin consultas de calentamiento del listado en este proceso. El login Retrofit ocurre antes de medir.',
        'La API y el emulador ya habían ejecutado pruebas previas; no representan un arranque frío del servidor.',
        'No se descartó ninguna muestra exitosa ni se incluyeron fallos en los promedios.',
        'Duración monotónica medida por la app: preparación, red, conversión y entrega a UI.',
        'Volley reutiliza cola; OkHttp y Retrofit construyen cliente por consulta. No se extrapola a otras implementaciones.',
        'Sin caché de aplicación y sin reintentos configurados; los tres envían Cache-Control: no-cache, no-store.',
        'La automatización toma capturas entre solicitudes; no es una prueba de carga ni de concurrencia.',
        'Las marcas horarias de Logcat corresponden al reloj del emulador.','',
        '| Cliente | t1 (ms) | t2 (ms) | t3 (ms) | t4 (ms) | t5 (ms) | Promedio (ms) |',
        '| --- | --- | --- | --- | --- | --- | --- |']
    for client in CLIENTS:
        values=[s['duracion_ms'] for s in samples if s['cliente']==client]
        report.append('| '+client+' | '+' | '.join(map(str,values))+f' | {mean(values):.2f} |')
    report+=['','Promedio = suma de las cinco duraciones / 5.','',
        '| Orden | Cliente | Ejecución | Registros | Duración (ms) | Evidencia |',
        '| --- | --- | --- | --- | --- | --- |']
    for s in samples:
        name=s['captura'][:-4]
        report.append(f"| {s['orden']} | {s['cliente']} | {s['ejecucion']} | {s['registros']} | {s['duracion_ms']} | [Captura]({s['captura']}), [Logcat]({name}.txt) |")
    report+=['','Reproducción: API local y emulador activos, APK debug instalada, credenciales locales disponibles:',
        '`python3 scripts/measure_lab_clients.py`. Genera JSON, capturas, Logcat y este informe.',
        'La aplicación permaneció en el laboratorio con el mismo PID durante las 15 consultas.']
    (OUT/'LABORATORIO_MEDICIONES.md').write_text('\n'.join(report)+'\n')
    print('15 muestras y promedios guardados; datos sin cambios.',flush=True)

if __name__=='__main__':
    main()

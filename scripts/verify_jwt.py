"""Build and verify local JWT integration. No tokens/passwords in reports.
Restarts only this project's API, keeps it running and uses the existing emulator.
"""
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path
import signal
import subprocess
import time
import urllib.error
import urllib.request
from smoke_android_crud import adb, android_login, api_token, find, fill, lab_credentials, nodes, tap, tap_node

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
OUT = ROOT / 'docs/evidencias'
OUT.mkdir(parents=True, exist_ok=True)

def run(command):
    print('Ejecutando:', ' '.join(command), flush=True)
    subprocess.run(command, check=True)

def call(method, path, token=None, body=None):
    headers = {}
    if token is not None:
        headers['Authorization'] = 'Bearer ' + token
    if body is not None:
        headers['Content-Type'] = 'application/json'
    request = urllib.request.Request('http://localhost:5000' + path, method=method,
        data=json.dumps(body).encode() if body is not None else None, headers=headers)
    try:
        response = urllib.request.urlopen(request, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        content = response.read()
        return response.status, response.headers, json.loads(content) if content else None

run(['dotnet', 'build', 'backend/Bitacora.sln'])
run(['./gradlew', ':app:installDebug'])
print('Reiniciando solo la API de este proyecto...', flush=True)
existing = subprocess.run(['lsof', '-t', '-iTCP:5000', '-sTCP:LISTEN'], capture_output=True, text=True)
for value in set(existing.stdout.split()):
    pid = int(value)
    command = subprocess.check_output(['ps', '-p', str(pid), '-o', 'comm='], text=True).strip()
    if not command.endswith('/Bitacora.Api'):
        raise RuntimeError('Puerto 5000 ocupado por otro servicio; no se detiene.')
    os.kill(pid, signal.SIGTERM)
    for _ in range(50):
        try: os.kill(pid, 0)
        except ProcessLookupError: break
        time.sleep(.1)
with (OUT / 'api-desarrollo.log').open('a') as log:
    server = subprocess.Popen(['dotnet', 'run', '--no-build', '--project', 'backend/Bitacora.Api', '--',
        '--urls', 'http://localhost:5000', '--environment', 'Development'], stdout=log, stderr=subprocess.STDOUT,
        start_new_session=True)
for _ in range(100):
    if server.poll() is not None:
        raise RuntimeError('La API no arrancó. Revisar el log local del servidor.')
    try:
        status, _, _ = call('GET', '/api/registros')
        if status == 401: break
        raise AssertionError('El endpoint no exige JWT')
    except urllib.error.URLError: time.sleep(.2)
else: raise RuntimeError('La API no respondió a tiempo')

checks = []
def expect(label, status, expected):
    assert status == expected, f'{label}: esperado {expected}, recibido {status}'
    checks.append(f'PASS: {label} -> HTTP {status}')

for method, path, body in [
    ('GET','/api/registros',None), ('GET','/api/registros/1',None),
    ('POST','/api/registros',{}), ('PUT','/api/registros/1',{}), ('DELETE','/api/registros/1',None),
    ('GET','/api/pruebas/error',None), ('GET','/api/pruebas/demora',None)]:
    status, headers, _ = call(method, path, body=body)
    expect(method + ' ' + path + ' sin JWT',status,401)
    assert 'Bearer' in headers.get('WWW-Authenticate','')
credentials = lab_credentials()
status, _, _ = call('POST','/api/auth/login',body=dict(credentials,password='incorrecta-para-prueba'))
expect('Credenciales incorrectas',status,401)
status, _, login = call('POST','/api/auth/login',body=credentials)
expect('Login válido',status,200)
token = login['accessToken']
status, _, _ = call('GET','/api/registros',token)
expect('Listado con JWT válido',status,200)
status, _, _ = call('GET','/api/registros','no-es-jwt')
expect('JWT malformado',status,401)

head, payload, signature = token.split('.')
def encode(value):
    return base64.urlsafe_b64encode(json.dumps(value,separators=(',',':')).encode()).rstrip(b'=').decode()
def decode(value):
    return json.loads(base64.urlsafe_b64decode(value+'='*(-len(value)%4)))
claims = decode(payload)
altered = dict(claims,sub='usuario-alterado')
status, _, _ = call('GET','/api/registros',head+'.'+encode(altered)+'.'+signature)
expect('JWT con contenido alterado',status,401)
key = json.loads(Path('backend/Bitacora.Api/.dev-auth.json').read_text())['Jwt']['SigningKey'].encode()
def signed(changes):
    body = head+'.'+encode(dict(claims,**changes))
    sig = base64.urlsafe_b64encode(hmac.new(key,body.encode(),hashlib.sha256).digest()).rstrip(b'=').decode()
    return body+'.'+sig
for label, changes in [
    ('JWT vencido',{'iat':int(time.time())-120,'nbf':int(time.time())-120,'exp':int(time.time())-60}),
    ('JWT emisor incorrecto',{'iss':'otro-emisor'}),
    ('JWT audiencia incorrecta',{'aud':'otra-app'})]:
    status, _, _ = call('GET','/api/registros',signed(changes))
    expect(label,status,401)
(OUT/'jwt-api.txt').write_text('\n'.join(checks)+'\n')
print(f'API JWT: {len(checks)} verificaciones correctas.',flush=True)

# Keep the export public; credentials exist only in this ignored local environment.
private = Path('postman/Local.private.postman_environment.json')
private.write_text(json.dumps({'name':'Local privado','values':[
    {'key':'baseUrl','value':'http://localhost:5000','enabled':True},
    {'key':'username','value':credentials['username'],'enabled':True},
    {'key':'password','value':credentials['password'],'enabled':True,'type':'secret'}],
    '_postman_variable_scope':'environment'},indent=2))
private.chmod(0o600)
print('Ejecutando colección Postman con JWT (salida sin secretos)...',flush=True)
with (OUT/'postman-api-jwt.txt').open('w') as output:
    subprocess.run(['postman','collection','run','postman/Bitacora.postman_collection.json','-e',str(private),
        '--no-report-events'],stdout=output,stderr=subprocess.STDOUT,check=True)

# Verify the issued Android session reaches each actual HTTP client.
print('Verificando login y tres clientes en Pixel...',flush=True)
adb('shell','am','force-stop','com.melvinpena.comparativa')
adb('shell','am','start','-W','-n','com.melvinpena.comparativa/.MainActivity')
android_login()
tap('Comenzar comparación   →')

def scroll_top():
    for _ in range(3): adb('shell','input','swipe','700','900','700','2600','200')

def node_visible(n):
    import re
    b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
    return len(b)==4 and b[3]>b[1] and b[1]>=100 and b[3]<=2940

def press_client(client):
    for _ in range(5):
        ns=nodes()
        labels=[n for n in ns if n.get('text')==client and node_visible(n)]
        buttons=[n for n in ns if n.get('text','').casefold()=='probar' and node_visible(n)]
        if labels and buttons:
            import re
            center=lambda n: sum(list(map(int,re.findall(r'\d+',n.get('bounds'))))[1::2])/2
            button=min(buttons,key=lambda n: abs(center(n)-center(labels[0])))
            if abs(center(button)-center(labels[0]))<200:
                tap_node(button);return
        adb('shell','input','swipe','700','2600','700','1000','250')
    raise AssertionError('No se localiza botón de '+client)

def client_result(client):
    for _ in range(6):
        for n in nodes():
            text=n.get('text','')
            if text.startswith(client+':'): return text
        adb('shell','input','swipe','700','2600','700','1000','250')
    raise AssertionError('No aparece resultado de '+client)

android=[]
for client in ['Volley','OkHttp','Retrofit']:
    press_client(client)
    text=client_result(client)
    assert 'registros recibidos' in text, text
    android.append('PASS: JWT de sesión Android en '+text)
scroll_top()
fill(1,'')
for client in ['Volley','OkHttp','Retrofit']:
    press_client(client)
    text=client_result(client)
    assert '(401)' in text,text
    android.append('PASS: encabezado ausente en '+text)
adb('shell','input','keyevent','KEYCODE_BACK')
(OUT/'jwt-android.txt').write_text('\n'.join(android)+'\n')
with urllib.request.urlopen('http://localhost:5000/openapi/v1.json',timeout=10) as response:
    Path('backend/openapi.json').write_bytes(response.read())
print('\n'.join(checks+android),flush=True)
print('FINAL: backend y Pixel listos; secretos solo en archivos locales ignorados.',flush=True)

"""Smoke test against a running development API and the Android emulator.
Creates and deletes only its own uniquely titled test record. No credentials.
"""
import json
import shutil
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb')
EVIDENCE = Path('docs/evidencias')
EVIDENCE.mkdir(parents=True, exist_ok=True)

def adb(*args):
    return subprocess.check_output([ADB, *args])

def nodes():
    adb('shell', 'uiautomator', 'dump', '/sdcard/bitacora-smoke.xml')
    return list(ET.fromstring(adb('shell', 'cat', '/sdcard/bitacora-smoke.xml')).iter('node'))

def find(text, exact=True):
    for _ in range(5):
        for node in nodes():
            value = node.get('text', '')
            if (value.casefold() == text.casefold() if exact else text.casefold() in value.casefold()):
                return node
    raise AssertionError('No aparece: ' + text)

def tap_node(node):
    left, top, right, bottom = map(int, re.findall(r'\d+', node.attrib['bounds']))
    adb('shell', 'input', 'tap', str((left+right)//2), str((top+bottom)//2))

def tap(text, exact=True):
    tap_node(find(text, exact))

def fill(index, value):
    fields = [n for n in nodes() if n.get('class') == 'android.widget.EditText']
    tap_node(fields[index])
    adb('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END')
    count = len(fields[index].get('text', ''))
    if count:
        adb('shell', 'input', 'keyevent', *(['KEYCODE_DEL'] * count))
    if value:
        adb('shell', 'input', 'text', value.replace(' ', '%s'))
    adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')

def snap(name):
    (EVIDENCE / (name + '.png')).write_bytes(adb('exec-out', 'screencap', '-p'))

def lab_credentials():
    return json.loads(Path('backend/Bitacora.Api/.dev-login.json').read_text())

_API_TOKEN = None

def api_token():
    global _API_TOKEN
    if _API_TOKEN is None:
        request = urllib.request.Request('http://localhost:5000/api/auth/login',
            data=json.dumps(lab_credentials()).encode(), headers={'Content-Type': 'application/json'})
        with urllib.request.urlopen(request, timeout=10) as response:
            _API_TOKEN = json.load(response)['accessToken']
    return _API_TOKEN

def android_login():
    tap('Iniciar sesión')
    credentials = lab_credentials()
    fill(0, credentials['username'])
    fill(1, credentials['password'])
    tap('Entrar')
    find('Cerrar sesión', exact=False)

def records():
    request = urllib.request.Request('http://localhost:5000/api/registros',
        headers={'Authorization': 'Bearer ' + api_token()})
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.load(response)

def main():
    title = 'PruebaAndroid' + str(int(time.time()))
    created_id = None
    results = []
    try:
        adb('shell', 'am', 'force-stop', 'com.melvinpena.comparativa')
        adb('shell', 'am', 'start', '-W', '-n', 'com.melvinpena.comparativa/.MainActivity')
        android_login()
        tap('Abrir bitácora de campo')
        tap('Nuevo registro')
        tap('Guardar en el servidor')
        find('El título debe tener de 3 a 100 caracteres.')
        results.append('PASS: validación local de título vacío')
        fill(0, title)
        fill(1, 'DescripcionAndroid')
        tap('Guardar en el servidor')
        find('Detalle del registro')
        find(title)
        matches = [r for r in records() if r['titulo'] == title]
        assert len(matches) == 1
        created_id = matches[0]['id']
        snap('android-crud-creado')
        results.append('PASS: POST desde Android, detalle y persistencia en la API')
        tap('Editar')
        fill(1, 'DescripcionEditada')
        tap('Guardar en el servidor')
        find('DescripcionEditada')
        matches = [r for r in records() if r['id'] == created_id]
        assert matches[0]['descripcion'] == 'DescripcionEditada'
        results.append('PASS: PUT desde Android y contenido actualizado en API')
        tap('Volver al listado')
        tap(title, exact=False)
        find('Detalle del registro')
        find('DescripcionEditada')
        results.append('PASS: GET listado, selección y GET detalle')
        tap('Eliminar')
        find('¿Eliminar este registro del servidor?')
        tap('Cancelar')
        assert any(r['id'] == created_id for r in records())
        results.append('PASS: cancelar eliminación conserva registro')
        tap('Eliminar')
        # Dialog positive button; there is also a background Eliminar button.
        positive = next(n for n in nodes() if n.get('resource-id') == 'android:id/button1')
        tap_node(positive)
        find('Registros del servidor')
        assert not any(r['id'] == created_id for r in records())
        snap('android-crud-eliminado')
        results.append('PASS: DELETE confirmado desde Android y eliminación en API')
    except Exception as error:
        results.append('FAIL: ' + str(error))
        snap('android-crud-fallo')
        raise
    finally:
        report = '\n'.join(results) + '\n'
        (EVIDENCE / 'android-crud-smoke.txt').write_text(report)
        print(report, flush=True)
        # Do not remove records silently after a failed test: leave ID for inspection.
        print('Registro de prueba:', created_id, title, flush=True)


if __name__ == "__main__":
    main()

"""Graba uso real de Android para el video guía; requiere API, APK y emulador.
No graba credenciales: el login ocurre antes de iniciar screenrecord.
"""
import json
import os
import subprocess
import time
from pathlib import Path
from smoke_android_crud import ADB,adb,android_login,fill,nodes,tap,tap_node
from verify_lab_failures import scroll_top,select_scenario,visible

ROOT=Path(__file__).resolve().parents[1]
OUT=Path('/tmp/tarea25-video');OUT.mkdir(exist_ok=True)
PACKAGE='com.melvinpena.comparativa'

def press(client):
    for _ in range(8):
        matches=[n for n in nodes() if n.get('text','').casefold() == ('Probar '+client).casefold() and visible(n)]
        if matches:
            tap_node(matches[0]);return
        adb('shell','input','swipe','700','2500','700','1200','200')
    raise AssertionError('Botón de '+client+' no visible')

def wait_result(client,expected):
    for _ in range(100):
        logs=adb('logcat','-d','-s','ComparativaHTTP:I','*:S').decode()
        result=next((line for line in logs.splitlines() if ' | '+client+':' in line),None)
        if result:
            assert expected in result,(client,result)
            break
        time.sleep(.2)
    else:raise AssertionError('Sin resultado '+client)
    for _ in range(5):
        if any(n.get('text','').startswith(client+':') for n in nodes()):return result
        adb('shell','input','swipe','700','2500','700','1100','200')
    raise AssertionError('No se ve resultado '+client)

def record(name,action):
    existing=subprocess.run([ADB,'shell','pidof','screenrecord'],capture_output=True).stdout.strip()
    assert not existing,'Hay otra grabación activa'
    remote='/sdcard/tarea25-'+name+'.mp4'
    proc=subprocess.Popen([ADB,'shell','screenrecord','--size','720x1560','--bit-rate','3000000','--time-limit','150',remote],stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
    time.sleep(1)
    pid=adb('shell','pidof','screenrecord').decode().strip()
    assert pid.isdigit()
    try:action()
    finally:
        adb('shell','kill','-2',pid)
        proc.wait(timeout=15)
    adb('pull',remote,str(OUT/(name+'.mp4')))
    adb('shell','rm',remote)
    print('Grabación real guardada:',name,flush=True)

if __name__=='__main__':
    os.chdir(ROOT)
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','am','start','-W','-n',PACKAGE+'/.MainActivity')
    android_login();tap('Laboratorio de Pruebas HTTP')
    scroll_top();fill(2,'2000')
    results=[]
    def normal():
        select_scenario(0)
        for c in ['Volley','OkHttp','Retrofit']:
            adb('logcat','-c');press(c);results.append(wait_result(c,'registros recibidos'));time.sleep(1)
    def timeout():
        select_scenario(1)
        for c in ['Volley','OkHttp']:
            adb('logcat','-c');press(c);results.append(wait_result(c,'Tiempo de espera agotado'));time.sleep(1)
    def error():
        select_scenario(2)
        for c in ['Volley','OkHttp']:
            adb('logcat','-c');press(c);results.append(wait_result(c,'(500)'));time.sleep(1)
        select_scenario(0)
        adb('logcat','-c');press('Retrofit');results.append(wait_result('Retrofit','registros recibidos'));time.sleep(1)
    record('android-listado',normal)
    record('android-timeout',timeout)
    record('android-error500',error)
    (OUT/'verificacion.txt').write_text('\n'.join(results)+'\n')

"""Video guía narrado con código y grabaciones reales de Android.
Requiere macOS say, Pillow, Pygments e imageio-ffmpeg; ejecutar antes record_android_demo.py.
"""
import json
import re
import os
import subprocess
import textwrap
import time
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
from pygments import lex
from pygments.lexers import KotlinLexer
from pygments.token import Token
import imageio_ffmpeg

ROOT=Path(__file__).resolve().parents[1]
WORK=Path('/tmp/tarea25-video');WORK.mkdir(exist_ok=True)
FF=imageio_ffmpeg.get_ffmpeg_exe()
OUT=ROOT/'Video_Guia_Tarea_2_5_Melvin_Pena.mp4'
ACT=ROOT/'app/src/main/java/com/melvinpena/comparativa/ComparativaActivity.kt'
FONT='/System/Library/Fonts/Supplemental/Arial.ttf'
BOLD='/System/Library/Fonts/Supplemental/Arial Bold.ttf'
MONO='/System/Library/Fonts/Menlo.ttc'
scenes=[
 dict(title='Una API. Tres clientes HTTP.',file='ComparativaActivity.kt',duration=18,kind='intro',
 narration='En este ejemplo explico cómo consumir la API de Bitácora de campo con Volley, OkHttp y Retrofit. Los tres consultan el mismo endpoint protegido con un token JWT. Primero revisaré el código y después mostraré el uso real de Android: listado, tiempo agotado y error quinientos.'),
 dict(title='Volley: solicitud y respuesta',file='ComparativaActivity.kt · requestWithVolley()',duration=32,kind='code',start='        val request = object : JsonArrayRequest(',end='        volleyQueue.add(request)',
 narration='Con Volley creo un JsonArrayRequest porque el servidor devuelve un arreglo JSON. La solicitud usa GET y la misma URL del laboratorio. Si responde correctamente, cuento los elementos con length y muestro el tiempo. Si falla, llamo al manejador de errores. Sobrescribo getHeaders para enviar el token. DefaultRetryPolicy establece el tiempo de espera y cero reintentos. Desactivo la caché y agrego la solicitud a la cola. Estos callbacks llegan al hilo principal.'),
 dict(title='JWT y tiempo de respuesta',file='ComparativaActivity.kt · authorizationHeaders()',duration=16,kind='code',start='    private fun authorizationHeaders(',end='    private fun showSuccess(',
 narration='El token lo genera la API después del inicio de sesión. Este auxiliar lo envía en Authorization con el prefijo Bearer. Si el campo está vacío, no agrega ese encabezado. La duración se calcula con un reloj monotónico, desde el inicio hasta publicar el resultado.'),
 dict(title='OkHttp: control manual y asincronía',file='ComparativaActivity.kt · requestWithOkHttp()',duration=43,kind='okhttp',
 narration='En OkHttp construyo manualmente el Request con la URL, los encabezados y GET. NewCall prepara la llamada y enqueue la ejecuta de forma asíncrona. OnFailure recibe fallos de transporte, como conexión o timeout. Un estado HTTP quinientos llega a onResponse porque el servidor sí respondió, por eso reviso isSuccessful. Response use cierra la respuesta. Gson y TypeToken convierten el cuerpo a una lista de objetos JSON. Estos callbacks trabajan fuera del hilo principal. Por eso uso runOnUiThread antes de actualizar la pantalla.'),
 dict(title='Retrofit reutiliza el repositorio',file='ComparativaActivity.kt · requestWithRetrofit()',duration=14,kind='code',start='        val repository = RegistroRepository(',end='    private fun escenarioSeleccionado(',
 narration='Retrofit reutiliza RegistroRepository del CRUD. El repositorio consulta el escenario seleccionado y entrega ApiResult de éxito o de error. El conversor transforma el JSON a objetos tipados y la pantalla presenta el resultado.'),
 dict(title='Android: consulta exitosa',file='Grabación real del emulador · listado normal',duration=27,kind='app',clip='android-listado',
 narration='Ahora uso la aplicación real. Ya inicié sesión sin mostrar las credenciales. Configuro dos mil milisegundos y selecciono Listado normal. Pruebo Volley, OkHttp y Retrofit. Cada cliente consulta la API y muestra la cantidad recibida y el tiempo. Para esta demostración hay registros guardados. El laboratorio muestra la cantidad; los títulos y detalles se consultan en la bitácora de campo.'),
 dict(title='Android: tiempo de espera agotado',file='Grabación real · demora de 15 s / timeout de 2000 ms',duration=28,kind='app',clip='android-timeout',
 narration='Selecciono el escenario de demora. El backend espera quince segundos mediante Task Delay, pero el cliente tiene un timeout de dos segundos. Ejecuto Volley y después OkHttp. Ambos muestran tiempo de espera agotado. Ese error es el resultado esperado de la prueba. La pantalla continúa abierta y me permite ejecutar otra consulta. Para este escenario mantengo el JWT válido.'),
 dict(title='Android: error 500 y recuperación',file='Grabación real · fallo controlado en Development',duration=32,kind='app',clip='android-error500',
 narration='Ahora selecciono Error interno y pruebo los dos clientes. La API provoca una excepción controlada y devuelve HTTP quinientos. La aplicación identifica el estado y muestra un mensaje, sin cerrarse. No es un error de conexión: el servidor respondió con un estado de error. Al final vuelvo a Listado normal y ejecuto Retrofit. La consulta exitosa demuestra que la aplicación sigue funcionando después de los fallos.'),
 dict(title='Resultados y recomendación',file='Informe individual · mediciones originales',duration=23,kind='results',
 narration='Realicé cinco consultas exitosas por cliente y cinco escenarios de fallo con cada uno. En la serie original, con un listado vacío, Volley tuvo el menor promedio. Los registros actuales se agregaron después y no cambian esas mediciones. Recomiendo Retrofit para el proyecto final por sus objetos tipados, el repositorio compartido y el manejo centralizado de errores. Esta es una guía: replica el recorrido con tus propias palabras y la cámara encendida.'),
]

def run(args):return subprocess.run(args,check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
def duration(path):
 p=subprocess.run([FF,'-i',str(path)],stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
 import re
 m=re.search(r'Duration: (\d+):(\d+):(\d+\.\d+)',p.stderr)
 assert m,path
 return int(m[1])*3600+int(m[2])*60+float(m[3])
def font(size,bold=False,mono=False):return ImageFont.truetype(MONO if mono else BOLD if bold else FONT,size)
def lines(draw,text,xy,size=30,width=65,color='#dae7ee',bold=False):
 x,y=xy
 for line in textwrap.wrap(text,width=width,break_long_words=False):
  draw.text((x,y),line,font=font(size,bold),fill=color);y+=size+10
 return y
def draw_kotlin(draw,text,xy,size):
 x,y=xy
 codefont=font(size,mono=True)
 offset=0
 for kind,value in lex(text,KotlinLexer(stripnl=False,ensurenl=False)):
  color='#e4edf5'
  if kind in Token.Comment:color='#88a799'
  elif kind in Token.Keyword:color='#c7a2ff'
  elif kind in Token.Literal.String:color='#a7db9b'
  elif kind in Token.Literal.Number:color='#ffbf80'
  elif kind in Token.Operator:color='#7bd9ed'
  elif kind in Token.Name:
   if value[:1].isupper():color='#69d2cb'
   elif re.match(r'\s*\(',text[offset+len(value):]):color='#ffd27e'
   elif value in ('override','private','fun','val','var','return','if','else','try','catch','object','null','false','true'):color='#c7a2ff'
  draw.text((x,y),value,font=codefont,fill=color)
  x+=draw.textlength(value,font=codefont);offset+=len(value)

def snippet(scene):
 text=ACT.read_text();start=text.index(scene['start']);end=text.index(scene['end'],start)
 if scene['end']=='        volleyQueue.add(request)':end+=len(scene['end'])
 raw=text[start:end].rstrip().splitlines()
 numbered=[]
 n=text[:start].count('\n')+1
 for index,line in enumerate(raw):numbered.append((n+index,line))
 return numbered

def background(scene,index):
 im=Image.new('RGB',(1920,1080),'#0d1d2d');d=ImageDraw.Draw(im)
 d.rectangle((0,0,1920,12),fill='#18b0a5')
 d.text((65,40),f'TAREA 2.5  /  PASO {index+1:02d} DE {len(scenes)}',font=font(22,True),fill='#59d5ca')
 d.text((65,85),scene['title'],font=font(45,True),fill='white')
 d.text((65,146),scene['file'],font=font(24),fill='#b9cdd9')
 if scene['kind'] in ['code','okhttp']:
  d.rounded_rectangle((60,205,1860,850),radius=20,fill='#152c40')
  if scene['kind']=='okhttp':
   text=ACT.read_text();start=text.index('        val client = httpClient(timeout)',text.index('    private fun requestWithOkHttp'))
   raw=text[start:text.index('    private fun requestWithRetrofit()',start)].rstrip().splitlines()
   # All operative parts remain visible; braces/blank lines are abbreviated for this guide.
   wanted=['val client =','val request =','.url(url)','.apply {','.get()','activeOkHttpCall =','!!.enqueue','override fun onFailure','runOnUiThread { showFailure','override fun onResponse','response.use','if (!it.isSuccessful)','showHttpFailure','val body =','val registros:','body,','TypeToken','showSuccess']
   content=[(text[:start].count('\n')+1+i,line) for i,line in enumerate(raw) if any(w in line for w in wanted)]
   d.text((100,813),'Selección de líneas reales; consulta el método completo en el IDE.',font=font(21),fill='#9bb6c8')
  else:content=snippet(scene)
  size=25 if len(content)<=22 else 22
  y=230
  for n,line in content:
   if not line.strip():y+=10;continue
   line=line[8:] if line.startswith('        ') else line.lstrip()
   d.text((90,y),str(n),font=font(size,mono=True),fill='#7596ad')
   # Wrap long real lines without changing source.
   for part in textwrap.wrap(line,width=105 if size==25 else 120,replace_whitespace=False,drop_whitespace=False) or ['']:
    draw_kotlin(d,part,(175,y),size);y+=size+6
  legend=[('Palabras clave','#c7a2ff'),('Tipos','#69d2cb'),('Funciones','#ffd27e'),('Textos','#a7db9b'),('Números','#ffbf80')]
  lx=80
  for label,color in legend:
   d.rectangle((lx,875,lx+13,888),fill=color);d.text((lx+23,868),label,font=font(20),fill=color);lx+=270
 elif scene['kind']=='app':
  steps={
   'android-listado':['1. Sesión JWT activa','2. Timeout: 2000 ms','3. Escenario: Listado normal','4. Probar Volley, OkHttp y Retrofit','5. Observar cantidad y duración'],
   'android-timeout':['1. Seleccionar Tiempo de espera','2. Backend: demora de 15 segundos','3. Cliente: espera de 2 segundos','4. Probar Volley y OkHttp','5. Ver timeout y continuar usando la app'],
   'android-error500':['1. Seleccionar Error interno (500)','2. Conservar un JWT válido','3. Probar Volley y OkHttp','4. Observar el error sin cierre','5. Volver al listado y probar Retrofit']}
  y=245
  for step in steps[scene['clip']]:y=lines(d,step,(80,y),size=33,width=49)+33
  lines(d,'Grabación auténtica del emulador. Se ajusta la velocidad para que el recorrido quepa en la guía.',(80,735),size=24,width=65,color='#9bb6c8')
  d.rounded_rectangle((1390,190,1780,895),radius=25,fill='#07111b')
 elif scene['kind']=='results':
  y=240
  for label,val in [('Volley','209,40 ms'),('OkHttp','774,20 ms'),('Retrofit','869,40 ms')]:
   d.text((100,y),label,font=font(45,True),fill='#eaf4fa');d.text((650,y),val,font=font(45,True),fill='#59d5ca');y+=95
  lines(d,'Serie original: 5 consultas por cliente, 0 registros. Los datos de demostración se agregaron después.',(100,560),size=31,width=85)
  lines(d,'Elección para el proyecto: Retrofit por DTO tipados, repositorio compartido y errores centralizados.',(100,690),size=34,width=82,bold=True)
 else:
  lines(d,'GET /api/registros · JWT válido',(90,260),size=45,width=65,bold=True)
  lines(d,'Código + uso real de Android',(90,365),size=41,width=65)
  lines(d,'Consulta exitosa → Timeout → HTTP 500 → Recuperación',(90,480),size=35,width=70)
  lines(d,'Video guía con voz sintética. Para tu entrega, explica con tus palabras y mantén la cámara encendida.',(90,675),size=30,width=85,color='#9bb6c8')
 d.rectangle((0,920,1920,1080),fill='#07131f')
 narration=scene['narration']
 # On-screen caption is a concise summary; full narration is in the transcript.
 summaries={
 'intro':'Recorrido: código de los tres clientes y uso real de Android. Video de ejemplo para replicar con tu cámara.',
 'okhttp':'enqueue es asíncrono. onFailure: transporte. onResponse: revisar HTTP, convertir JSON y actualizar UI con runOnUiThread.',
 'results':'Los promedios pertenecen al listado vacío original. La recomendación de Retrofit se basa en estructura y mantenimiento.'}
 caption=summaries.get(scene['kind'],narration.split('. ')[0]+'.')
 lines(d,caption,(65,944),size=26,width=124)
 d.text((65,1039),'GUÍA DEMOSTRATIVA · VOZ SINTÉTICA · TU ENTREGA REQUIERE CÁMARA Y EXPLICACIÓN PROPIA',font=font(17,True),fill='#73a1ba')
 path=WORK/f'scene-{index:02d}.png';im.save(path);return path

def main():
 total=0;transcript=['# Video guía para replicar — Tarea 2.5','',
 'Ejemplo con narración sintética y grabación real del emulador. No sustituye el video individual con cámara.',
 'Login previo a la grabación; no se muestran contraseñas ni JWT completos.','']
 for i,scene in enumerate(scenes):
  print('Generando escena',i+1,scene['title'],flush=True)
  textfile=WORK/f'voice-{i:02d}.txt';textfile.write_text(scene['narration'])
  audio=WORK/f'voice-{i:02d}.aiff'
  run(['say','-v','Mónica','-r','170','-f',str(textfile),'-o',str(audio)])
  seconds=max(scene['duration'],duration(audio)+.7)
  bg=background(scene,i)
  args=[FF,'-hide_banner','-loglevel','error','-y','-loop','1','-framerate','24','-i',str(bg)]
  if scene.get('clip'):
   clip=WORK/(scene['clip']+'.mp4')
   deadline=time.monotonic()+300
   while not clip.exists() and time.monotonic()<deadline:time.sleep(.5)
   assert clip.exists(),clip
   clipduration=duration(clip);factor=min(1,seconds/clipduration)
   args+=['-i',str(clip),'-i',str(audio),'-filter_complex',f'[1:v]setpts={factor:.6f}*(PTS-STARTPTS),scale=-2:690,tpad=stop_mode=clone:stop_duration={seconds}[phone];[0:v][phone]overlay=1425:195[v];[2:a]apad[a]','-map','[v]','-map','[a]']
  else:args+=['-i',str(audio),'-vf','format=yuv420p','-af','apad','-map','0:v','-map','1:a']
  args+=['-t',str(round(seconds,3)),'-c:v','libx264','-preset','veryfast','-crf','22','-pix_fmt','yuv420p','-r','24','-threads','2','-c:a','aac','-b:a','128k','-ar','48000','-ac','2',str(WORK/f'part-{i:02d}.mp4')]
  run(args)
  fmt=lambda t:f'{int(t)//60:02d}:{int(t)%60:02d}'
  transcript += [f'## {fmt(total)}–{fmt(total+seconds)} · {scene["title"]}', '',f'Mostrar: {scene["file"]}.', '',scene['narration'],'']
  total+=seconds
 concat=WORK/'concat.txt';concat.write_text('\n'.join("file '"+str(WORK/f'part-{i:02d}.mp4')+"'" for i in range(len(scenes))))
 run([FF,'-hide_banner','-loglevel','error','-y','-f','concat','-safe','0','-i',str(concat),'-c','copy','-movflags','+faststart',str(OUT)])
 (ROOT/'docs/GUION_VIDEO_DEMOSTRATIVO.md').write_text('\n'.join(transcript)+f'\nDuración total aproximada: {fmt(total)}.\n')
 print(json.dumps({'archivo':str(OUT),'segundos':total,'tamano_mb':OUT.stat().st_size/1024**2}),flush=True)

if __name__=='__main__':main()

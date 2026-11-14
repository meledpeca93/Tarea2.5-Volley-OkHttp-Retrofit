"""Genera el PDF individual desde las evidencias reales. Requiere reportlab y Pillow.
Salida local en la raíz; no publica archivos ni modifica evidencias.
"""
import json
import re
from pathlib import Path
from statistics import mean
from xml.sax.saxutils import escape
from PIL import Image as PILImage
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak, Preformatted

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'Informe_Tarea_2_5_Melvin_Pena.pdf'
E=ROOT/'docs/evidencias'
FAILURES=json.loads((E/'laboratorio-fallos.json').read_text())
SAMPLES=json.loads((E/'laboratorio-mediciones.json').read_text())
COUNTS=json.loads((E/'laboratorio-lineas.json').read_text())
CLIENTS=['Volley','OkHttp','Retrofit']
REPO='https://github.com/meledpeca93/Tarea2.5-Volley-OkHttp-Retrofit'
PR=REPO+'/pull/4'
BLOB=REPO+'/blob/feature/comparativa-melvin-pena/'
NAVY=colors.HexColor('#16324F');TEAL=colors.HexColor('#087F83');LIGHT=colors.HexColor('#EFF5F8');INK=colors.HexColor('#213547');MUTED=colors.HexColor('#526779')
fontdir=Path('/System/Library/Fonts/Supplemental')
if (fontdir/'Arial.ttf').exists():
    for name,file in [('Body','Arial.ttf'),('BodyBold','Arial Bold.ttf'),('BodyItalic','Arial Italic.ttf')]:pdfmetrics.registerFont(TTFont(name,str(fontdir/file)))
else:
    # Portable fallback; Spanish accents are supported by the standard fonts.
    for name,base in [('Body','Helvetica'),('BodyBold','Helvetica-Bold'),('BodyItalic','Helvetica-Oblique')]:pdfmetrics.registerFont(pdfmetrics.Font(name,base,'WinAnsiEncoding'))
pdfmetrics.registerFontFamily('Body',normal='Body',bold='BodyBold',italic='BodyItalic',boldItalic='BodyBold')
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='Text',fontName='Body',fontSize=9.3,leading=13.1,textColor=INK,spaceAfter=7))
styles.add(ParagraphStyle(name='SmallText',parent=styles['Text'],fontSize=8,leading=10.5,spaceAfter=5))
styles.add(ParagraphStyle(name='CellText',parent=styles['Text'],fontSize=8,leading=10.4,spaceAfter=0))
styles.add(ParagraphStyle(name='HeadCell',parent=styles['CellText'],fontName='BodyBold',textColor=colors.white))
styles.add(ParagraphStyle(name='TitleMain',fontName='BodyBold',fontSize=29,leading=34,textColor=NAVY,spaceAfter=16))
styles.add(ParagraphStyle(name='SectionTitle',fontName='BodyBold',fontSize=20,leading=25,textColor=NAVY,spaceAfter=13))
styles.add(ParagraphStyle(name='SubTitleCustom',fontName='BodyBold',fontSize=11.5,leading=15,textColor=TEAL,spaceBefore=7,spaceAfter=7))
styles.add(ParagraphStyle(name='CaptionCustom',parent=styles['SmallText'],alignment=TA_CENTER,textColor=MUTED))
styles.add(ParagraphStyle(name='CodeCustom',fontName='Courier',fontSize=7,leading=9,textColor=INK,backColor=LIGHT,borderPadding=7,spaceAfter=10))
W=A4[0]-36*mm
story=[]
def p(s,style='Text'):return Paragraph(s,styles[style])
def add(s,style='Text'):story.append(p(s,style))
def title(number,s):
    if story:story.append(PageBreak())
    add(f'TAREA 2.5  /  {number}','SubTitleCustom');add(s,'SectionTitle')
def table(headers,rows,widths=None):
    data=[[p(escape(str(x)),'HeadCell') for x in headers]]+[[p(str(x),'CellText') for x in row] for row in rows]
    t=Table(data,colWidths=widths or [W/len(headers)]*len(headers),repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),NAVY),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,LIGHT]),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),7),('RIGHTPADDING',(0,0),(-1,-1),7),('TOPPADDING',(0,0),(-1,-1),7),('BOTTOMPADDING',(0,0),(-1,-1),7),('LINEBELOW',(0,0),(-1,0),.8,TEAL)]))
    story.append(t);story.append(Spacer(1,8))
def link(label,url):return f'<link href="{escape(url)}" color="#087F83">{escape(label)}</link>'
def code(s):story.append(Preformatted(s,styles['CodeCustom']))

# 1. Cover and objective.
logo=ROOT/'app/src/main/res/drawable-nodpi/uth_logo.png'
if logo.exists():
    lw,lh=PILImage.open(logo).size
    im=Image(str(logo),width=lw/lh*17*mm,height=17*mm);im.hAlign='LEFT';story.append(im)
story.append(Spacer(1,19*mm))
add('INFORME INDIVIDUAL · UNIDAD 9','SubTitleCustom')
add('Comparación práctica de<br/>Volley, OkHttp y Retrofit','TitleMain')
add('Consumo de un endpoint protegido con JWT y manejo de fallos','SubTitleCustom')
story.append(Spacer(1,8*mm))
table(['Estudiante','Entrega'],[['Melvin Peña','Tarea 2.5 · Individual · 5 %'],['Fecha del informe: 12 de noviembre de 2026','Fecha límite: 13 de noviembre de 2026, 23:59']],[W*.50,W*.50])
add('Objetivo','SubTitleCustom')
add('Implementar la misma consulta GET /api/registros con Volley, OkHttp directo y Retrofit; comparar su código, tiempos y comportamiento ante errores del servidor, falta de conexión, timeout y autenticación inválida o ausente.')
add('El laboratorio forma parte de Bitácora de campo. Retrofit reutiliza el repositorio del cliente MVVM; Volley y OkHttp consumen el mismo servicio y usan el mismo JWT.')
add('Contenido','SubTitleCustom')
add('Implementación y metodología; cuadro comparativo; cinco escenarios con quince resultados, capturas y extractos de Logcat; recomendación final y trazabilidad de Git.')
add(link('Repositorio del proyecto',REPO)+'<br/>'+link('Pull request individual #4 hacia main',PR))
add('Resultados medidos: 15 consultas exitosas y 15 pruebas de fallos. En todos los fallos se conservó el proceso Android. La revisión del PR por otro integrante y el video son entregables separados.','SmallText')

# 2. Implementation.
title('01','Implementación del laboratorio')
add('ComparativaActivity ofrece tres botones y un selector de escenario. Cada solicitud muestra cantidad y duración, o un mensaje de error. Se bloquea la configuración durante la petición y se cancelan solicitudes al destruir la pantalla.')
table(['Escenario','Ruta GET','Preparación'],[
['Listado normal','/api/registros','JWT válido; mismo endpoint y datos para los tres clientes.'],
['Tiempo de espera','/api/pruebas/demora','await Task.Delay de 15 s; timeout Android de 2000 ms.'],
['Error interno','/api/pruebas/error','Excepción controlada, HTTP 500; solo Development.']],[W*.22,W*.31,W*.47])
add('Volley','SubTitleCustom')
add('JsonArrayRequest recibe un JSONArray y cuenta sus elementos. getHeaders envía Authorization: Bearer y Cache-Control. DefaultRetryPolicy configura el timeout y cero reintentos. La respuesta llega al hilo principal; la solicitud tiene tag y caché desactivada.')
code('override fun getHeaders() = authorizationHeaders(token)\nrequest.retryPolicy = DefaultRetryPolicy(timeout, 0,\n    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)\nrequest.setShouldCache(false)')
add('OkHttp directo','SubTitleCustom')
add('Request.Builder construye GET y encabezados; enqueue ejecuta en segundo plano. response.use cierra el recurso. Gson y TypeToken convierten a List&lt;JsonObject&gt;. Los estados HTTP se revisan aparte de IOException; runOnUiThread publica éxito o error.')
code('activeOkHttpCall = client.newCall(request)\nactiveOkHttpCall!!.enqueue(object : OkHttpCallback {\n    // onFailure: error de transporte; onResponse: revisar HTTP y Gson.\n    // runOnUiThread { mostrar resultado }\n})')
add('Retrofit y JWT','SubTitleCustom')
add('RegistroRepository.consultarLaboratorio delega el listado normal en listar y usa el contrato con @Url para las rutas de prueba. GsonConverterFactory produce List&lt;RegistroResumen&gt;. El repositorio comparte autorización y ApiResult con el CRUD; los callbacks llegan al hilo principal con la configuración Android actual.')
add('El login local entrega un JWT de 15 minutos, conservado en memoria. Se omite Authorization al vaciar el campo para probar 401. Las rutas de fallo exigen JWT, están limitadas a Development y su selector a debug. No se incluyen tokens ni credenciales en este informe.','SmallText')

# 3. measurements.
title('02','Metodología y tiempos medidos')
add('Se ejecutaron cinco rondas en orden Volley → OkHttp → Retrofit, una solicitud a la vez. Todas usaron GET /api/registros, JWT válido, timeout de 2000 ms, Pixel 4 XL emulado, app debug y API ASP.NET Core local Development en http://10.0.2.2:5000/.')
add('<b>Datos: 0 registros.</b> Se comprobó que el contenido antes y después fuera idéntico mediante SHA-256 en memoria. No se modificaron datos. Wi-Fi activo, modo avión apagado; sin cambios de red durante la serie.')
add('No hubo calentamiento del listado en este proceso; el login ocurrió antes de medir y la API ya había atendido las pruebas previas. Se conservaron las cinco muestras de cada cliente, incluida la primera.')
rows=[]
for c in CLIENTS:
    vals=[s['duracion_ms'] for s in SAMPLES if s['cliente']==c]
    rows.append([c]+list(map(str,vals))+[f'{mean(vals):.2f}'.replace('.',',')])
table(['Cliente','t1','t2','t3','t4','t5','Promedio'],rows,[W*.19]+[W*.115]*5+[W*.235])
add('Todos los tiempos están en milisegundos. Promedio = (t1 + t2 + t3 + t4 + t5) / 5.','SmallText')
add('Qué mide el reloj','SubTitleCustom')
add('SystemClock.elapsedRealtime incluye preparación del cliente, red, conversión JSON y entrega a la interfaz. Las capturas se toman después de recibir el resultado; no se usan como cronómetro. El proceso conservó el mismo PID durante las quince consultas.')
add('Condiciones de transporte','SubTitleCustom')
add('Volley reutiliza RequestQueue y desactiva caché y reintentos. OkHttp y Retrofit construyen cliente por consulta, sin caché de disco y con retryOnConnectionFailure(false). Los tres envían Cache-Control: no-cache, no-store. OkHttp/Retrofit no siguen redirecciones; se usó la URL final.')
add('Límites de interpretación','SubTitleCustom')
add('Volley tuvo el menor promedio observado. La lista vacía no mide el costo de convertir registros reales. Volley cuenta un JSONArray, OkHttp convierte a JsonObject y Retrofit a DTO tipado; Retrofit además usa logging BASIC en debug. La construcción de clientes, los usos previos del servidor y el emulador impiden concluir una ventaja general de rendimiento.')
add('El timeout de Volley se aplica al socket; en OkHttp/Retrofit se aplica a conexión, lectura y escritura. No equivale a un límite total de extremo a extremo. No se midió concurrencia ni carga.')
add(link('Las 15 muestras, capturas y Logcat',BLOB+'docs/evidencias/LABORATORIO_MEDICIONES.md'))

# 4. Technical comparison.
title('03','Cuadro comparativo · funcionamiento')
table(['Criterio','Volley','OkHttp directo','Retrofit'],[
['Líneas en Activity','26','42','18; delega en repositorio. No es el total.'],
['Encabezados','getHeaders; Authorization y Cache-Control.','Request.Builder.addHeader.','@Header para JWT y @Headers para caché; sesión desde repositorio.'],
['Conversión JSON','JsonArrayRequest entrega JSONArray; length, sin DTO.','Gson manual y TypeToken a List&lt;JsonObject&gt;.','GsonConverterFactory a List&lt;RegistroResumen&gt;.'],
['Hilo del callback','Principal en esta cola Android.','Trabajo; requiere runOnUiThread.','Principal con configuración Android actual.'],
['Estados HTTP','VolleyError.networkResponse.statusCode.','Response.code; no equivalen a onFailure.','Response.code y ApiResult.Failure.httpCode.'],
['Transporte y timeout','NoConnectionError / ConnectException; TimeoutError.','ConnectException; SocketTimeoutException.','onFailure; repositorio traduce ConnectException / SocketTimeoutException.'],
['Reintentos','DefaultRetryPolicy: 0.','retryOnConnectionFailure(false).','Igual ajuste en ApiClient (OkHttp).'],
['Tiempo de espera','Socket por DefaultRetryPolicy.','Conexión, lectura y escritura.','Los mismos tres ajustes de OkHttp.'],
['Caché','setShouldCache(false).','Sin caché de disco.','Sin caché de disco.'],
['Cancelación','Tag y cancelAll al destruir.','Call.cancel al destruir.','RequestHandle.cancel al destruir.'],
['Promedio de 5','209,40 ms','774,20 ms','869,40 ms'],
['Fallos sin cierre','5 de 5','5 de 5','5 de 5']],[W*.21,W*.255,W*.255,W*.28])
add('Comparación basada en el código de esta implementación. Los tres envían Cache-Control: no-cache, no-store. Las clases de error de las pruebas se detallan en las páginas de cada escenario.','SmallText')

# 5. code count & maintenance.
title('04','Conteo, pruebas y mantenimiento')
add('Criterio del conteo','SubTitleCustom')
add('Líneas físicas no vacías, incluidas firmas y llaves. Se excluyen comentarios completos, package, imports y decoración visual. Una línea cuenta una vez aunque contenga varias instrucciones. La fila de Activity cuenta únicamente requestWithVolley, requestWithOkHttp y requestWithRetrofit, hasta antes del siguiente método.')
table(['Bloque de código','Líneas','Alcance'],[
['requestWithVolley','26','Adaptador en Activity.'],['requestWithOkHttp','42','Adaptador en Activity.'],['requestWithRetrofit','18','Adaptador; delega en el repositorio.'],
['httpClient (Activity)','8','Auxiliar de OkHttp directo.'],['RegistroApi.kt completo','15','Interfaz, anotaciones y DTO; incluye CRUD y laboratorio.'],['ApiClient.kt completo','31','Fábrica, conversor, timeouts y logging debug.'],['RegistroRepository.kt completo','89','CRUD, autorización, ApiResult, RequestHandle y errores compartidos.']],[W*.40,W*.12,W*.48])
add('La cola Volley se crea en onCreate. El ciclo de vida y los auxiliares compartidos de UI, validación, tiempo y resultados quedan fuera del conteo principal. Los archivos Retrofit incluyen operaciones ajenas a este GET: no se suman como un costo mínimo equivalente del listado. Tener menos líneas en Activity refleja delegación, no menor código total.','SmallText')
add('Facilidad de prueba y mantenimiento','SubTitleCustom')
table(['Cliente','Valoración sustentada en el código'],[
['Volley','Conciso para arreglos. Construcción y callbacks residen en Activity; si cambian los campos se necesita agregar mapeo/validación a objetos.'],
['OkHttp','Request explícito y control del transporte. Requiere mantener cierre de Response, Gson, HTTP y cambios de hilo en la pantalla.'],
['Retrofit','DTO y contrato compartidos con CRUD; errores y autorización centralizados. Para pruebas unitarias aisladas habría que inyectar el servicio: el constructor actual lo crea internamente.']],[W*.18,W*.82])
add('Los tres se verificaron mediante UI real y API local. No se ejecutaron pruebas unitarias con servicios inyectados; la facilidad de prueba descrita es una valoración del diseño.','SmallText')
add('Reproducción: python3 scripts/count_lab_lines.py. '+link('Conteo estructurado',BLOB+'docs/evidencias/laboratorio-lineas.json'),'SmallText')

# 6-10. One page per failure, all clients and full screenshots.
scenarios=[
('api-detenida','API detenida','Se detuvo únicamente la API iniciada por el script, conservando la red activa. Se consultó /api/registros con JWT válido. Después se reinició la API.'),
('modo-avion','Modo avión sin conexión','Se activó modo avión y se apagaron Wi-Fi y datos. Antes de las pruebas finales se esperó hasta que Android no tuviera red predeterminada activa. API encendida; GET /api/registros con JWT válido.'),
('timeout','Tiempo de espera agotado','API en Development: GET /api/pruebas/demora con JWT válido. await Task.Delay espera 15 segundos y devuelve un arreglo; timeout configurado en los tres clientes: 2000 ms.'),
('sin-token-401','Token ausente · HTTP 401','Red restaurada y API activa. Se vació el campo JWT para omitir Authorization en GET /api/registros. El caso Android es token ausente; no se atribuye a token vencido.'),
('error-500','Error interno · HTTP 500','GET /api/pruebas/error con JWT válido, solo en Development. Una excepción controlada provoca HTTP 500; el manejador entrega una respuesta genérica sin detalles internos.')]
for idx,(key,label,setup) in enumerate(scenarios,5):
    title(f'{idx:02d}',label)
    add(setup,'SmallText')
    cases=[next(r for r in FAILURES if r['escenario']==key and r['cliente']==c) for c in CLIENTS]
    data=[]
    for r in cases:
        diagnostic=r['logcat'].split(' | ')[1]
        ms=re.search(r'\((\d+) ms\)',r['mensaje']).group(1)
        visible=r['mensaje'].split(': ',1)[1]
        data.append([r['cliente'],escape(diagnostic),escape(visible),'Sí'])
    table(['Cliente','Error técnico recibido','Mensaje visible y tiempo','Sin cierre'],data,[W*.13,W*.30,W*.45,W*.12])
    add('Capturas reales del laboratorio','SubTitleCustom')
    imgs=[]
    for r in cases:
        iw,ih=PILImage.open(E/r['captura']).size
        im=Image(str(E/r['captura']),width=iw/ih*105*mm,height=105*mm);imgs.append(im)
    t=Table([imgs,[p(c,'CaptionCustom') for c in CLIENTS]],colWidths=[W/3]*3)
    t.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('ALIGN',(0,0),(-1,-1),'CENTER'),('LEFTPADDING',(0,0),(-1,-1),0),('RIGHTPADDING',(0,0),(-1,-1),0)]));story.append(t)
    add('Extractos de Logcat · ComparativaHTTP','SubTitleCustom')
    for r in cases:
        # Omit timestamp/PID here; preserve result and technical class from actual log.
        diagnostic=r['logcat'].split('ComparativaHTTP: ',1)[1]
        add('<b>'+r['cliente']+'</b>: '+escape(diagnostic),'SmallText')
    add('Se verificaron resultado visible, actividad de laboratorio y mismo PID del proceso. Capturas y registros completos: '+link('evidencias de los cinco fallos',BLOB+'docs/evidencias/LABORATORIO_FALLOS.md')+'.','SmallText')
    if key=='modo-avion':add('La captura inicial de red se tomó durante la transición; se repitieron los tres casos tras confirmar desconexión. Las evidencias incluidas son las finales. Al terminar se restauró la red (avión 0, Wi-Fi 1).','SmallText')

# 11. final recommendation and traceability.
title('10','Recomendación final y trazabilidad')
add('Elección para Bitácora de campo: Retrofit','SubTitleCustom')
add('Recomiendo Retrofit para el proyecto final. RegistroRepository ya se comparte entre el CRUD MVVM y el laboratorio; los DTO tipados y RegistroApi concentran el contrato. Autorización, conversión y errores están centralizados, de modo que un cambio del backend requiere menos adaptación repetida en las pantallas.')
add('Los tres clientes manejaron los cinco fallos sin cerrarse. Retrofit no fue el más rápido: promedió 869,40 ms, frente a 774,20 ms de OkHttp y 209,40 ms de Volley. La elección se fundamenta en mantenimiento y reutilización, considerando que los tiempos provienen de un listado vacío y de clientes construidos de forma diferente.')
add('Volley resulta práctico para consultas sencillas de arreglos. OkHttp directo aporta control del transporte a cambio de más manejo manual. En este proyecto, Retrofit reduce la lógica HTTP repetida en UI y permite mantener un contrato consistente con el CRUD.')
add('Como mejora futura convendría inyectar y reutilizar el cliente HTTP y medir con registros reales. Esas mejoras no se implementaron ni forman parte de las mediciones de este informe.','SmallText')
add('Rama, commits y solicitud de integración','SubTitleCustom')
add('Rama individual publicada: <b>feature/comparativa-melvin-pena</b>. Se conservó el historial y se incorporó main mediante fast-forward, sin force push. El PR #3 anterior ya estaba integrado; esta entrega continúa en el PR #4.')
table(['Commit','Cambio'],[
['c0ff331','Selector de escenarios compartidos en el laboratorio.'],['dcee07d','Quince fallos y quince mediciones con scripts y evidencias.'],['8654ba5','Cuadro comparativo y recomendación con resultados medidos.'],['2d2e3d3','Enlace de entrega y estado de publicación Git.']],[W*.20,W*.80])
add(link('PR #4 hacia main',PR)+' · '+link('Repositorio',REPO))
add('En la última verificación de publicación, el PR #4 estaba abierto, sin conflictos y pendiente de revisión de otro integrante. No se atribuye una revisión no realizada. El video individual de 3–4 minutos con cámara es un entregable separado. Este PDF se genera localmente para revisión del estudiante.','SmallText')
add('Fuentes de evidencia y reproducción','SubTitleCustom')
add(link('Quince fallos con capturas y Logcat',BLOB+'docs/evidencias/LABORATORIO_FALLOS.md')+'<br/>'+link('Quince mediciones exitosas',BLOB+'docs/evidencias/LABORATORIO_MEDICIONES.md')+'<br/>'+link('Informe y cuadro comparativo en Markdown',BLOB+'docs/BASE_INFORME.md'),'SmallText')
add('Scripts: verify_lab_failures.py, measure_lab_clients.py y count_lab_lines.py, en scripts/. Compilación e instalación debug correctas; backend compilado sin errores ni advertencias. No se modificaron registros de usuario durante estas pruebas.','SmallText')

def decorate(canvas,doc):
    canvas.saveState();page=doc.page
    canvas.setStrokeColor(TEAL);canvas.setLineWidth(1);canvas.line(18*mm,A4[1]-13*mm,A4[0]-18*mm,A4[1]-13*mm)
    canvas.setFont('Body',7.5);canvas.setFillColor(MUTED)
    canvas.drawString(18*mm,11*mm,'Melvin Peña  ·  Tarea 2.5  ·  Comparativa HTTP')
    canvas.drawRightString(A4[0]-18*mm,11*mm,str(page));canvas.restoreState()

def main():
    assert len(FAILURES)==15 and len(SAMPLES)==15
    assert COUNTS['adaptadores_activity']=={'Volley':26,'OkHttp':42,'Retrofit':18}
    doc=SimpleDocTemplate(str(OUT),pagesize=A4,rightMargin=18*mm,leftMargin=18*mm,topMargin=19*mm,bottomMargin=19*mm,title='Tarea 2.5 — Comparación práctica de Volley, OkHttp y Retrofit',author='Melvin Peña',subject='Informe individual con mediciones, escenarios de fallo y recomendación',pageCompression=1)
    doc.build(story,onFirstPage=decorate,onLaterPages=decorate)
    print(OUT)

if __name__=='__main__':main()

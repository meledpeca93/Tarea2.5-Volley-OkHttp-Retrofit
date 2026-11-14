# Base del informe individual — Tarea 2.5

## Datos de la entrega

- Tema: comparación práctica de Volley, OkHttp y Retrofit con manejo de fallos.
- Estudiante: Melvin Peña (confirmar nombre completo).
- Modalidad: individual; valor: 5 puntos / 5 %.
- Entrega: 13 de noviembre de 2026, a las 23:59, según el enunciado.
- Repositorio local: Tarea2.5-Volley-OkHttp-Retrofit.
- Repositorio: https://github.com/meledpeca93/Tarea2.5-Volley-OkHttp-Retrofit
- Confirmar que corresponde al repositorio del equipo exigido por el enunciado.
- PR utilizado: https://github.com/meledpeca93/Tarea2.5-Volley-OkHttp-Retrofit/pull/3
- PR #3 integrado en main; revisión de otro integrante no acreditada.
- PR de entrega actual: https://github.com/meledpeca93/Tarea2.5-Volley-OkHttp-Retrofit/pull/4
- Documento de trabajo actualizado: 12 de noviembre de 2026.

Este archivo es la fuente de contenido del PDF individual generado. Los resultados pendientes no representan
pruebas ejecutadas ni deben presentarse como evidencia de cumplimiento.

## Contexto y objetivo

La tarea continúa el consumo de la API de Bitácora de campo realizado con Retrofit
en la tarea 2.3. Cada estudiante debe implementar la misma consulta protegida con
Volley y OkHttp y comparar los tres clientes. El objetivo es comprobar el envío
de JWT, la conversión de respuestas, los tiempos y el comportamiento ante fallos.

## Estado comprobado

| Requisito | Estado y evidencia disponible |
| --- | --- |
| Pantalla de laboratorio con tres clientes | Implementada en ComparativaActivity.kt |
| GET /api/registros | Verificado con JWT en los tres clientes; ver evidencias/jwt-android.txt |
| JWT en Authorization | Verificado: acceso válido y 401 sin token en los tres clientes |
| Volley: JsonArrayRequest, getHeaders y DefaultRetryPolicy | Implementado |
| OkHttp: Request manual, enqueue y Gson | Implementado |
| Cantidad de registros y duración | Implementadas |
| Manejo del hilo de OkHttp | Usa runOnUiThread |
| Reutilización del repositorio Retrofit de tarea 2.3 | Implementada: CRUD y laboratorio usan RegistroRepository |
| Cinco escenarios de fallo por cliente | Verificados: 15 casos; ver evidencias/LABORATORIO_FALLOS.md |
| Cinco consultas exitosas por cliente | Verificadas: 15 mediciones; ver evidencias/LABORATORIO_MEDICIONES.md |
| Rama personal, commits y PR revisado | Pendiente de ordenar/verificar |
| PDF con evidencias y recomendación | Generado en la raíz; pendiente de revisión final del estudiante |
| Video individual con cámara, 3–4 minutos | Pendiente |

## Implementación y correcciones realizadas

- Volley utiliza JsonArrayRequest y sobrescribe getHeaders para enviar el JWT.
- OkHttp construye el Request, ejecuta enqueue y convierte el cuerpo con Gson.
- Retrofit declara el endpoint mediante una interfaz y utiliza GsonConverterFactory.
- Se muestran cantidad recibida y duración con SystemClock.elapsedRealtime.
- Se identifican HTTP 401 y 500, timeout, resolución de host, conexión, TLS y JSON inválido.
- Se valida la URL con el parser de OkHttp y se rechazan tokens con caracteres no válidos.
- Se impiden solicitudes simultáneas en la misma pantalla.
- Volley etiqueta sus solicitudes; los tres clientes cancelan las pendientes al destruir la actividad.
- Se registran resultados en Logcat bajo ComparativaHTTP sin JWT, URL ni cuerpo de respuesta.

## Metodología para las mediciones

1. Usar el mismo dispositivo, API, endpoint, token, timeout y conjunto de datos.
2. Usar la URL final del servidor, sin redirecciones.
3. Mantener condiciones de red estables y ejecutar las consultas individualmente.
4. Registrar cinco respuestas exitosas por cliente, su cantidad de registros y duración.
5. Anotar el orden de ejecución y si hubo calentamiento; no mezclar resultados fallidos con promedios exitosos.
6. Calcular promedio = (t1 + t2 + t3 + t4 + t5) / 5.

Volley tiene cero reintentos configurados; OkHttp y Retrofit desactivan
retryOnConnectionFailure. No se configura caché de disco en OkHttp; Volley
desactiva su caché. Los tres envían Cache-Control: no-cache, no-store.
Esto no garantiza el comportamiento de proxies o de toda la pila de transporte.

El timeout configura el socket de Volley y conexión, lectura y escritura en
OkHttp/Retrofit. No es un límite total de extremo a extremo. La duración incluye
preparación del cliente, red, conversión y entrega en la interfaz. Los clientes
OkHttp/Retrofit se construyen por consulta; esta condición debe declararse al
interpretar los resultados y no permite concluir rendimiento general de las librerías.

| Cliente | t1 (ms) | t2 (ms) | t3 (ms) | t4 (ms) | t5 (ms) | Promedio (ms) |
| --- | --- | --- | --- | --- | --- | --- |
| Volley | 827 | 48 | 62 | 61 | 49 | 209.40 |
| OkHttp | 854 | 515 | 791 | 842 | 869 | 774.20 |
| Retrofit | 862 | 983 | 839 | 817 | 846 | 869.40 |

## Pruebas de fallos y evidencias

Resultados reales del 12 de noviembre: 15 casos. Todos conservaron el proceso de la app.

| Escenario | Volley (ms) | OkHttp (ms) | Retrofit (ms) |
| --- | --- | --- | --- |
| API detenida | 143 | 177 | 206 |
| Modo avión sin red | 38 | 15 | 37 |
| Timeout | 2015 | 2092 | 2437 |
| JWT ausente (401) | 999 | 587 | 1006 |
| HTTP 500 | 639 | 754 | 838 |

Estos tiempos corresponden a errores y no sustituyen las mediciones exitosas.
La tabla detallada incluye errores técnicos, mensajes y enlaces individuales a capturas/Logcat:
[evidencias verificadas](evidencias/LABORATORIO_FALLOS.md).

## Selector de escenarios y pruebas realizadas

En ComparativaActivity se agregó un Spinner con rutas fijas definidas en
`data/remote/EscenarioLaboratorio.kt`:

| Selección | Ruta GET | Resultado esperado |
| --- | --- | --- |
| Listado normal | /api/registros | Arreglo de registros |
| Tiempo de espera | /api/pruebas/demora | Demora de 15 segundos, superior al timeout de prueba |
| Error interno | /api/pruebas/error | HTTP 500 controlado |

Volley y OkHttp construyen la URL con la ruta seleccionada. Retrofit llama
`RegistroRepository.consultarLaboratorio`; el caso normal delega en `listar`
y los otros usan `RegistroApi.consultarLaboratorio` con `@Url` y `@Header`.
Los tres conservan JWT y timeout configurados. URL, JWT, timeout y selector quedan
bloqueados durante una solicitud. Logcat incluye escenario y error, sin JWT ni cuerpos.
Release ofrece solo listado normal; el backend registra las rutas de fallo
exclusivamente en Development y exige autenticación.

Verificación ejecutada: `./gradlew :app:assembleDebug`, `./gradlew :app:installDebug`
y `dotnet build backend/Bitacora.sln`. Compilación e instalación correctas.
Los 15 fallos se ejecutaron con timeout de 2000 ms y JWT válido salvo el caso 401.
El 401 documentado en Android corresponde a token ausente; la expiración también
se probó en API durante la fase JWT, sin atribuirla a esta matriz Android.

[Tabla completa de fallos, capturas y Logcat](evidencias/LABORATORIO_FALLOS.md).
Datos estructurados: `evidencias/laboratorio-fallos.json`.
Script reproducible: `scripts/verify_lab_failures.py`; requiere APK instalada,
emulador, backend compilado y puerto 5000 libre. Deja su API activa y restaura la red.
No modifica registros. Se comprobó resultado visible, actividad de laboratorio
y conservación del PID, sin cierre inesperado en los 15 casos.

La primera automatización falló al seleccionar un botón por la evaluación de un
nodo XML; se corrigió el script antes de guardar resultados. La evidencia inicial
de red se tomó durante la transición de modo avión: se repitieron únicamente
esos tres casos tras confirmar ausencia de red predeterminada activa. Las capturas
y Logcat actuales corresponden a las ejecuciones finales. Las horas de Logcat
pertenecen al reloj del emulador. La red quedó restaurada (avión 0, Wi-Fi 1).

## Cuadro comparativo final

Comparación del código y de las pruebas reales del 12 de noviembre de 2026.

| Criterio | Volley | OkHttp directo | Retrofit |
| --- | --- | --- | --- |
| Líneas del adaptador en Activity | 26 | 42 | 18; delega en el repositorio, no es el total |
| Envío de encabezados | Sobrescribe getHeaders; Authorization y Cache-Control | Request.Builder.addHeader; Authorization y Cache-Control | @Header para JWT; @Headers para caché; repositorio resuelve sesión o token explícito |
| Conversión JSON | JsonArrayRequest entrega JSONArray; se cuenta length, sin mapeo a DTO | Gson manual con TypeToken a List<JsonObject>; se cuenta size | GsonConverterFactory a List<RegistroResumen> con DTO tipado |
| Hilo de respuesta | Principal en esta cola Android | Trabajo; runOnUiThread para publicar en pantalla | Principal en configuración Android actual; repositorio entrega ApiResult |
| Error HTTP | VolleyError.networkResponse.statusCode | Response.code, distinto de onFailure de transporte | Response.code y ApiResult.Failure.httpCode en repositorio |
| Error de conexión/timeout | NoConnectionError/ConnectException y TimeoutError observados | ConnectException y SocketTimeoutException observados | onFailure recibe ConnectException/SocketTimeoutException; repositorio traduce a ApiResult.Failure |
| Reintentos | DefaultRetryPolicy con 0 reintentos | retryOnConnectionFailure(false) | Igual ajuste en ApiClient basado en OkHttp |
| Tiempo de espera | Socket mediante DefaultRetryPolicy | connectTimeout, readTimeout y writeTimeout | Los tres ajustes en el OkHttp de ApiClient |
| Caché | Solicitud sin caché y Cache-Control: no-cache, no-store | Sin caché de disco y mismo Cache-Control | Sin caché de disco y mismo Cache-Control |
| Cancelación | Tag de Activity y cancelAll al destruir | Conserva Call y ejecuta cancel al destruir | RequestHandle del repositorio y cancel al destruir |
| Facilidad de prueba actual | Verificado por UI; callback y construcción de solicitud están en Activity | Verificado por UI; Request explícito ayuda a inspeccionar transporte, pero hay callbacks, JSON y UI juntos | Verificado por UI; contrato y errores centralizados facilitan reutilizar casos; constructor actual crea servicio internamente y requeriría inyección para pruebas unitarias aisladas |
| Mantenimiento | Conciso para obtener un arreglo; un cambio de campos requeriría añadir mapeo/validación a objetos | Mayor control manual; hay que mantener cierre de Response, Gson, estados HTTP y cambios de hilo | Contrato y DTO compartidos con CRUD; cambios de rutas/tipos y traducción de errores quedan centralizados |
| Cinco consultas exitosas (ms) | 827, 48, 62, 61, 49 | 854, 515, 791, 842, 869 | 862, 983, 839, 817, 846 |
| Promedio (ms) | 209,40 | 774,20 | 869,40 |
| Cinco fallos por cliente | 5/5, sin cierre inesperado | 5/5, sin cierre inesperado | 5/5, sin cierre inesperado |

### Criterio del conteo de líneas

Se cuentan líneas físicas de Kotlin no vacías, incluidas firmas, llaves y cierres.
Se excluyen líneas completas de comentario, package, imports y decoración visual.
Cada línea cuenta una vez aunque contenga varias instrucciones. El conteo mide
este formato del código, no complejidad ni rendimiento.

La fila principal cuenta únicamente los métodos `requestWithVolley`,
`requestWithOkHttp` y `requestWithRetrofit` en ComparativaActivity, desde su firma
hasta antes del siguiente método. Los auxiliares no se ocultan ni se suman como
si representaran implementaciones independientes equivalentes:

| Soporte fuera de los adaptadores | Líneas | Alcance |
| --- | --- | --- |
| httpClient en ComparativaActivity | 8 | Configuración usada por OkHttp directo |
| RegistroApi.kt completo | 15 | Interfaz Retrofit, anotaciones y DTO; incluye CRUD y rutas de laboratorio |
| ApiClient.kt completo | 31 | Fábrica Retrofit/OkHttp, timeouts, conversor y logging debug |
| RegistroRepository.kt completo | 89 | Repositorio, ApiResult y RequestHandle; CRUD, autorización y errores compartidos |

Volley también depende de la creación de su RequestQueue en onCreate. Los tres
usan auxiliares de UI, reloj, validación, escenario, encabezados o resultados;
no se atribuyen esas líneas compartidas a una librería. El ciclo de vida y la
cola quedan fuera del conteo de los adaptadores. Los archivos Retrofit completos
incluyen funciones que no necesita exclusivamente este GET; sumarlos como costo
mínimo del listado daría una comparación engañosa. Las 18 líneas de Retrofit
expresan la delegación desde la pantalla, no una reducción total equivalente.

Reproducción desde la raíz: `python3 scripts/count_lab_lines.py`.
Salida: [conteo estructurado](evidencias/laboratorio-lineas.json).

### Interpretación de las pruebas

Volley obtuvo el menor promedio observado en esta serie de cinco solicitudes.
Todos manejaron los cinco fallos: API detenida, modo avión sin red activa,
timeout, JWT ausente (401) y error 500. El caso 401 Android usó token ausente;
no se presenta como una prueba de expiración Android. El repositorio Retrofit
muestra un mensaje de conexión más general; los errores técnicos están en Logcat.

Las mediciones usaron un arreglo vacío (0 registros), emulador Pixel 4 XL,
API local Development, JWT válido, timeout de 2000 ms y cinco rondas en orden
Volley → OkHttp → Retrofit. No hubo calentamiento del listado en este proceso;
login y pruebas previas ya habían usado la API. Se incluyen todas las muestras,
incluso la primera. El contenido de datos fue idéntico antes y después.

El reloj incluye preparación, red, conversión y entrega a UI. Volley reutiliza
cola; OkHttp/Retrofit crean cliente por consulta. Retrofit además activa logging
BASIC en debug mediante ApiClient. La conversión de Volley solo produce JSONArray,
OkHttp usa JsonObject y Retrofit DTO tipado. Estas diferencias y el arreglo vacío
limitan la comparación de rendimiento: no se demuestra velocidad general ni costo
de deserializar registros reales. Los timeouts tampoco son un límite total de
extremo a extremo. No se midió carga concurrente ni se ejecutaron pruebas unitarias
con servicios inyectados; la facilidad de prueba es una valoración del diseño.

Consultar [las 15 mediciones y sus evidencias](evidencias/LABORATORIO_MEDICIONES.md)
y [los 15 fallos con capturas y Logcat](evidencias/LABORATORIO_FALLOS.md).

## Recomendación final

Recomiendo Retrofit para el proyecto final de Bitácora de campo. El proyecto ya
comparte RegistroRepository entre el CRUD MVVM y el laboratorio; sus contratos
usan DTO tipados y centralizan autorización, conversión y errores. Si cambia un
campo o una respuesta del backend, ese diseño reduce los puntos de adaptación
en las pantallas. Para nuevas operaciones CRUD mantiene un contrato explícito
en RegistroApi y evita repetir manejo manual de Response y cambios de hilo.

La recomendación se apoya en esa estructura y en los cinco fallos manejados sin
cierre, no en una ventaja de velocidad: Retrofit tuvo 869,40 ms de promedio,
frente a 774,20 ms de OkHttp y 209,40 ms de Volley en esta serie. Los tres
cumplieron el manejo de errores. Volley es una opción práctica para consultas
sencillas de arreglos; OkHttp directo resulta útil cuando se necesita controlar
el transporte y aceptar más código manual. En este proyecto, la reutilización
y los DTO de Retrofit pesan más que el promedio de un listado vacío.

Como mejora futura, conviene inyectar el servicio/cliente y reutilizar el cliente
HTTP para facilitar pruebas aisladas y estudiar costos de preparación. Eso no se
implementó ni se atribuye a las mediciones actuales. Una comparación de rendimiento
más amplia requeriría registros reales y condiciones de construcción equivalentes.

## Punto de continuación — pendientes de tarea 2.5

Estado actualizado al 12 de noviembre de 2026.

### Completado

- [x] Laboratorio con Volley, OkHttp y Retrofit, JWT, cantidad y duración.
- [x] Retrofit reutiliza RegistroRepository del CRUD.
- [x] Selector de listado, demora y HTTP 500 compartido por los tres clientes.
- [x] Rutas de fallos limitadas a Development; selector de fallos solo en debug.
- [x] Compilación API sin errores ni advertencias e instalación debug en Pixel 4 XL.
- [x] Cinco escenarios por cliente: 15 casos con captura y Logcat, sin cierre.
- [x] Red restaurada y API reiniciada después de las pruebas.

### Pendientes en orden

- [x] Realizar cinco consultas exitosas por cliente: 15 mediciones con el mismo
  endpoint, JWT válido, dispositivo, datos, timeout y condiciones de red.
- [x] Calcular promedios y completar cuadro comparativo: conteo de líneas con
  criterio explícito, encabezados, JSON, hilo, errores, reintentos/timeouts y mantenimiento.
- [x] Redactar recomendación final con resultados reales.
- [x] Ordenar rama personal según el enunciado: feature/comparativa-melvin-pena.
  Se conserva el historial previo y se incorpora main sin reescribir commits.
- [x] Publicar y verificar commits de selector, pruebas, evidencias y documentación.
- [x] Abrir PR de continuación hacia main: [PR #4](https://github.com/meledpeca93/Tarea2.5-Volley-OkHttp-Retrofit/pull/4).
- [ ] Obtener revisión real de otro integrante antes de integrar el PR #4.
  El PR #3 anterior ya fue integrado; resolver conflictos no acredita revisión.
- [x] Generar PDF individual con tabla comparativa, matriz de fallos y evidencias.
- [ ] Revisión final del PDF por el estudiante antes de entregar.
- [ ] Grabar video individual de 3–4 minutos, cámara encendida, explicación de
  Volley/OkHttp y demostración de al menos dos fallos.
- [ ] Verificar enlaces y archivos finales para entregar el 13 de noviembre a las 23:59.

### Cómo retomar

1. Comprobar API y emulador; no repetir fallos ya verificados salvo cambios o dudas.
2. Iniciar sesión si el token de 15 minutos venció. Consultar credenciales solo
   en backend/Bitacora.Api/.dev-login.json, sin publicarlas ni modificar su contraseña.
3. Continuar con rama/commits/PR revisado y preparación del PDF y video.
   Las 15 mediciones exitosas ya están guardadas; no repetirlas salvo cambios o dudas.
4. Declarar el orden y las condiciones; excluir tiempos fallidos de los promedios.

Dictado real, orientación EXIF y persistencia de sesión son asuntos de tarea 2.3
fuera del alcance actual. No bloquean los siguientes pasos de la tarea 2.5.

## Guion sugerido del video

- 0:00–0:30: objetivo y pantalla de laboratorio.
- 0:30–1:20: Volley, encabezado JWT y política de espera/reintentos.
- 1:20–2:10: Request, enqueue, Gson y actualización de UI en OkHttp.
- 2:10–3:20: demostración de dos escenarios de fallo reales.
- 3:20–3:50: resultados y recomendación con palabras propias.

## Mediciones exitosas completadas (12 de noviembre de 2026)

Se realizaron cinco consultas autenticadas de listado por cliente, en cinco
rondas Volley → OkHttp → Retrofit, timeout de 2000 ms, mismo emulador y datos.
No se ejecutó calentamiento del listado en este proceso; el login ocurrió antes.
La API ya estaba activa por las pruebas anteriores. Los datos conservaron el mismo
contenido antes y después. No se añadieron ni eliminaron registros de prueba.

[Metodología, quince muestras, promedios y evidencias](evidencias/LABORATORIO_MEDICIONES.md).
Script: `scripts/measure_lab_clients.py`. La app conservó su PID en toda la serie.
Estos promedios describen esta implementación y este conjunto de datos; no
establecen una clasificación universal de las librerías.
Conteo de líneas, valoración de mantenimiento y recomendación final completados.
Los siguientes pasos son revisar el PDF generado, grabar el video y obtener revisión del PR.

## Cuadro comparativo y recomendación completados

La base del informe contiene la comparación final de encabezados, JSON, hilos,
errores, reintentos, timeouts, pruebas, mantenimiento y promedios medidos.
Conteo reproducible: `python3 scripts/count_lab_lines.py`; adaptadores de Activity:
Volley 26, OkHttp 42 y Retrofit 18 líneas, con soporte compartido informado aparte.
Se recomienda Retrofit por DTO, repositorio reutilizado y errores centralizados;
Volley obtuvo el menor promedio de esta serie. No se infiere rendimiento general.
La recomendación declara arreglo vacío, creación de clientes por consulta y
logging BASIC de Retrofit en debug como límites de las mediciones.

Ver el cuadro comparativo de este documento y el
[conteo de líneas](evidencias/laboratorio-lineas.json).
Próximo paso: obtener revisión del PR, revisar el PDF generado y grabar el video individual.

## Publicación Git verificada — 12 de noviembre de 2026

Rama: `feature/comparativa-melvin-pena`, publicada en origin sin force push.
La rama ya existía; se actualizó con fast-forward conservando el historial.
Los conflictos resueltos por el estudiante en el PR #3 ya estaban en main;
se incorporaron antes de preparar los cambios nuevos.

Commits de esta continuación:

- `c0ff331`: selector de escenarios compartidos en laboratorio HTTP.
- `dcee07d`: scripts, quince fallos y quince mediciones con sus evidencias.
- `8654ba5`: cuadro comparativo, recomendación y documentación actualizada.

PR abierto hacia main: [#4](https://github.com/meledpeca93/Tarea2.5-Volley-OkHttp-Retrofit/pull/4).
El PR #3 anterior está integrado. El PR #4 queda abierto para revisión real de
otro integrante; no se atribuye aprobación ni participación aún no realizadas.
La actualización del enlace del PR se guarda en un commit adicional de documentación.

Verificación: diff sin errores de espacios, credenciales locales ignoradas y
ausencia de JWT completos en los archivos de texto revisados. Las pruebas Android
y backend documentadas se conservan; no se repitieron al cambiar solo documentación.
Pendientes de entrega: revisión del compañero, revisión final del PDF y video individual.

## PDF individual generado — 12 de noviembre de 2026

Archivo en la raíz: [Informe_Tarea_2_5_Melvin_Pena.pdf](../Informe_Tarea_2_5_Melvin_Pena.pdf).
Contiene 11 páginas: portada, implementación, metodología y cinco mediciones por
cliente, cuadro comparativo completo, conteo de líneas, los cinco escenarios
con sus quince capturas y extractos de Logcat, recomendación final y trazabilidad
de Git/PR. Los resultados se leen de los JSON de evidencias; no se inventan mediciones.

La portada usa el nombre disponible, Melvin Peña. El informe declara listado vacío,
diferencias de creación de clientes y logging debug; no afirma revisión del
compañero ni realización del video. No incluye credenciales ni JWT completos.

Generador reproducible: `scripts/generate_report_pdf.py`, requiere Python con
ReportLab y Pillow. Ejecutar desde la raíz: `python3 scripts/generate_report_pdf.py`.
Se revisaron visualmente las once páginas, la integridad de las tablas, las quince
capturas y los textos dentro de los límites de página. Tamaño aproximado: 3,59 MiB.
El PDF se deja local para que el estudiante lo revise; no se publica automáticamente.
Pendientes actuales: revisión final del PDF, revisión del compañero y video individual.

## Datos de demostración posteriores a las mediciones

Se agregaron tres registros ficticios a la API local, con
`scripts/seed_demo_records.py`, que evita duplicarlos y conserva otros datos.
Se verificó desde Android que Volley, OkHttp y Retrofit muestran 3 registros
al seleccionar Listado normal. Para ver títulos y detalles se abre la bitácora.
[Logcat de demostración](evidencias/laboratorio-datos-demo.txt); capturas
lab-demo-volley.png, lab-demo-okhttp.png y lab-demo-retrofit.png en evidencias.
El endpoint de demora devuelve un arreglo vacío por diseño.

Estos datos fueron creados después de las quince mediciones originales. No
cambian sus promedios ni el conjunto vacío declarado en el PDF. El PDF, su
generador y esta documentación posterior permanecen locales para revisión;
no se han publicado en GitHub.

## Video guía demostrativo — 13 de noviembre de 2026

Generado localmente: [Video_Guia_Tarea_2_5_Melvin_Pena.mp4](../Video_Guia_Tarea_2_5_Melvin_Pena.mp4).
Duración: 3 minutos 55 segundos; 1920×1080, H.264/AAC, narración sintética
en español. Incluye código real de Volley, JWT, OkHttp y Retrofit y grabaciones
reales del emulador con la interfaz actual: listado con los tres clientes,
timeout con Volley/OkHttp, HTTP 500 con ambos y recuperación con Retrofit.
El login se realizó antes de grabar; no se muestran credenciales ni JWT completos.

La captura actual mostró 4 registros; no se modificaron datos durante el video.
Las mediciones originales con cero registros y sus promedios se mantienen.
El recorrido Android se acelera para ajustarse al tiempo del ejemplo y se
identifica en pantalla. El video es una guía para replicar, no acredita la
explicación individual con cámara exigida por la tarea.

Guion con capítulos: docs/GUION_VIDEO_DEMOSTRATIVO.md.
Verificación de las peticiones: docs/evidencias/video-demostracion-logcat.txt.
Generadores: scripts/record_android_demo.py y scripts/generate_demo_video.py.
Se revisaron fotogramas, resolución, duración y presencia de audio.
Video, guion y cambios nuevos permanecen locales; no se publican automáticamente.

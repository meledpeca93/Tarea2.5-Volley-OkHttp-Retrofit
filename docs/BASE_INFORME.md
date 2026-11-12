# Base del informe individual — Tarea 2.5

## Datos de la entrega

- Tema: comparación práctica de Volley, OkHttp y Retrofit con manejo de fallos.
- Estudiante: Melvin Peña (confirmar nombre completo).
- Modalidad: individual; valor: 5 puntos / 5 %.
- Entrega: 13 de noviembre de 2026, a las 23:59, según el enunciado.
- Repositorio local: Tarea2.5-Volley-OkHttp-Retrofit.
- URL del repositorio del equipo: pendiente de confirmar.
- URL del pull request y revisión del compañero: pendientes de verificar.
- Documento de trabajo actualizado: 11 de noviembre de 2026.

Este archivo es la base del futuro PDF. Los resultados pendientes no representan
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
| GET /api/registros | Configurado en los tres clientes; falta validación real |
| JWT en Authorization | Verificado: acceso válido y 401 sin token en los tres clientes |
| Volley: JsonArrayRequest, getHeaders y DefaultRetryPolicy | Implementado |
| OkHttp: Request manual, enqueue y Gson | Implementado |
| Cantidad de registros y duración | Implementadas |
| Manejo del hilo de OkHttp | Usa runOnUiThread |
| Reutilización del repositorio Retrofit de tarea 2.3 | Implementada: CRUD y laboratorio usan RegistroRepository |
| Cinco escenarios de fallo por cliente | Pendientes: 15 casos |
| Cinco consultas exitosas por cliente | Pendientes: 15 mediciones |
| Rama personal, commits y PR revisado | Pendiente de ordenar/verificar |
| PDF con evidencias y recomendación | Pendiente; este documento es una base |
| Video individual con cámara, 3–4 minutos | Pendiente |

Se verificó una compilación exitosa mediante `./gradlew :app:assembleDebug`
después de las correcciones. En una ejecución anterior se instaló y abrió la app
en el Pixel 4 XL. Esto no demuestra que los cinco escenarios estén superados.

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
| Volley | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |
| OkHttp | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |
| Retrofit | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente | Pendiente |

## Pruebas de fallos y evidencias

Para cada fila, ejecutar los tres clientes. Registrar fecha, configuración,
clase de excepción o estado HTTP, mensaje visible, tiempo, ausencia/presencia de
cierre inesperado y ruta de captura o Logcat. No guardar credenciales en evidencias.

| Escenario | Preparación | Volley | OkHttp | Retrofit | Evidencias |
| --- | --- | --- | --- | --- | --- |
| API detenida | Detener la API manteniendo la red disponible | Pendiente | Pendiente | Pendiente | Pendiente |
| Modo avión | Activarlo y comprobar que Wi-Fi/datos estén realmente desactivados | Pendiente | Pendiente | Pendiente | Pendiente |
| Timeout | Endpoint temporal de API con await Task.Delay superior al timeout; adaptar los tres clientes al mismo endpoint | Pendiente | Pendiente | Pendiente | Pendiente |
| HTTP 401 | Omitir JWT o usar uno vencido en endpoint protegido | Pendiente | Pendiente | Pendiente | Pendiente |
| HTTP 500 | Preparar un fallo controlado en la API de desarrollo | Pendiente | Pendiente | Pendiente | Pendiente |

La app todavía fija api/registros: el endpoint temporal para timeout y la
provocación controlada de 500 requieren integración con el backend. Un JSON
simulado o una compilación exitosa no sustituye estas pruebas.

Plantilla de registro por ejecución:

- Identificador / fecha / cliente / escenario:
- Dispositivo, versión de app y timeout:
- Preparación realizada:
- Error técnico recibido:
- Mensaje que vio el usuario:
- Duración y continuidad de la app:
- Captura o Logcat:

## Cuadro comparativo preliminar

Descripción basada en el código actual, pendiente de completar con datos medidos.

| Criterio | Volley | OkHttp | Retrofit |
| --- | --- | --- | --- |
| Líneas de código | Pendiente de contar | Pendiente de contar | Pendiente de contar incluyendo interfaz |
| Encabezados | getHeaders | addHeader en Request.Builder | @Header y @Headers |
| JSON | JsonArrayRequest entrega JSONArray | Gson manual a List<JsonObject> | GsonConverterFactory a List<RegistroResumen> |
| Hilo del callback actual | Principal | Trabajo; se usa runOnUiThread | Principal con configuración Android predeterminada |
| Errores | VolleyError y networkResponse.statusCode | IOException y estado de Response | onFailure y Response.code |
| Reintentos | DefaultRetryPolicy con 0 | retryOnConnectionFailure(false) | Mismo ajuste de OkHttp |
| Timeout | Socket mediante DefaultRetryPolicy | Conexión, lectura y escritura | Cliente OkHttp configurado |
| Prueba y mantenimiento | Evaluar tras las pruebas | Evaluar tras las pruebas | Evaluar tras integrar tarea 2.3 |
| Promedio de cinco consultas | Pendiente | Pendiente | Pendiente |

Para contar líneas, documentar si se incluyen blancos, comentarios, auxiliares,
interfaz y código compartido. Excluir decoración visual y usar el mismo criterio
para los tres clientes. La valoración de mantenimiento debe justificar ejemplos
concretos del código, no solo preferencias.

## Recomendación provisional

Retrofit es un candidato para el proyecto final por declarar el contrato en una
interfaz y delegar la conversión JSON. La recomendación definitiva debe incorporar
las mediciones reales, los errores observados y la integración del repositorio
existente. No se afirma todavía que sea el cliente más rápido.

## Ruta para completar la entrega

1. Confirmar repositorio del equipo y recuperar el repositorio Retrofit de tarea 2.3.
2. Llevar el trabajo a feature/comparativa-melvin-pena sin perder cambios y hacer commits descriptivos.
3. Validar consulta autenticada real con los tres clientes.
4. Ejecutar y documentar las 15 pruebas de fallo.
5. Registrar las 15 mediciones exitosas y completar el cuadro.
6. Redactar conclusiones, incorporar capturas y exportar PDF.
7. Crear/verificar PR hacia principal y solicitar revisión de otro integrante.
8. Grabar video individual con cámara: explicar Volley y OkHttp y mostrar al menos dos fallos.
9. Entregar enlace de PR, PDF y video según el aula.

## Guion sugerido del video

- 0:00–0:30: objetivo y pantalla de laboratorio.
- 0:30–1:20: Volley, encabezado JWT y política de espera/reintentos.
- 1:20–2:10: Request, enqueue, Gson y actualización de UI en OkHttp.
- 2:10–3:20: demostración de dos escenarios de fallo reales.
- 3:20–3:50: resultados y recomendación con palabras propias.


## Actualización del alcance

Se confirmó que tanto la API de tarea 2.2 como el cliente MVVM de tarea 2.3 deben
crearse desde cero. Las indicaciones anteriores de recuperar un repositorio
Retrofit existente se sustituyen por construir ese repositorio y reutilizarlo
en el laboratorio. Se requiere el enunciado 2.2 antes de cerrar el contrato y
las validaciones del servidor. No hay aún CRUD ni backend funcional acreditado.

Se prepararon dependencias MVVM, fábrica HTTP, estados de pantalla y restricción
de HTTP a 10.0.2.2 en debug. Esta infraestructura no demuestra que los requisitos
funcionales de tarea 2.3 estén cumplidos.


## Avance: backend de tarea 2.2 implementado

- Solución `backend/Bitacora.sln` con Domain, Application, Infrastructure y Api.
- Registro con ID entero, título, descripción, foto Base64 opcional y fecha del servidor.
- DTO separados para listado sin foto y detalle; servicio con cinco casos de uso.
- Repositorio EF Core SQLite, migración InitialCreate generada y aplicada a la base local.
- Validación por campo y manejo global con ProblemDetails genérico para 500.
- OpenAPI exportado en backend/openapi.json y Swagger UI en /swagger (Development).
- Colección Postman exportada y entorno baseUrl incluidos en postman/.
- Rutas de demora 15 s y error 500 registradas únicamente en Development.
- .gitignore excluye bin, obj y bases SQLite.

Verificación ejecutada: `dotnet build backend/Bitacora.sln` terminó sin errores ni
advertencias. La colección final se ejecutó contra la API real local: 17 solicitudes,
25 aserciones, 0 fallos, exit code 0. Evidencia:
`docs/evidencias/postman-api-local.txt`. Comprueba CRUD, Location, listado sin foto,
404, validación de título/descripción/Base64 y 500 sin excepción interna en el cuerpo.
No acredita aún el límite exacto de 2 MiB con una prueba de frontera, JWT, ni fallos
observados desde Android. Los tiempos de Postman no son las mediciones Android.

La opción de reporte --output del CLI exigió login; se guardó salida de consola
sin publicar resultados. Siguen pendientes capturas visuales, distribución del
trabajo, PDF y participación/revisión real en GitHub. La fase Android 2.3 continúa
pendiente: no se debe presentar el backend como cumplimiento del cliente MVVM.


## Avance Android — integración de tarea 2.3

Se implementó la bitácora con ViewModel/LiveData, repositorio Retrofit compartido,
DTO tipados, listado RecyclerView, detalle, creación/edición y eliminación con
confirmación. El laboratorio Retrofit ya reutiliza el mismo RegistroRepository.
La captura reduce a 1024 px y JPEG 70 antes de enviar Base64; el formulario permite
obtener la descripción mediante RecognizerIntent y valida límites del contrato.
Se muestran Loading con controles deshabilitados, Empty, Success y Error; los
400 del servidor se presentan con sus detalles por campo.

La compilación/instalación Android y la consulta de listado vacío en Pixel fueron
verificadas. Ver el reporte `docs/evidencias/android-crud-smoke.txt` para el resultado
final del recorrido CRUD desde UI; no confundirlo con pruebas de cámara/voz ni con
los quince escenarios de comparación, que siguen pendientes.

Pendientes principales para 2.5: JWT, selección de endpoints de fallo, quince
escenarios documentados y quince mediciones exitosas. Para entrega: PDF, videos,
capturas de Logcat y revisión real del PR. No se consideran cumplidos por compilar.


## Evidencias Android verificadas (11 de noviembre de 2026)

| Prueba | Resultado | Evidencia |
| --- | --- | --- |
| CRUD desde UI y verificación en API | Pasó: crear, consultar, editar y eliminar; cancelar borrado conserva datos | evidencias/android-crud-smoke.txt |
| Foto desde cámara del emulador | Pasó: JPEG 768×1024, 13 234 bytes; enviada Base64 y recuperada del backend | evidencias/android-fotografia.txt |
| Modo avión + Wi-Fi apagado | Pasó: mensaje sin conexión, Reintentar y recuperación tras restaurar red | evidencias/android-conexion.txt |
| API detenida con red activa | Ver reporte de ejecución | evidencias/android-api-detenida.txt |
| Peticiones/respuestas en Logcat | Capturadas sin cuerpos ni credenciales | evidencias/android-http-logcat.txt |

Capturas: android-crud-creado.png, android-crud-eliminado.png,
android-formulario-fotografia.png, android-detalle-fotografia.png y
android-sin-conexion.png. La imagen en la carpeta depuracion corresponde a un fallo
previo del script y no debe utilizarse como evidencia final del comportamiento.
Los registros creados por las pruebas exitosas fueron eliminados.

Estas pruebas acreditan la bitácora Retrofit. No completan la matriz de quince
fallos de la tarea 2.5 ni la media de cinco ejecuciones por cliente. Falta dictado
real por micrófono, protección JWT, mediciones y entregables académicos.


## Avance JWT de tarea 2.5

Se implementaron protección JwtBearer del CRUD, login local de laboratorio y
sesión Android en memoria. El mismo token se precarga para Volley, OkHttp y
Retrofit. Las credenciales se generan localmente, fuera de Git, y no se deben
incluir en el PDF/video/Logcat. El token dura 15 minutos.

La verificación agrupada se documenta en evidencias/jwt-api.txt,
evidencias/jwt-android.txt y evidencias/postman-api-jwt.txt al finalizar su ejecución.
Distinguir esos resultados de las pruebas anteriores sin JWT. El login solo está
disponible en Development; no acredita una plataforma de identidad de producción
ni requisitos adicionales de la tarea 2.4.


## Resultado final de la fase JWT

Verificación concluida correctamente:

- API: 15 comprobaciones de autenticación; todas pasaron. Se probaron ausencia,
  formato inválido, alteración, vencimiento, emisor y audiencia incorrectos.
- Postman actualizado: 20 solicitudes y 29 aserciones, 0 fallos, exit code 0.
- Android real en Pixel: login válido; Volley, OkHttp y Retrofit recibieron 200
  consultando GET /api/registros con el token precargado de sesión.
- Al borrar el campo JWT, cada cliente recibió 401 y mostró el mensaje de token
  ausente/inválido/vencido sin cierre inesperado. La sesión de inicio de la app
  sigue disponible; volver a abrir el laboratorio precarga otra vez el token.

Evidencias: [API](evidencias/jwt-api.txt),
[Android](evidencias/jwt-android.txt) y
[Postman](evidencias/postman-api-jwt.txt).

Los tiempos de esta comprobación NO son los promedios de cinco ejecuciones:
solo acreditan autenticación. El listado estaba vacío, por lo que se verificó
acceso protegido y cero registros; la comparación final debe mantener un conjunto
de datos estable y documentar su cantidad.

### Matriz 2.5 al cerrar JWT

| Escenario | Volley | OkHttp | Retrofit |
| --- | --- | --- | --- |
| Token ausente (401) | Verificado | Verificado | Verificado |
| API detenida | Pendiente en laboratorio | Pendiente en laboratorio | Probado en CRUD; repetir/documentar en laboratorio |
| Modo avión | Pendiente en laboratorio | Pendiente en laboratorio | Probado en CRUD; repetir/documentar en laboratorio |
| Tiempo de espera | Pendiente | Pendiente | Pendiente |
| HTTP 500 | Pendiente en Android | Pendiente en Android | Pendiente en Android |

Próxima fase: habilitar la selección de los endpoints de prueba en el laboratorio
para timeout/500, completar los doce casos restantes y registrar cinco ejecuciones
exitosas por cliente. Luego finalizar cuadro comparativo, recomendación, PDF,
video y rama/commits/PR revisado. La fase actual no completa por sí sola la tarea.

## Punto de continuación — pendientes de tarea 2.5

Estado al pausar el trabajo, 11 de noviembre de 2026. Retomar esta noche desde
esta lista. No iniciar nuevamente las fases ya verificadas salvo cambios o fallos.

### Ya verificado

- Laboratorio con Volley, OkHttp y Retrofit para GET /api/registros.
- JWT local y login Android; mismo token compartido por los tres clientes.
- Acceso exitoso autenticado y respuesta 401 al omitir el token en cada cliente.
- Retrofit reutiliza el repositorio del CRUD MVVM.
- API local, compilación e instalación Android funcionando en la última verificación.
- Evidencias JWT: jwt-api.txt, jwt-android.txt y postman-api-jwt.txt en docs/evidencias.

### Pendientes en orden

- [ ] Preparar selección de escenarios del laboratorio: GET /api/registros para
  consulta normal, /api/pruebas/demora para timeout y /api/pruebas/error para 500.
  Los tres clientes deben consultar la misma ruta en cada escenario, con JWT
  válido, y las rutas de prueba deben permanecer solo en Development.
- [ ] API detenida: ejecutar Volley, OkHttp y Retrofit; registrar error técnico,
  mensaje visible, duración, continuidad de la app y captura o Logcat.
- [ ] Modo avión: ejecutar los tres clientes con Wi-Fi/datos realmente apagados;
  guardar la misma evidencia y restaurar la conexión al terminar.
- [ ] Timeout: ejecutar los tres clientes con espera configurada menor a los
  15 segundos del endpoint de demora; documentar excepción y mensaje.
- [ ] HTTP 500: ejecutar los tres clientes contra el fallo controlado y documentar
  estado HTTP, mensaje y ausencia de cierre inesperado.
- [ ] Consolidar las 15 pruebas de fallos en la tabla final: 3 casos 401 ya
  verificados y 12 casos restantes. Las pruebas previas del CRUD Retrofit de API
  detenida/modo avión no sustituyen la matriz completa del laboratorio.
- [ ] Realizar cinco consultas exitosas por cliente: 15 mediciones en total,
  mismo endpoint, token válido, dispositivo, timeout, condiciones de red y datos.
  Registrar cantidad de registros y calcular tres promedios. Los tiempos aislados
  de la verificación JWT no son esos promedios.
- [ ] Completar cuadro comparativo: líneas de código con criterio de conteo
  explícito, encabezados, JSON, hilo del callback, errores, reintentos/timeouts,
  facilidad de prueba/mantenimiento y promedios de cinco ejecuciones.
- [ ] Redactar recomendación final apoyada en el código y resultados reales.
- [ ] Ordenar GitHub: llevar todo a feature/comparativa-melvin-pena sin perder
  cambios, realizar commits descriptivos y preparar PR hacia principal con
  revisión real de otro integrante. Verificar el estado antes de modificar ramas.
- [ ] Preparar PDF individual con comparación, tabla de fallos, capturas/Logcat
  y recomendación; no incluir contraseñas, claves ni JWT completos.
- [ ] Grabar video individual de 3–4 minutos con cámara encendida: explicar
  Volley y OkHttp con palabras propias y demostrar al menos dos fallos.
- [ ] Verificar enlaces/archivos finales de PR, PDF y video para la entrega.

### Cómo retomar

1. Confirmar que la API local y el Pixel están disponibles; si la API está
   detenida, seguir backend/README.md para iniciarla en Development.
2. Iniciar sesión en Android si el proceso murió o el token de 15 minutos venció.
   Consultar credenciales solo en backend/Bitacora.Api/.dev-login.json; no editar
   ese archivo para cambiar la contraseña. El usuario decidió conservar la actual
   y no añadir una opción de cambio.
3. Empezar por la selección de endpoints de prueba y los doce casos pendientes.
4. Agrupar compilación, instalación y verificaciones en acciones concretas para
   reducir solicitudes de permisos. No repetir comprobaciones ya exitosas sin
   un cambio, fallo o duda que lo justifique.

La entrega sigue incompleta hasta finalizar las pruebas, mediciones y entregables.
El dictado real por micrófono y otras verificaciones específicas de tarea 2.3
son pendientes separados; no confundirlos con los requisitos de esta lista 2.5.

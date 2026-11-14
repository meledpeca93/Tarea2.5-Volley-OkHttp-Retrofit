# Video guía para replicar — Tarea 2.5

Ejemplo con narración sintética y grabación real del emulador. No sustituye el video individual con cámara.
Login previo a la grabación; no se muestran contraseñas ni JWT completos.

## 00:00–00:18 · Una API. Tres clientes HTTP.

Mostrar: ComparativaActivity.kt.

En este ejemplo explico cómo consumir la API de Bitácora de campo con Volley, OkHttp y Retrofit. Los tres consultan el mismo endpoint protegido con un token JWT. Primero revisaré el código y después mostraré el uso real de Android: listado, tiempo agotado y error quinientos.

## 00:18–00:50 · Volley: solicitud y respuesta

Mostrar: ComparativaActivity.kt · requestWithVolley().

Con Volley creo un JsonArrayRequest porque el servidor devuelve un arreglo JSON. La solicitud usa GET y la misma URL del laboratorio. Si responde correctamente, cuento los elementos con length y muestro el tiempo. Si falla, llamo al manejador de errores. Sobrescribo getHeaders para enviar el token. DefaultRetryPolicy establece el tiempo de espera y cero reintentos. Desactivo la caché y agrego la solicitud a la cola. Estos callbacks llegan al hilo principal.

## 00:50–01:06 · JWT y tiempo de respuesta

Mostrar: ComparativaActivity.kt · authorizationHeaders().

El token lo genera la API después del inicio de sesión. Este auxiliar lo envía en Authorization con el prefijo Bearer. Si el campo está vacío, no agrega ese encabezado. La duración se calcula con un reloj monotónico, desde el inicio hasta publicar el resultado.

## 01:06–01:49 · OkHttp: control manual y asincronía

Mostrar: ComparativaActivity.kt · requestWithOkHttp().

En OkHttp construyo manualmente el Request con la URL, los encabezados y GET. NewCall prepara la llamada y enqueue la ejecuta de forma asíncrona. OnFailure recibe fallos de transporte, como conexión o timeout. Un estado HTTP quinientos llega a onResponse porque el servidor sí respondió, por eso reviso isSuccessful. Response use cierra la respuesta. Gson y TypeToken convierten el cuerpo a una lista de objetos JSON. Estos callbacks trabajan fuera del hilo principal. Por eso uso runOnUiThread antes de actualizar la pantalla.

## 01:49–02:03 · Retrofit reutiliza el repositorio

Mostrar: ComparativaActivity.kt · requestWithRetrofit().

Retrofit reutiliza RegistroRepository del CRUD. El repositorio consulta el escenario seleccionado y entrega ApiResult de éxito o de error. El conversor transforma el JSON a objetos tipados y la pantalla presenta el resultado.

## 02:03–02:30 · Android: consulta exitosa

Mostrar: Grabación real del emulador · listado normal.

Ahora uso la aplicación real. Ya inicié sesión sin mostrar las credenciales. Configuro dos mil milisegundos y selecciono Listado normal. Pruebo Volley, OkHttp y Retrofit. Cada cliente consulta la API y muestra la cantidad recibida y el tiempo. Para esta demostración hay registros guardados. El laboratorio muestra la cantidad; los títulos y detalles se consultan en la bitácora de campo.

## 02:30–02:58 · Android: tiempo de espera agotado

Mostrar: Grabación real · demora de 15 s / timeout de 2000 ms.

Selecciono el escenario de demora. El backend espera quince segundos mediante Task Delay, pero el cliente tiene un timeout de dos segundos. Ejecuto Volley y después OkHttp. Ambos muestran tiempo de espera agotado. Ese error es el resultado esperado de la prueba. La pantalla continúa abierta y me permite ejecutar otra consulta. Para este escenario mantengo el JWT válido.

## 02:58–03:30 · Android: error 500 y recuperación

Mostrar: Grabación real · fallo controlado en Development.

Ahora selecciono Error interno y pruebo los dos clientes. La API provoca una excepción controlada y devuelve HTTP quinientos. La aplicación identifica el estado y muestra un mensaje, sin cerrarse. No es un error de conexión: el servidor respondió con un estado de error. Al final vuelvo a Listado normal y ejecuto Retrofit. La consulta exitosa demuestra que la aplicación sigue funcionando después de los fallos.

## 03:30–03:54 · Resultados y recomendación

Mostrar: Informe individual · mediciones originales.

Realicé cinco consultas exitosas por cliente y cinco escenarios de fallo con cada uno. En la serie original, con un listado vacío, Volley tuvo el menor promedio. Los registros actuales se agregaron después y no cambian esas mediciones. Recomiendo Retrofit para el proyecto final por sus objetos tipados, el repositorio compartido y el manejo centralizado de errores. Esta es una guía: replica el recorrido con tus propias palabras y la cámara encendida.

Duración total aproximada: 03:54.

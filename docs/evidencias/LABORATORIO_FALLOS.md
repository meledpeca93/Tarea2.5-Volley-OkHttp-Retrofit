# Pruebas de fallos del laboratorio — Tarea 2.5

Ejecución: 12 de noviembre de 2026. Pixel 4 XL emulado, APK debug y API local Development.
Timeout configurado: 2000 ms; demora del servidor: 15 segundos con await Task.Delay.
JWT válido de sesión en todos los escenarios salvo el caso de token ausente.
Los tiempos son de solicitudes fallidas; no se utilizan como promedios de rendimiento.
La hora de Logcat corresponde al reloj del emulador.

| Escenario | Cliente | Error técnico | Mensaje visible y duración | Sin cierre | Evidencia |
| --- | --- | --- | --- | --- | --- |
| HTTP 500 | Volley | HTTP 500 | Volley: Error interno del servidor (500). (639 ms). | Sí | [Captura](lab-error-500-volley.png), [Logcat](lab-error-500-volley.txt) |
| HTTP 500 | OkHttp | HTTP 500 | OkHttp: Error interno del servidor (500). (754 ms). | Sí | [Captura](lab-error-500-okhttp.png), [Logcat](lab-error-500-okhttp.txt) |
| HTTP 500 | Retrofit | HTTP 500 | Retrofit: Error interno del servidor (500). (838 ms). | Sí | [Captura](lab-error-500-retrofit.png), [Logcat](lab-error-500-retrofit.txt) |
| Timeout | Volley | TimeoutError | Volley: Tiempo de espera agotado. (2015 ms). | Sí | [Captura](lab-timeout-volley.png), [Logcat](lab-timeout-volley.txt) |
| Timeout | OkHttp | SocketTimeoutException <- SocketTimeoutException | OkHttp: Tiempo de espera agotado. (2092 ms). | Sí | [Captura](lab-timeout-okhttp.png), [Logcat](lab-timeout-okhttp.txt) |
| Timeout | Retrofit | SocketTimeoutException | Retrofit: Tiempo de espera agotado. (2437 ms). | Sí | [Captura](lab-timeout-retrofit.png), [Logcat](lab-timeout-retrofit.txt) |
| API detenida | Volley | NoConnectionError <- ConnectException | Volley: No se pudo conectar con la API. Revisa la red y que el servidor esté activo. (143 ms). | Sí | [Captura](lab-api-detenida-volley.png), [Logcat](lab-api-detenida-volley.txt) |
| API detenida | OkHttp | ConnectException <- ConnectException <- ErrnoException | OkHttp: No se pudo conectar con la API. Revisa la red y que el servidor esté activo. (177 ms). | Sí | [Captura](lab-api-detenida-okhttp.png), [Logcat](lab-api-detenida-okhttp.txt) |
| API detenida | Retrofit | ConnectException | Retrofit: Sin conexión con la API. Revisa la red y el servidor. (206 ms). | Sí | [Captura](lab-api-detenida-retrofit.png), [Logcat](lab-api-detenida-retrofit.txt) |
| JWT ausente (401) | Volley | HTTP 401 | Volley: Token ausente, inválido o vencido (401). (999 ms). | Sí | [Captura](lab-sin-token-401-volley.png), [Logcat](lab-sin-token-401-volley.txt) |
| JWT ausente (401) | OkHttp | HTTP 401 | OkHttp: Token ausente, inválido o vencido (401). (587 ms). | Sí | [Captura](lab-sin-token-401-okhttp.png), [Logcat](lab-sin-token-401-okhttp.txt) |
| JWT ausente (401) | Retrofit | HTTP 401 | Retrofit: Token ausente, inválido o vencido (401). (1006 ms). | Sí | [Captura](lab-sin-token-401-retrofit.png), [Logcat](lab-sin-token-401-retrofit.txt) |
| Modo avión | Volley | NoConnectionError <- ConnectException | Volley: No se pudo conectar con la API. Revisa la red y que el servidor esté activo. (38 ms). | Sí | [Captura](lab-modo-avion-volley.png), [Logcat](lab-modo-avion-volley.txt) |
| Modo avión | OkHttp | ConnectException <- ConnectException <- ErrnoException | OkHttp: No se pudo conectar con la API. Revisa la red y que el servidor esté activo. (15 ms). | Sí | [Captura](lab-modo-avion-okhttp.png), [Logcat](lab-modo-avion-okhttp.txt) |
| Modo avión | Retrofit | ConnectException | Retrofit: Sin conexión con la API. Revisa la red y el servidor. (37 ms). | Sí | [Captura](lab-modo-avion-retrofit.png), [Logcat](lab-modo-avion-retrofit.txt) |

Se comprobó resultado visible, actividad de laboratorio y mismo PID durante las pruebas.
API detenida: se detuvo únicamente el proceso iniciado por el script y se reinició después.
Modo avión: Wi-Fi y datos desactivados; se esperó hasta no tener red predeterminada activa.
Ver [estado de red](laboratorio-modo-avion-red.txt). La conexión fue restaurada al terminar.
Los tres casos 401 tras restaurar la red también verifican que la API vuelve a ser accesible.
No se crearon, modificaron ni eliminaron registros del usuario.

Reproducción: con API compilada, puerto 5000 libre, APK debug instalada y emulador activo,
ejecutar `python3 scripts/verify_lab_failures.py`. El script deja su API activa.

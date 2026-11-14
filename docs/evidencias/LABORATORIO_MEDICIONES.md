# Mediciones exitosas — Tarea 2.5

Fecha: 12 de noviembre de 2026. Pixel 4 XL emulado; APK debug; API local Development.
GET /api/registros en http://10.0.2.2:5000/, JWT válido de sesión y timeout de 2000 ms.
Conjunto de datos: 0 registros, contenido idéntico antes y después (verificación SHA-256 en memoria).
No se añadieron, editaron ni eliminaron registros para estas mediciones.
Un listado vacío mide la respuesta y conversión del arreglo vacío; no permite evaluar el costo de convertir volúmenes de objetos.
Wi-Fi del emulador activo y modo avión desactivado; no se alteró la red durante la serie.
Orden: cinco rondas Volley → OkHttp → Retrofit, una solicitud a la vez.
Sin consultas de calentamiento del listado en este proceso. El login Retrofit ocurre antes de medir.
La API y el emulador ya habían ejecutado pruebas previas; no representan un arranque frío del servidor.
No se descartó ninguna muestra exitosa ni se incluyeron fallos en los promedios.
Duración monotónica medida por la app: preparación, red, conversión y entrega a UI.
Volley reutiliza cola; OkHttp y Retrofit construyen cliente por consulta. No se extrapola a otras implementaciones.
Sin caché de aplicación y sin reintentos configurados; los tres envían Cache-Control: no-cache, no-store.
La automatización toma capturas entre solicitudes; no es una prueba de carga ni de concurrencia.
Las marcas horarias de Logcat corresponden al reloj del emulador.

| Cliente | t1 (ms) | t2 (ms) | t3 (ms) | t4 (ms) | t5 (ms) | Promedio (ms) |
| --- | --- | --- | --- | --- | --- | --- |
| Volley | 827 | 48 | 62 | 61 | 49 | 209.40 |
| OkHttp | 854 | 515 | 791 | 842 | 869 | 774.20 |
| Retrofit | 862 | 983 | 839 | 817 | 846 | 869.40 |

Promedio = suma de las cinco duraciones / 5.

| Orden | Cliente | Ejecución | Registros | Duración (ms) | Evidencia |
| --- | --- | --- | --- | --- | --- |
| 1 | Volley | 1 | 0 | 827 | [Captura](lab-medicion-volley-1.png), [Logcat](lab-medicion-volley-1.txt) |
| 2 | OkHttp | 1 | 0 | 854 | [Captura](lab-medicion-okhttp-1.png), [Logcat](lab-medicion-okhttp-1.txt) |
| 3 | Retrofit | 1 | 0 | 862 | [Captura](lab-medicion-retrofit-1.png), [Logcat](lab-medicion-retrofit-1.txt) |
| 4 | Volley | 2 | 0 | 48 | [Captura](lab-medicion-volley-2.png), [Logcat](lab-medicion-volley-2.txt) |
| 5 | OkHttp | 2 | 0 | 515 | [Captura](lab-medicion-okhttp-2.png), [Logcat](lab-medicion-okhttp-2.txt) |
| 6 | Retrofit | 2 | 0 | 983 | [Captura](lab-medicion-retrofit-2.png), [Logcat](lab-medicion-retrofit-2.txt) |
| 7 | Volley | 3 | 0 | 62 | [Captura](lab-medicion-volley-3.png), [Logcat](lab-medicion-volley-3.txt) |
| 8 | OkHttp | 3 | 0 | 791 | [Captura](lab-medicion-okhttp-3.png), [Logcat](lab-medicion-okhttp-3.txt) |
| 9 | Retrofit | 3 | 0 | 839 | [Captura](lab-medicion-retrofit-3.png), [Logcat](lab-medicion-retrofit-3.txt) |
| 10 | Volley | 4 | 0 | 61 | [Captura](lab-medicion-volley-4.png), [Logcat](lab-medicion-volley-4.txt) |
| 11 | OkHttp | 4 | 0 | 842 | [Captura](lab-medicion-okhttp-4.png), [Logcat](lab-medicion-okhttp-4.txt) |
| 12 | Retrofit | 4 | 0 | 817 | [Captura](lab-medicion-retrofit-4.png), [Logcat](lab-medicion-retrofit-4.txt) |
| 13 | Volley | 5 | 0 | 49 | [Captura](lab-medicion-volley-5.png), [Logcat](lab-medicion-volley-5.txt) |
| 14 | OkHttp | 5 | 0 | 869 | [Captura](lab-medicion-okhttp-5.png), [Logcat](lab-medicion-okhttp-5.txt) |
| 15 | Retrofit | 5 | 0 | 846 | [Captura](lab-medicion-retrofit-5.png), [Logcat](lab-medicion-retrofit-5.txt) |

Reproducción: API local y emulador activos, APK debug instalada, credenciales locales disponibles:
`python3 scripts/measure_lab_clients.py`. Genera JSON, capturas, Logcat y este informe.
La aplicación permaneció en el laboratorio con el mismo PID durante las 15 consultas.

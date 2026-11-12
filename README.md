# Bitácora de campo y comparativa HTTP

Proyecto Android Kotlin con backend ASP.NET Core para las tareas 2.2, 2.3 y 2.5.

## Componentes

- `backend/`: API CRUD en cuatro capas con EF Core SQLite, validación y OpenAPI.
- `app/`: bitácora con MVVM/LiveData, repositorio Retrofit, listado RecyclerView,
  detalle, formulario con cámara/voz y eliminación con confirmación.
- Laboratorio: misma consulta con Volley, OkHttp directo y Retrofit mediante el
  repositorio compartido; muestra cantidad y duración.
- `postman/`: colección exportada y entorno local.
- `docs/`: base de informe, estructura y evidencias verificadas.

## Ejecutar

Preparar/iniciar la API según [backend/README.md](backend/README.md).
La app usa `BuildConfig.API_BASE_URL`, configurada en `app/build.gradle.kts` como
`http://10.0.2.2:5000/`. Este host permite al emulador acceder al servidor de la Mac.
HTTP solo está permitido hacia ese host en debug; release requiere HTTPS.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -W -n com.melvinpena.comparativa/.MainActivity
```

En la portada, seleccionar Abrir bitácora de campo para CRUD o Comenzar comparación
para el laboratorio. Cámara y reconocimiento de voz requieren aplicaciones
compatibles instaladas en el dispositivo. La descripción también puede editarse.

## Verificación

- Backend: compilación correcta y colección Postman contra API local con 17
  solicitudes y 25 aserciones exitosas. Ver `docs/evidencias/postman-api-local.txt`.
- Android: compilación e instalación en Pixel 4 XL verificadas. El resultado del
  recorrido automatizado CRUD está en `docs/evidencias/android-crud-smoke.txt`.
- Prueba reutilizable de UI: `python3 scripts/smoke_android_crud.py`, con API y
  emulador activos. Crea/edita/elimina solo su registro único; guarda capturas.
  Si falla deja el ID para inspección. No utiliza JWT ni prueba cámara/voz.

## Condiciones del laboratorio

- Mismo GET `/api/registros`; token vacío omite Authorization.
- Volley: DefaultRetryPolicy sin reintentos, caché desactivada y solicitudes con tag.
- OkHttp/Retrofit: sin caché de disco, retryOnConnectionFailure(false).
- Todos envían Cache-Control: no-cache, no-store. No garantiza el comportamiento de proxies.
- Usar URL final sin redirecciones: OkHttp/Retrofit no las siguen; Volley conserva
  el comportamiento predeterminado de su transporte.
- Timeout por socket en Volley y conexión/lectura/escritura en OkHttp/Retrofit.
  No equivale a un límite total de extremo a extremo.
- Duración con reloj monotónico: incluye preparación, red, conversión y entrega en UI.
  OkHttp/Retrofit se construyen por consulta, Volley reutiliza la cola. Declarar
  esta condición al comparar; los resultados describen esta implementación.
- Una consulta a la vez; cancelar al destruir el laboratorio. Logcat:
  ComparativaHTTP registra resultado/clase de error sin URL/JWT/cuerpo.

## Pendientes de entrega

La API incorpora JWT y un login local de laboratorio; ver la sección de acceso.
Faltan la matriz completa de fallos de los tres clientes, cinco mediciones por cliente, dictado real
por micrófono, capturas finales, PDF, videos y revisión del PR.
Las pruebas del backend no sustituyen las pruebas de la interfaz Android.

- [Base del informe individual](docs/BASE_INFORME.md)
- [Desarrollo y estructura](docs/DESARROLLO_Y_ESTRUCTURA.md)
- [Contrato y ejecución de la API](backend/README.md)


La fotografía del emulador y el CRUD fueron comprobados contra la API; la
captura/modo avión cuentan con evidencia en docs/evidencias. Ver los documentos
para distinguir esas pruebas de las pendientes de tarea 2.5.


## Acceso JWT del laboratorio

En Development, la API genera una cuenta local al arrancar. Consultar usuario y
contraseña en `backend/Bitacora.Api/.dev-login.json` (archivo privado, ignorado por
Git). En Android pulsar Iniciar sesión y usar esas credenciales. El CRUD utiliza
la sesión en memoria y el laboratorio precarga el mismo token para los tres clientes.
Al cerrar la app por completo hay que iniciar sesión otra vez. El token dura 15 min.
Se puede borrar el campo JWT del laboratorio para comprobar el error 401.

El login local no es un proveedor de identidad de producción; está disponible
solo en Development. Las claves y contraseñas no se incluyen en los informes.
La verificación conjunta está en `scripts/verify_jwt.py`; reportes de esta fase:
`docs/evidencias/jwt-api.txt`, `jwt-android.txt` y `postman-api-jwt.txt`.


JWT verificado: 15 casos API; Postman actualizado 20 solicitudes/29 aserciones;
los tres clientes Android pasan con JWT y muestran 401 sin él. Esto completa el
caso de token ausente de la matriz 2.5; aún faltan los otros fallos y los promedios.

Para retomar esta noche, consultar la sección **Punto de continuación — pendientes de tarea 2.5** en ambos documentos de docs/.

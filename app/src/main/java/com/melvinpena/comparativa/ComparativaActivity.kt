package com.melvinpena.comparativa

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.Call as OkHttpCall
import okhttp3.Callback as OkHttpCallback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.Response as OkHttpResponse
import com.melvinpena.comparativa.data.remote.EscenarioLaboratorio
import com.melvinpena.comparativa.data.repository.ApiResult
import com.melvinpena.comparativa.data.repository.RegistroRepository
import com.melvinpena.comparativa.data.repository.RequestHandle
import java.io.IOException
import java.util.concurrent.TimeUnit

class ComparativaActivity : Activity() {
    private lateinit var baseUrlInput: EditText
    private lateinit var tokenInput: EditText
    private lateinit var timeoutInput: EditText
    private lateinit var escenarioInput: Spinner
    private lateinit var resultView: TextView
    private lateinit var volleyQueue: com.android.volley.RequestQueue

    private var activeOkHttpCall: OkHttpCall? = null
    private var activeRetrofitCall: RequestHandle? = null
    private var requestInProgress = false

    private fun beginRequest(client: String): Boolean {
        if (requestInProgress) return false
        if (tokenInput.text.any { it.code < 32 || it.code > 126 }) {
            resultView.text = "El token contiene caracteres inválidos. Pégalo en una sola línea."
            return false
        }
        requestInProgress = true
        setSettingsEnabled(false)
        resultView.text = "Consultando con $client…\n${escenarioSeleccionado()}"
        return true
    }

    private fun publishResult(message: String, diagnostic: String) {
        if (isFinishing || isDestroyed) return
        requestInProgress = false
        setSettingsEnabled(true)
        resultView.text = message
        // Do not log URL, headers, response body or JWT.
        Log.i("ComparativaHTTP", "${escenarioSeleccionado().name} | $diagnostic | $message")
    }

    private fun httpClient(timeout: Int) = OkHttpClient.Builder()
        .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiStyle.canvas
        window.navigationBarColor = UiStyle.canvas
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        volleyQueue = Volley.newRequestQueue(applicationContext)
        setContentView(createContent())
    }

    private fun createContent(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStyle.dp(this@ComparativaActivity, 22),
                UiStyle.dp(this@ComparativaActivity, 16),
                UiStyle.dp(this@ComparativaActivity, 22),
                UiStyle.dp(this@ComparativaActivity, 28),
            )
        }

        val topBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val backButton = ImageButton(this).apply {
            contentDescription = "Volver al inicio"
            setImageResource(android.R.drawable.ic_media_previous)
            setColorFilter(UiStyle.ink)
            background = UiStyle.surface(this@ComparativaActivity, 14)
            setOnClickListener { finish() }
        }
        topBar.addView(
            backButton,
            LinearLayout.LayoutParams(
                UiStyle.dp(this, 44),
                UiStyle.dp(this, 44),
            ),
        )
        val topLabel = UiStyle.text(this, "  INICIO   /   LABORATORIO", 11f, UiStyle.muted, true)
        topBar.addView(topLabel)
        content.addView(topBar)
        content.addView(UiStyle.space(this, 22))
        content.addView(UiStyle.text(this, "Compara los clientes HTTP", 25f, UiStyle.ink, true))
        content.addView(UiStyle.space(this, 7))
        content.addView(
            UiStyle.text(
                this,
                "Envía la misma solicitud y compara su respuesta y tiempo.",
                14f,
                UiStyle.muted,
            ),
        )
        content.addView(UiStyle.space(this, 20))

        val settingsCard = UiStyle.card(this)
        settingsCard.addView(UiStyle.text(this, "CONFIGURACIÓN DE LA API", 11f, UiStyle.teal, true))
        settingsCard.addView(UiStyle.space(this, 8))
        settingsCard.addView(label("URL base"))
        baseUrlInput = input("https://servidor/").also { it.setText(BuildConfig.API_BASE_URL); settingsCard.addView(it) }
        settingsCard.addView(label("Token JWT"))
        tokenInput = input("Pega tu token (opcional)").also {
            it.setText(com.melvinpena.comparativa.data.remote.AuthSession.accessToken.orEmpty())
            it.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            settingsCard.addView(it)
        }
        settingsCard.addView(label("Tiempo de espera (ms)"))
        timeoutInput = input("10000").also {
            it.setText("10000")
            it.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            settingsCard.addView(it)
        }
        settingsCard.addView(label("Escenario de la consulta"))
        escenarioInput = Spinner(this).apply {
            val escenarios = if (BuildConfig.DEBUG) EscenarioLaboratorio.entries.toList()
                else listOf(EscenarioLaboratorio.NORMAL)
            adapter = ArrayAdapter(this@ComparativaActivity,
                android.R.layout.simple_spinner_item, escenarios).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        settingsCard.addView(escenarioInput)
        settingsCard.addView(UiStyle.text(this,
            "Para probar timeout, usa una espera menor a 15000 ms. Demora y error 500 requieren la API en Development y un JWT válido.",
            12f, UiStyle.muted))
        content.addView(settingsCard)
        content.addView(UiStyle.space(this, 22))
        content.addView(UiStyle.text(this, "Ejecuta una prueba", 18f, UiStyle.ink, true))
        content.addView(UiStyle.space(this, 4))
        content.addView(UiStyle.text(this, "Los tres clientes consultarán el escenario seleccionado.", 13f, UiStyle.muted))
        content.addView(UiStyle.space(this, 12))
        content.addView(clientCard("V", "Volley", "JsonArrayRequest · cola de solicitudes", UiStyle.teal) {
            requestWithVolley()
        })
        content.addView(UiStyle.space(this, 10))
        content.addView(clientCard("O", "OkHttp", "Request manual · ejecución asíncrona", UiStyle.navy) {
            requestWithOkHttp()
        })
        content.addView(UiStyle.space(this, 10))
        content.addView(clientCard("R", "Retrofit", "Interfaz de servicio · conversión Gson", Color.rgb(183, 126, 30)) {
            requestWithRetrofit()
        })
        content.addView(UiStyle.space(this, 18))
        val resultCard = UiStyle.card(this, 18).apply {
            background = UiStyle.surface(this@ComparativaActivity, 22, Color.rgb(235, 243, 246))
        }
        resultView = UiStyle.text(
            this,
            "Los resultados de la solicitud aparecerán aquí.",
            14f,
            UiStyle.ink,
        ).apply {
            setLineSpacing(UiStyle.dp(this@ComparativaActivity, 3).toFloat(), 1f)
        }
        resultCard.addView(UiStyle.text(this, "RESULTADO", 11f, UiStyle.teal, true))
        resultCard.addView(UiStyle.space(this, 8))
        resultCard.addView(resultView)
        content.addView(resultCard)

        return ScrollView(this).apply {
            setBackgroundColor(UiStyle.canvas)
            isFillViewport = true
            addView(content)
        }
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(UiStyle.muted)
        setPadding(0, UiStyle.dp(this@ComparativaActivity, 12), 0, UiStyle.dp(this@ComparativaActivity, 6))
    }

    private fun input(hintText: String) = EditText(this).apply {
        hint = hintText
        isSingleLine = true
        UiStyle.field(this@ComparativaActivity, this)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun clientCard(
        mark: String,
        name: String,
        description: String,
        accent: Int,
        onClick: () -> Unit,
    ): LinearLayout {
        val card = UiStyle.card(this, 14).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = UiStyle.text(this, mark, 16f, accent, true).apply {
            gravity = Gravity.CENTER
            background = UiStyle.surface(this@ComparativaActivity, 13, UiStyle.canvas)
        }
        card.addView(
            badge,
            LinearLayout.LayoutParams(UiStyle.dp(this, 42), UiStyle.dp(this, 42)),
        )
        val descriptionBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiStyle.dp(this@ComparativaActivity, 12), 0, UiStyle.dp(this@ComparativaActivity, 8), 0)
            addView(UiStyle.text(this@ComparativaActivity, name, 15f, UiStyle.ink, true))
            addView(UiStyle.text(this@ComparativaActivity, description, 11f, UiStyle.muted))
        }
        card.addView(
            descriptionBlock,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        val runButton = UiStyle.button(this, "Probar", UiStyle.navy, Color.WHITE, onClick).apply {
            minHeight = UiStyle.dp(this@ComparativaActivity, 42)
            minimumHeight = UiStyle.dp(this@ComparativaActivity, 42)
            textSize = 13f
            setPadding(UiStyle.dp(this@ComparativaActivity, 12), 0, UiStyle.dp(this@ComparativaActivity, 12), 0)
        }
        card.addView(runButton)
        return card
    }

    private fun requestWithVolley() {
        val url = requestUrl() ?: return
        val token = tokenInput.text.toString().trim()
        val timeout = timeoutMillis() ?: return
        val startedAt = SystemClock.elapsedRealtime()
        if (!beginRequest("Volley")) return

        val request = object : JsonArrayRequest(
            Request.Method.GET,
            url,
            null,
            { records -> showSuccess("Volley", records.length(), startedAt) },
            { error -> showFailure("Volley", error, startedAt) },
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return authorizationHeaders(token)
            }
        }
        request.retryPolicy = DefaultRetryPolicy(
            timeout,
            0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT,
        )
        request.tag = this
        request.setShouldCache(false)
        volleyQueue.add(request)
    }

    private fun requestWithOkHttp() {
        val url = requestUrl() ?: return
        val token = tokenInput.text.toString().trim()
        val timeout = timeoutMillis() ?: return
        val startedAt = SystemClock.elapsedRealtime()
        if (!beginRequest("OkHttp")) return

        val client = httpClient(timeout)
        val request = OkHttpRequest.Builder()
            .url(url)
            .apply { authorizationHeaders(token).forEach { (name, value) -> addHeader(name, value) } }
            .get()
            .build()

        activeOkHttpCall = client.newCall(request)
        activeOkHttpCall!!.enqueue(object : OkHttpCallback {
            override fun onFailure(call: OkHttpCall, e: IOException) {
                runOnUiThread { showFailure("OkHttp", e, startedAt) }
            }

            override fun onResponse(call: OkHttpCall, response: OkHttpResponse) {
                response.use {
                    if (!it.isSuccessful) {
                        runOnUiThread {
                            showHttpFailure("OkHttp", it.code, it.message, startedAt)
                        }
                        return
                    }
                    try {
                        val body = it.body?.string()
                            ?: throw IOException("El servidor devolvió una respuesta vacía.")
                        val registros: List<JsonObject> = com.google.gson.Gson().fromJson(
                            body,
                            object : TypeToken<List<JsonObject>>() {}.type,
                        ) ?: throw IOException("El servidor devolvió una lista vacía o inválida.")
                        runOnUiThread {
                            showSuccess("OkHttp", registros.size, startedAt)
                        }
                    } catch (error: Exception) {
                        runOnUiThread { showFailure("OkHttp", error, startedAt) }
                    }
                }
            }
        })
    }

    private fun requestWithRetrofit() {
        val baseUrl = baseUrl() ?: return
        val token = tokenInput.text.toString().trim()
        val timeout = timeoutMillis() ?: return
        val startedAt = SystemClock.elapsedRealtime()
        if (!beginRequest("Retrofit")) return

        val repository = RegistroRepository(baseUrl, timeout.toLong(), token)
        activeRetrofitCall = repository.consultarLaboratorio(escenarioSeleccionado()) { result ->
            when (result) {
                is ApiResult.Success -> showSuccess("Retrofit", result.value.size, startedAt)
                is ApiResult.Failure -> {
                    if (result.httpCode != null) showHttpFailure("Retrofit", result.httpCode, "", startedAt)
                    else publishResult("Retrofit: ${result.message} (${elapsedMillis(startedAt)} ms).",
                        result.cause?.javaClass?.simpleName ?: "error")
                }
            }
        }
    }

    private fun escenarioSeleccionado() = escenarioInput.selectedItem as EscenarioLaboratorio

    private fun setSettingsEnabled(enabled: Boolean) {
        baseUrlInput.isEnabled = enabled
        tokenInput.isEnabled = enabled
        timeoutInput.isEnabled = enabled
        escenarioInput.isEnabled = enabled
    }

    private fun requestUrl(): String? {
        val baseUrl = baseUrl() ?: return null
        return "$baseUrl${escenarioSeleccionado().ruta}"
    }

    private fun baseUrl(): String? {
        val value = baseUrlInput.text.toString().trim()
        if (value.isBlank()) {
            resultView.text = "Ingresa la URL base de la API."
            return null
        }
        val normalized = if (value.endsWith("/")) value else "$value/"
        val parsed = normalized.toHttpUrlOrNull()
        if (parsed == null || parsed.query != null || parsed.fragment != null ||
            parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) {
            resultView.text = "La URL debe comenzar con http:// o https:// y tener un host válido."
            return null
        }
        return parsed.toString()
    }

    private fun timeoutMillis(): Int? {
        val timeout = timeoutInput.text.toString().toIntOrNull()
        if (timeout == null || timeout <= 0) {
            resultView.text = "El tiempo de espera debe ser un número mayor que cero."
            return null
        }
        return timeout
    }

    private fun authorizationHeaders(token: String): MutableMap<String, String> {
        return mutableMapOf("Cache-Control" to "no-cache, no-store").apply {
            if (token.isNotBlank()) put("Authorization", bearerToken(token))
        }
    }

    private fun bearerToken(token: String) =
        if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"

    private fun showSuccess(client: String, count: Int, startedAt: Long) {
        publishResult("$client: $count registros recibidos en ${elapsedMillis(startedAt)} ms.", "success")
    }

    private fun showHttpFailure(client: String, code: Int, message: String, startedAt: Long) {
        val explanation = when (code) {
            401 -> "Token ausente, inválido o vencido (401)."
            500 -> "Error interno del servidor (500)."
            else -> "El servidor respondió HTTP $code${if (message.isBlank()) "" else ": $message"}."
        }
        publishResult("$client: $explanation (${elapsedMillis(startedAt)} ms).", "HTTP $code")
    }

    private fun showFailure(client: String, error: Throwable, startedAt: Long) {
        val status = (error as? com.android.volley.VolleyError)?.networkResponse?.statusCode
        if (status != null) {
            showHttpFailure(client, status, "", startedAt)
            return
        }
        val causes = generateSequence(error) { it.cause }.take(16).toList()
        val explanation = when {
            causes.any { it is com.android.volley.TimeoutError || it is java.net.SocketTimeoutException ||
                it is java.io.InterruptedIOException } -> "Tiempo de espera agotado."
            causes.any { it is java.net.UnknownHostException } ->
                "No se pudo resolver el servidor. Revisa la conexión y la URL."
            causes.any { it is java.net.ConnectException || it is com.android.volley.NoConnectionError } ->
                "No se pudo conectar con la API. Revisa la red y que el servidor esté activo."
            causes.any { it is javax.net.ssl.SSLException } -> "No se pudo establecer una conexión segura."
            causes.any { it is com.android.volley.ParseError || it is com.google.gson.JsonParseException } ->
                "La API devolvió un JSON inválido o con un formato inesperado."
            else -> "No se pudo completar la solicitud."
        }
        publishResult("$client: $explanation (${elapsedMillis(startedAt)} ms).",
            causes.joinToString(" <- ") { it.javaClass.simpleName })
    }

    private fun elapsedMillis(startedAt: Long) = SystemClock.elapsedRealtime() - startedAt

    override fun onDestroy() {
        activeOkHttpCall?.cancel()
        activeRetrofitCall?.cancel()
        super.onDestroy()
        if (::volleyQueue.isInitialized) {
            volleyQueue.cancelAll(this)
        }
    }
}

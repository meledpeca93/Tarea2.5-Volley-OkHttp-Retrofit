package com.melvinpena.comparativa

import android.R
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.TimeoutError
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.melvinpena.comparativa.data.remote.AuthSession
import com.melvinpena.comparativa.data.remote.EscenarioLaboratorio
import com.melvinpena.comparativa.data.repository.ApiResult
import com.melvinpena.comparativa.data.repository.RegistroRepository
import com.melvinpena.comparativa.data.repository.RequestHandle
import okhttp3.Call as OkHttpCall
import okhttp3.Callback as OkHttpCallback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.Response as OkHttpResponse
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class ComparativaActivity : ComponentActivity() {
    private lateinit var baseUrlInput: EditText
    private lateinit var tokenInput: EditText
    private lateinit var timeoutInput: EditText
    private lateinit var escenarioInput: Spinner
    private lateinit var resultView: TextView
    private lateinit var statusBadge: TextView
    private lateinit var statusContainer: LinearLayout
    private lateinit var metricsRow: LinearLayout
    private lateinit var metricClientChip: TextView
    private lateinit var metricLatencyChip: TextView
    private lateinit var metricStatusChip: TextView
    private lateinit var loadingProgress: ProgressBar
    private lateinit var volleyQueue: RequestQueue

    private var volleyBtn: View? = null
    private var okHttpBtn: View? = null
    private var retrofitBtn: View? = null

    private var activeOkHttpCall: OkHttpCall? = null
    private var activeRetrofitCall: RequestHandle? = null
    private var requestInProgress = false

    private enum class ResultState { IDLE, LOADING, SUCCESS, ERROR }

    private fun updateResultsUI(
        state: ResultState,
        statusText: String,
        client: String = "",
        latencyMs: Long = -1,
        detailStat: String = "",
        message: String = "",
    ) {
        if (isFinishing || isDestroyed) return
        when (state) {
            ResultState.IDLE -> {
                statusBadge.text = "STATUS: LISTO PARA PROBAR"
                statusBadge.setTextColor(UiStyle.muted)
                statusContainer.background = UiStyle.surface(this, 12, UiStyle.canvas)
                loadingProgress.visibility = View.GONE
                metricsRow.visibility = View.GONE
                resultView.text = message.ifEmpty { "Selecciona un cliente arriba para ejecutar la consulta." }
            }
            ResultState.LOADING -> {
                statusBadge.text = "SOLICITANDO CON $client..."
                statusBadge.setTextColor(UiStyle.navy)
                statusContainer.background = UiStyle.surface(this, 12, Color.rgb(224, 242, 254))
                loadingProgress.visibility = View.VISIBLE
                metricsRow.visibility = View.GONE
                resultView.text = "Ejecutando escenario: ${escenarioSeleccionado().etiqueta} (GET /${escenarioSeleccionado().ruta})"
            }
            ResultState.SUCCESS -> {
                statusBadge.text = "✓ SOLICITUD EXITOSA ($client)"
                statusBadge.setTextColor(Color.rgb(22, 101, 52))
                statusContainer.background = UiStyle.surface(this, 12, Color.rgb(220, 252, 231))
                loadingProgress.visibility = View.GONE
                metricsRow.visibility = View.VISIBLE
                metricClientChip.text = "Cliente: $client"
                metricLatencyChip.text = "⏱️ ${if (latencyMs >= 0) "$latencyMs ms" else "N/A"}"
                metricStatusChip.text = "📦 $detailStat"
                resultView.text = message
            }
            ResultState.ERROR -> {
                statusBadge.text = "⚠️ ERROR EN LA SOLICITUD ($client)"
                statusBadge.setTextColor(UiStyle.errorText)
                statusContainer.background = UiStyle.surface(this, 12, UiStyle.errorBg)
                loadingProgress.visibility = View.GONE
                metricsRow.visibility = View.VISIBLE
                metricClientChip.text = "Cliente: $client"
                metricLatencyChip.text = "⏱️ ${if (latencyMs >= 0) "$latencyMs ms" else "N/A"}"
                metricStatusChip.text = "❌ $detailStat"
                resultView.text = message
            }
        }
    }

    private fun beginRequest(client: String): Boolean {
        if (requestInProgress) return false
        if (tokenInput.text.any { it.code < 32 || it.code > 126 }) {
            updateResultsUI(
                ResultState.ERROR,
                "Error en Token",
                client = client,
                detailStat = "Token Inválido",
                message = "El token contiene caracteres inválidos. Pégalo en una sola línea.",
            )
            return false
        }
        requestInProgress = true
        setSettingsEnabled(false)
        updateResultsUI(ResultState.LOADING, "Cargando", client = client)
        return true
    }

    private fun publishResult(client: String, message: String, diagnostic: String, isError: Boolean = false, latencyMs: Long = -1, detailStat: String = "") {
        if (isFinishing || isDestroyed) return
        requestInProgress = false
        setSettingsEnabled(true)
        val state = if (isError) ResultState.ERROR else ResultState.SUCCESS
        updateResultsUI(state, diagnostic, client = client, latencyMs = latencyMs, detailStat = detailStat, message = message)
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
        var sysUiFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) {
            sysUiFlags = sysUiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = sysUiFlags
        volleyQueue = Volley.newRequestQueue(applicationContext)
        setContentView(createContent())
    }

    private fun createContent(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStyle.dp(this@ComparativaActivity, 20),
                UiStyle.dp(this@ComparativaActivity, 16),
                UiStyle.dp(this@ComparativaActivity, 20),
                UiStyle.dp(this@ComparativaActivity, 28),
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val padX = UiStyle.dp(this, 20)
            val padY = UiStyle.dp(this, 16)
            view.setPadding(padX + bars.left, padY + bars.top, padX + bars.right, padY + bars.bottom)
            insets
        }

        // Top bar navigation
        val topBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val backButton = ImageButton(this).apply {
            contentDescription = "Volver al inicio"
            setImageResource(R.drawable.ic_media_previous)
            setColorFilter(UiStyle.ink)
            background = UiStyle.surface(this@ComparativaActivity, 14)
            setOnClickListener { finish() }
        }
        topBar.addView(
            backButton,
            LinearLayout.LayoutParams(
                UiStyle.dp(this, 42),
                UiStyle.dp(this, 42),
            ),
        )
        val topLabel = UiStyle.text(this, "  INICIO   /   LABORATORIO HTTP", 11f, UiStyle.muted, bold = true)
        topBar.addView(topLabel)
        content.addView(topBar)

        content.addView(UiStyle.space(this, 16))
        content.addView(UiStyle.text(this, "Laboratorio de Pruebas HTTP", 25f, UiStyle.ink, bold = true))
        content.addView(UiStyle.space(this, 4))
        content.addView(
            UiStyle.text(
                this,
                "Ejecuta y compara la misma consulta HTTP con Volley, OkHttp y Retrofit.",
                14f,
                UiStyle.muted,
            ),
        )
        content.addView(UiStyle.space(this, 18))

        // API Settings Card
        val settingsCard = UiStyle.card(this, 18)
        settingsCard.addView(UiStyle.text(this, "PARÁMETROS DE LA PRUEBA", 11f, UiStyle.teal, bold = true))
        settingsCard.addView(UiStyle.space(this, 12))

        settingsCard.addView(label("URL Base de la API"))
        baseUrlInput = input("https://servidor/").also {
            it.setText(BuildConfig.API_BASE_URL)
            settingsCard.addView(it)
        }

        settingsCard.addView(label("Token JWT de Autenticación"))
        tokenInput = input("Pega tu token (opcional)").also {
            it.setText(AuthSession.accessToken.orEmpty())
            it.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            settingsCard.addView(it)
        }

        settingsCard.addView(label("Tiempo de espera máximo (ms)"))
        timeoutInput = input("10000").also {
            it.setText("10000")
            it.inputType = InputType.TYPE_CLASS_NUMBER
            settingsCard.addView(it)
        }

        settingsCard.addView(label("Escenario de la consulta"))
        val spinnerContainer = LinearLayout(this).apply {
            background = UiStyle.outline(this@ComparativaActivity, 14)
            setPadding(UiStyle.dp(this@ComparativaActivity, 8), UiStyle.dp(this@ComparativaActivity, 4), UiStyle.dp(this@ComparativaActivity, 8), UiStyle.dp(this@ComparativaActivity, 4))
        }
        escenarioInput = Spinner(this).apply {
            val escenarios = if (BuildConfig.DEBUG) EscenarioLaboratorio.entries.toList()
            else listOf(EscenarioLaboratorio.NORMAL)
            adapter = ArrayAdapter(
                this@ComparativaActivity,
                R.layout.simple_spinner_item,
                escenarios,
            ).apply {
                setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            }
        }
        spinnerContainer.addView(
            escenarioInput,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        settingsCard.addView(spinnerContainer)

        settingsCard.addView(UiStyle.space(this, 10))
        val tipBox = LinearLayout(this).apply {
            background = UiStyle.surface(this@ComparativaActivity, 12, Color.rgb(243, 246, 249))
            setPadding(UiStyle.dp(this@ComparativaActivity, 10), UiStyle.dp(this@ComparativaActivity, 8), UiStyle.dp(this@ComparativaActivity, 10), UiStyle.dp(this@ComparativaActivity, 8))
        }
        tipBox.addView(
            UiStyle.text(
                this,
                "💡 Para probar Timeout usa < 15000 ms. Los escenarios Demora y Error 500 requieren backend en desarrollo.",
                12f,
                UiStyle.muted,
            )
        )
        settingsCard.addView(tipBox)

        content.addView(settingsCard)
        content.addView(UiStyle.space(this, 20))

        // Clients Section
        content.addView(UiStyle.text(this, "SELECCIONA UN CLIENTE PARA PROBAR", 11f, UiStyle.teal, bold = true))
        content.addView(UiStyle.space(this, 10))

        volleyBtn = clientCard("V", "Volley", "JsonArrayRequest · Cola de solicitudes", UiStyle.teal) {
            requestWithVolley()
        }
        content.addView(volleyBtn)

        content.addView(UiStyle.space(this, 10))
        okHttpBtn = clientCard("O", "OkHttp", "Request manual · Ejecución asíncrona", UiStyle.navy) {
            requestWithOkHttp()
        }
        content.addView(okHttpBtn)

        content.addView(UiStyle.space(this, 10))
        retrofitBtn = clientCard("R", "Retrofit", "Interfaz de servicio · Conversor Gson", Color.rgb(183, 126, 30)) {
            requestWithRetrofit()
        }
        content.addView(retrofitBtn)

        content.addView(UiStyle.space(this, 22))

        // Rich Results Dashboard
        content.addView(UiStyle.text(this, "PANEL DE RESULTADOS Y MÉTRICAS", 11f, UiStyle.teal, bold = true))
        content.addView(UiStyle.space(this, 10))

        val resultCard = UiStyle.card(this, 18).apply {
            background = UiStyle.surface(this@ComparativaActivity, 20, Color.WHITE)
        }

        statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiStyle.dp(this@ComparativaActivity, 12), UiStyle.dp(this@ComparativaActivity, 10), UiStyle.dp(this@ComparativaActivity, 12), UiStyle.dp(this@ComparativaActivity, 10))
        }
        statusBadge = UiStyle.text(this, "STATUS: LISTO PARA PROBAR", 12f, UiStyle.muted, bold = true)
        loadingProgress = ProgressBar(this).apply {
            indeterminateDrawable?.setTint(UiStyle.navy)
            visibility = View.GONE
        }
        statusContainer.addView(statusBadge, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        statusContainer.addView(loadingProgress, LinearLayout.LayoutParams(UiStyle.dp(this, 20), UiStyle.dp(this, 20)))

        resultCard.addView(statusContainer)
        resultCard.addView(UiStyle.space(this, 12))

        // Metrics Row (Chips for Client, Latency, Status)
        metricsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }

        metricClientChip = createChip(UiStyle.navy, Color.WHITE)
        metricLatencyChip = createChip(Color.rgb(241, 245, 249), UiStyle.ink)
        metricStatusChip = createChip(Color.rgb(241, 245, 249), UiStyle.ink)

        metricsRow.addView(metricClientChip)
        metricsRow.addView(UiStyle.spaceHorizontal(this, 6))
        metricsRow.addView(metricLatencyChip)
        metricsRow.addView(UiStyle.spaceHorizontal(this, 6))
        metricsRow.addView(metricStatusChip)

        resultCard.addView(metricsRow)
        resultCard.addView(UiStyle.space(this, 12))

        val detailBox = LinearLayout(this).apply {
            background = UiStyle.surface(this@ComparativaActivity, 14, UiStyle.canvas)
            setPadding(UiStyle.dp(this@ComparativaActivity, 14), UiStyle.dp(this@ComparativaActivity, 12), UiStyle.dp(this@ComparativaActivity, 14), UiStyle.dp(this@ComparativaActivity, 12))
        }

        resultView = UiStyle.text(
            this,
            "Selecciona un cliente arriba para ejecutar la consulta.",
            14f,
            UiStyle.ink,
        ).apply {
            setLineSpacing(UiStyle.dp(this@ComparativaActivity, 3).toFloat(), 1f)
        }
        detailBox.addView(resultView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        resultCard.addView(detailBox)
        content.addView(resultCard)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(UiStyle.canvas)
            isFillViewport = true
            addView(content)
        }

        animateEntrance(topBar, settingsCard, volleyBtn!!, okHttpBtn!!, retrofitBtn!!, resultCard)
        return scroll
    }

    private fun createChip(bgColor: Int, textColor: Int): TextView =
        UiStyle.text(this, "", 11f, textColor, bold = true).apply {
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = UiStyle.dp(this@ComparativaActivity, 8).toFloat()
            }
            setPadding(
                UiStyle.dp(this@ComparativaActivity, 10),
                UiStyle.dp(this@ComparativaActivity, 6),
                UiStyle.dp(this@ComparativaActivity, 10),
                UiStyle.dp(this@ComparativaActivity, 6),
            )
        }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(UiStyle.muted)
        setPadding(0, UiStyle.dp(this@ComparativaActivity, 10), 0, UiStyle.dp(this@ComparativaActivity, 4))
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
        val badge = UiStyle.text(this, mark, 16f, accent, bold = true).apply {
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
            addView(UiStyle.text(this@ComparativaActivity, name, 15f, UiStyle.ink, bold = true))
            addView(UiStyle.text(this@ComparativaActivity, description, 11f, UiStyle.muted))
        }
        card.addView(
            descriptionBlock,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        val runButton = UiStyle.button(this, "Probar $name", accent, Color.WHITE, onClick).apply {
            minHeight = UiStyle.dp(this@ComparativaActivity, 40)
            minimumHeight = UiStyle.dp(this@ComparativaActivity, 40)
            textSize = 13f
            setPadding(UiStyle.dp(this@ComparativaActivity, 14), 0, UiStyle.dp(this@ComparativaActivity, 14), 0)
        }
        card.addView(runButton)
        return card
    }

    
    
    //REQUEST CON VOLLEY
    
    private fun requestWithVolley() {
        val url = requestUrl() ?: return
        val token = tokenInput.text.toString().trim()
        val timeout = timeoutMillis() ?: return
        val startedAt = SystemClock.elapsedRealtime()
        if (!beginRequest("Volley")) return

        val request = object : JsonArrayRequest(
            Method.GET,
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
    
    
    
    
    //REQUEST CON OKHTTP

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
                        val registros: List<JsonObject> = Gson().fromJson(
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

    
    
    
    
    //REQUEST CON RETROFIT
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
                    else {
                        val duration = elapsedMillis(startedAt)
                        publishResult("Retrofit", "Retrofit: ${result.message} ($duration ms).",
                            result.cause?.javaClass?.simpleName ?: "error", isError = true, latencyMs = duration, detailStat = "Error")
                    }
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
        volleyBtn?.isEnabled = enabled
        okHttpBtn?.isEnabled = enabled
        retrofitBtn?.isEnabled = enabled
    }

    private fun requestUrl(): String? {
        val baseUrl = baseUrl() ?: return null
        return "$baseUrl${escenarioSeleccionado().ruta}"
    }
    
    
    //RECUPERACION DE URL, TIMEOUT Y TOKEN PARA CADA CLIENTE

    private fun baseUrl(): String? {
        val value = baseUrlInput.text.toString().trim()
        if (value.isBlank()) {
            updateResultsUI(
                ResultState.ERROR,
                "URL Inválida",
                message = "Ingresa la URL base de la API.",
            )
            return null
        }
        val normalized = if (value.endsWith("/")) value else "$value/"
        val parsed = normalized.toHttpUrlOrNull()
        if (parsed == null || parsed.query != null || parsed.fragment != null ||
            parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) {
            updateResultsUI(
                ResultState.ERROR,
                "URL Inválida",
                message = "La URL debe comenzar con http:// o https:// y tener un host válido.",
            )
            return null
        }
        return parsed.toString()
    }

    
    
    private fun timeoutMillis(): Int? {
        val timeout = timeoutInput.text.toString().toIntOrNull()
        if (timeout == null || timeout <= 0) {
            updateResultsUI(
                ResultState.ERROR,
                "Timeout Inválido",
                message = "El tiempo de espera debe ser un número mayor que cero.",
            )
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
        val latency = elapsedMillis(startedAt)
        publishResult(client, "$client: $count registros recibidos exitosamente en $latency ms.", "success", isError = false, latencyMs = latency, detailStat = "$count registros")
    }

    
    
    private fun showHttpFailure(client: String, code: Int, message: String, startedAt: Long) {
        val latency = elapsedMillis(startedAt)
        val explanation = when (code) {
            401 -> "Token ausente, inválido o vencido (401)."
            500 -> "Error interno del servidor (500)."
            else -> "El servidor respondió HTTP $code${if (message.isBlank()) "" else ": $message"}."
        }
        publishResult(client, "$client: $explanation ($latency ms).", "HTTP $code", isError = true, latencyMs = latency, detailStat = "HTTP $code")
    }

    
    
    private fun showFailure(client: String, error: Throwable, startedAt: Long) {
        val latency = elapsedMillis(startedAt)
        val status = (error as? VolleyError)?.networkResponse?.statusCode
        if (status != null) {
            showHttpFailure(client, status, "", startedAt)
            return
        }
        val causes = generateSequence(error) { it.cause }.take(16).toList()
        val explanation = when {
            causes.any { it is TimeoutError || it is SocketTimeoutException ||
                it is InterruptedIOException } -> "Tiempo de espera agotado (Timeout)."
            causes.any { it is UnknownHostException } ->
                "No se pudo resolver el servidor. Revisa la conexión y la URL."
            causes.any { it is ConnectException || it is NoConnectionError } ->
                "No se pudo conectar con la API. Revisa la red y que el servidor esté activo."
            causes.any { it is SSLException } -> "No se pudo establecer una conexión segura."
            causes.any { it is ParseError || it is JsonParseException } ->
                "La API devolvió un JSON inválido o con un formato inesperado."
            else -> "No se pudo completar la solicitud."
        }
        val errStat = if (causes.any { it is SocketTimeoutException || it is TimeoutError }) "Timeout" else "Error Red"
        publishResult(client, "$client: $explanation ($latency ms).",
            causes.joinToString(" <- ") { it.javaClass.simpleName }, isError = true, latencyMs = latency, detailStat = errStat)
    }

    
    
    
    private fun elapsedMillis(startedAt: Long) = SystemClock.elapsedRealtime() - startedAt

    private fun animateEntrance(vararg views: View) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = UiStyle.dp(this, 14).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 90L)
                .setDuration(380L)
                .start()
        }
    }

    override fun onDestroy() {
        activeOkHttpCall?.cancel()
        activeRetrofitCall?.cancel()
        super.onDestroy()
        if (::volleyQueue.isInitialized) {
            volleyQueue.cancelAll(this)
        }
    }
}

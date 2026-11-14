package com.melvinpena.comparativa.data.repository

import com.google.gson.JsonParser
import com.melvinpena.comparativa.BuildConfig
import com.melvinpena.comparativa.data.remote.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun interface RequestHandle { fun cancel() }
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val message: String, val retryable: Boolean, val httpCode: Int? = null,
        val cause: Throwable? = null) : ApiResult<Nothing>
}

/** The only application class that calls the Retrofit service. */


class RegistroRepository(
    baseUrl: String = BuildConfig.API_BASE_URL,
    timeoutMs: Long = ApiClient.DEFAULT_TIMEOUT_MS,
    private val token: String? = null,
) {
    private val api = ApiClient.createRetrofit(baseUrl, timeoutMs).create(RegistroApi::class.java)
    private val authorization: String?
        get() = (token ?: AuthSession.accessToken.orEmpty()).trim().takeIf { it.isNotEmpty() }?.let {
            if (it.startsWith("Bearer ", true)) it else "Bearer $it"
        }  
 fun listar(done: (ApiResult<List<RegistroResumen>>) -> Unit) = execute(api.listar(authorization), done)
    fun consultarLaboratorio(escenario: EscenarioLaboratorio, done: (ApiResult<List<RegistroResumen>>) -> Unit): RequestHandle {
        require(BuildConfig.DEBUG || escenario == EscenarioLaboratorio.NORMAL) {
            "Los escenarios de fallo solo están disponibles en debug."
        }
        return if (escenario == EscenarioLaboratorio.NORMAL) listar(done)
        else execute(api.consultarLaboratorio(escenario.ruta, authorization), done)
    }
    fun consultar(id: Int, done: (ApiResult<RegistroDetalle>) -> Unit) = execute(api.consultar(id, authorization), done)
    fun crear(input: RegistroInput, done: (ApiResult<RegistroDetalle>) -> Unit) = execute(api.crear(input, authorization), done)
    fun actualizar(id: Int, input: RegistroInput, done: (ApiResult<RegistroDetalle>) -> Unit) = execute(api.actualizar(id, input, authorization), done)
    fun eliminar(id: Int, done: (ApiResult<Unit>) -> Unit): RequestHandle =
        execute(api.eliminar(id, authorization), { result ->
            when (result) {
                is ApiResult.Success -> done(ApiResult.Success(Unit))
                is ApiResult.Failure -> done(result)
            }

        }, allowEmpty = true)
        
        
        

    private fun <T> execute(call: Call<T>, done: (ApiResult<T?>) -> Unit, allowEmpty: Boolean): RequestHandle {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (call.isCanceled) return
                if (!response.isSuccessful) {
                    done(httpError(response))
                } else if (response.body() == null && !allowEmpty) {
                    done(ApiResult.Failure("La API devolvió una respuesta vacía.", false))
                } else done(ApiResult.Success(response.body()))
            }
            override fun onFailure(call: Call<T>, t: Throwable) {
                if (!call.isCanceled) done(networkError(t))
            }
        })
        return RequestHandle { call.cancel() }
    }
    
    private fun <T : Any> execute(call: Call<T>, done: (ApiResult<T>) -> Unit): RequestHandle =
        execute(call, { result ->
            when (result) {
                is ApiResult.Success -> result.value?.let { done(ApiResult.Success(it)) }
                is ApiResult.Failure -> done(result)
            }
        }, allowEmpty = false)

        
        
    private fun httpError(response: Response<*>): ApiResult.Failure {
        val message = when (response.code()) {
            400 -> {
                val details = runCatching {
                    response.errorBody()?.use { body ->
                        val obj = JsonParser.parseString(body.string()).asJsonObject
                        obj.getAsJsonObject("errors")?.entrySet()?.joinToString("\n") { (field, errors) ->
                            "$field: " + errors.asJsonArray.joinToString { it.asString }
                        } ?: obj.get("title")?.asString
                    }
                }.getOrNull()
                details ?: "Revisa los datos enviados (400)."
            }
            401 -> "Token ausente, inválido o vencido (401)."
            404 -> "El registro ya no existe (404)."
            in 500..599 -> "Error del servidor (${response.code()}). Intenta más tarde."
            else -> "La API respondió HTTP ${response.code()}."
        }
        return ApiResult.Failure(message, response.code() >= 500, response.code())
    }
    
    
    
    private fun networkError(t: Throwable): ApiResult.Failure {
        val causes = generateSequence(t) { it.cause }.take(16).toList()
        val message = when {
            causes.any { it is java.net.SocketTimeoutException || it is java.io.InterruptedIOException } -> "Tiempo de espera agotado."
            causes.any { it is javax.net.ssl.SSLException } -> "No se pudo establecer una conexión segura."
            causes.any { it is com.google.gson.JsonParseException } -> "La respuesta JSON no tiene el formato esperado."
            causes.any { it is java.io.IOException } -> "Sin conexión con la API. Revisa la red y el servidor."
            else -> "No se pudo completar la solicitud."
        }
        return ApiResult.Failure(message, causes.any { it is java.io.IOException }, cause = t)
    }
}

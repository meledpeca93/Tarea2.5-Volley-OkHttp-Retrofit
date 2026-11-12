package com.melvinpena.comparativa.data.repository

import com.melvinpena.comparativa.data.remote.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthRepository {
    private val api = ApiClient.createRetrofit().create(AuthApi::class.java)
    fun login(username: String, password: String, done: (ApiResult<LoginReply>) -> Unit): RequestHandle {
        val call = api.login(LoginInput(username, password))
        call.enqueue(object : Callback<LoginReply> {
            override fun onResponse(call: Call<LoginReply>, response: Response<LoginReply>) {
                if (call.isCanceled) return
                val body = response.body()
                if (response.isSuccessful && body != null && body.accessToken.isNotBlank()) {
                    AuthSession.open(body.accessToken, body.username)
                    done(ApiResult.Success(body))
                } else {
                    response.errorBody()?.close()
                    val message = when(response.code()) {
                        400 -> "Completa el usuario y la contraseña."
                        401 -> "Usuario o contraseña incorrectos."
                        429 -> "Demasiados intentos. Espera un minuto antes de volver a intentarlo."
                        else -> "No se pudo iniciar sesión (HTTP ${response.code()})."
                    }
                    done(ApiResult.Failure(message, response.code() >= 500, response.code()))
                }
            }
            override fun onFailure(call: Call<LoginReply>, t: Throwable) {
                if (!call.isCanceled) done(ApiResult.Failure("No se pudo conectar con la API. Revisa la red y el servidor.", true))
            }
        })
        return RequestHandle { call.cancel() }
    }
}

package com.melvinpena.comparativa.data.remote

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginInput(val username: String, val password: String)
data class LoginReply(val accessToken: String, val expiresAtUtc: String, val username: String)
interface AuthApi {
    @POST("api/auth/login") fun login(@Body input: LoginInput): Call<LoginReply>
}

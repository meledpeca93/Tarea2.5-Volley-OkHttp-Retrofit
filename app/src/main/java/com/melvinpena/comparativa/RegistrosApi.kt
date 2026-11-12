package com.melvinpena.comparativa

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface RegistrosApi {
    @GET("api/registros")
    fun getRegistros(@Header("Authorization") authorization: String): Call<List<JsonObject>>
}

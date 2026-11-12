package com.melvinpena.comparativa.data.remote

import retrofit2.Call
import retrofit2.http.*

data class RegistroResumen(val id: Int, val titulo: String, val descripcion: String, val fechaCreacion: String)
data class RegistroDetalle(val id: Int, val titulo: String, val descripcion: String,
    val fotoBase64: String?, val fechaCreacion: String)
data class RegistroInput(val titulo: String, val descripcion: String, val fotoBase64: String?)

interface RegistroApi {
    @Headers("Cache-Control: no-cache, no-store")
    @GET("api/registros") fun listar(@Header("Authorization") token: String?): Call<List<RegistroResumen>>
    @Headers("Cache-Control: no-cache, no-store")
    @GET fun consultarLaboratorio(@Url ruta: String, @Header("Authorization") token: String?): Call<List<RegistroResumen>>
    @GET("api/registros/{id}") fun consultar(@Path("id") id: Int, @Header("Authorization") token: String?): Call<RegistroDetalle>
    @POST("api/registros") fun crear(@Body input: RegistroInput, @Header("Authorization") token: String?): Call<RegistroDetalle>
    @PUT("api/registros/{id}") fun actualizar(@Path("id") id: Int, @Body input: RegistroInput,
        @Header("Authorization") token: String?): Call<RegistroDetalle>
    @DELETE("api/registros/{id}") fun eliminar(@Path("id") id: Int, @Header("Authorization") token: String?): Call<Void>
}

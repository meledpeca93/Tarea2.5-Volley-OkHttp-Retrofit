package com.melvinpena.comparativa.data.remote

import com.melvinpena.comparativa.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Shared transport configuration. No Activity should construct Retrofit. */
object ApiClient {
    const val DEFAULT_TIMEOUT_MS = 10_000L

    fun createRetrofit(
        baseUrl: String = BuildConfig.API_BASE_URL,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): Retrofit {
        require(timeoutMs > 0) { "El timeout debe ser mayor que cero." }
        val client = OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .apply {
                if (BuildConfig.DEBUG) {
                    // BASIC excludes bodies, photographs and descriptions. Redact auth
                    // as a safeguard if the logging level is changed during debugging.
                    val logger = HttpLoggingInterceptor().apply {
                        redactHeader("Authorization")
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                    addInterceptor(logger)
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

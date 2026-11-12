package com.melvinpena.comparativa.viewmodel

/** States to be exposed by the CRUD ViewModels through LiveData. */
sealed interface ScreenState<out T> {
    data object Idle : ScreenState<Nothing>
    data object Loading : ScreenState<Nothing>
    data object Empty : ScreenState<Nothing>
    data class Success<T>(val data: T) : ScreenState<T>
    data class Error(
        val message: String,
        val retryable: Boolean,
        val httpCode: Int? = null,
    ) : ScreenState<Nothing>
}

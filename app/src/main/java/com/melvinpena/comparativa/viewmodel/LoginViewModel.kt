package com.melvinpena.comparativa.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.melvinpena.comparativa.data.remote.LoginReply
import com.melvinpena.comparativa.data.repository.*

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()
    private val mutable = MutableLiveData<ScreenState<LoginReply>>(ScreenState.Idle)
    val state: LiveData<ScreenState<LoginReply>> = mutable
    private var request: RequestHandle? = null
    fun login(username: String, password: String) {
        if (mutable.value is ScreenState.Loading) return
        if (username.isBlank() || password.isBlank() || username.length > 100 || password.length > 128) {
            mutable.value = ScreenState.Error("Completa el usuario y la contraseña (máximo 100 y 128 caracteres).", false)
            return
        }
        mutable.value = ScreenState.Loading
        request = repository.login(username.trim(), password) {
            mutable.value = when (it) {
                is ApiResult.Success -> ScreenState.Success(it.value)
                is ApiResult.Failure -> ScreenState.Error(it.message, it.retryable, it.httpCode)
            }
        }
    }
    override fun onCleared() { request?.cancel() }
}

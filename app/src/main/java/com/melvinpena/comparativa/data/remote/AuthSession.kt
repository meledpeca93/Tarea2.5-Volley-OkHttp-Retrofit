package com.melvinpena.comparativa.data.remote

/** Session deliberately kept in memory; re-authenticate after process death. */
object AuthSession {
    @Volatile var accessToken: String? = null; private set
    @Volatile var username: String? = null; private set
    fun open(token: String, user: String) { accessToken = token; username = user }
    fun clear() { accessToken = null; username = null }
}

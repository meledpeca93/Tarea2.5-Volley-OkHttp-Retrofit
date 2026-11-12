package com.melvinpena.comparativa.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.melvinpena.comparativa.data.remote.*
import com.melvinpena.comparativa.data.repository.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

enum class Pantalla { LISTADO, DETALLE, FORMULARIO }
data class BitacoraContent(val pantalla: Pantalla, val registros: List<RegistroResumen>, val detalle: RegistroDetalle?)

class BitacoraViewModel : ViewModel() {
    private val repository = RegistroRepository()
    private val mutableState = MutableLiveData<ScreenState<BitacoraContent>>(ScreenState.Idle)
    val state: LiveData<ScreenState<BitacoraContent>> = mutableState
    var pantalla = Pantalla.LISTADO; private set
    var registros = emptyList<RegistroResumen>(); private set
    var detalle: RegistroDetalle? = null; private set
    var titulo = ""
    var descripcion = ""
    var fotoBase64: String? = null; private set
    var fotoPreview: Bitmap? = null; private set
    var pendingPhotoPath: String? = null
    private var editId: Int? = null
    private var active: RequestHandle? = null
    private var retry: (() -> Unit)? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var cleared = false
    val busy get() = mutableState.value is ScreenState.Loading

    private fun content() = BitacoraContent(pantalla, registros, detalle)
    private fun show() { mutableState.value = if (pantalla == Pantalla.LISTADO && registros.isEmpty()) ScreenState.Empty else ScreenState.Success(content()) }
    private fun <T> handle(result: ApiResult<T>, success: (T) -> Unit) {
        when (result) {
            is ApiResult.Success -> success(result.value)
            is ApiResult.Failure -> mutableState.value = ScreenState.Error(result.message, result.retryable, result.httpCode)
        }
    }
    fun listar() {
        if (busy) return
        pantalla = Pantalla.LISTADO
        retry = ::listar
        mutableState.value = ScreenState.Loading
        active = repository.listar { handle(it) { rows -> registros = rows; show() } }
    }
    fun consultar(id: Int) {
        if (busy) return
        pantalla = Pantalla.DETALLE
        detalle = null
        fotoPreview = null
        retry = { consultar(id) }
        mutableState.value = ScreenState.Loading
        active = repository.consultar(id) { handle(it) { record ->
            detalle = record
            decodePhoto(record.fotoBase64) { show() }
        } }
    }
    fun nuevo() {
        if (busy) return
        editId = null; titulo = ""; descripcion = ""; fotoBase64 = null; fotoPreview = null
        pantalla = Pantalla.FORMULARIO; show()
    }
    fun editar() {
        if (busy) return
        val record = detalle ?: return
        editId = record.id; titulo = record.titulo; descripcion = record.descripcion; fotoBase64 = record.fotoBase64
        pantalla = Pantalla.FORMULARIO; show()
    }
    fun guardar() {
        if (busy) return
        val error = when {
            titulo.trim().length !in 3..100 || titulo.length > 100 -> "El título debe tener de 3 a 100 caracteres."
            descripcion.isBlank() || descripcion.length > 500 -> "La descripción es obligatoria y admite hasta 500 caracteres."
            else -> null
        }
        if (error != null) { mutableState.value = ScreenState.Error(error, false); return }
        mutableState.value = ScreenState.Loading
        // Retrying a write after timeout may duplicate a server-side creation.
        retry = null
        val input = RegistroInput(titulo.trim(), descripcion.trim(), fotoBase64)
        val done: (ApiResult<RegistroDetalle>) -> Unit = { result -> handle(result) { record ->
            detalle = record; pantalla = Pantalla.DETALLE; show()
        } }
        active = editId?.let { repository.actualizar(it, input, done) } ?: repository.crear(input, done)
    }
    fun eliminar() {
        if (busy) return
        val id = detalle?.id ?: return
        retry = null
        mutableState.value = ScreenState.Loading
        active = repository.eliminar(id) { result -> handle(result) {
            mutableState.value = ScreenState.Idle; detalle = null; listar()
        } }
    }
    fun reintentar() { if (!busy) retry?.invoke() }
    val canRetry get() = retry != null
    fun quitarFoto() { if (!busy) { fotoBase64 = null; fotoPreview = null; show() } }
    fun voiceResult(text: String) { if (!busy) { descripcion = text; show() } }
    fun photoResult(file: File) {
        if (busy) return
        retry = null
        mutableState.value = ScreenState.Loading
        executor.execute {
            val processed = runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, options)
                require(options.outWidth > 0 && options.outHeight > 0) { "La cámara no devolvió una imagen válida." }
                var sample = 1
                while (maxOf(options.outWidth, options.outHeight) / sample > 2048) sample *= 2
                val original = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?: error("No se pudo abrir la fotografía.")
                val ratio = minOf(1f, 1024f / maxOf(original.width, original.height))
                val reduced = Bitmap.createScaledBitmap(original, (original.width * ratio).toInt().coerceAtLeast(1),
                    (original.height * ratio).toInt().coerceAtLeast(1), true)
                val bytes = ByteArrayOutputStream().use { out -> reduced.compress(Bitmap.CompressFormat.JPEG, 70, out); out.toByteArray() }
                if (original !== reduced) original.recycle()
                require(bytes.size <= 2 * 1024 * 1024) { "La fotografía supera 2 MiB después de reducirla." }
                Base64.encodeToString(bytes, Base64.NO_WRAP) to reduced
            }
            file.delete()
            main.post {
                if (!cleared) processed.fold({ (encoded, bitmap) -> fotoBase64 = encoded; fotoPreview = bitmap; show() },
                    { mutableState.value = ScreenState.Error(it.message ?: "No se pudo procesar la foto.", false) })
            }
        }
    }
    private fun decodePhoto(encoded: String?, done: () -> Unit) {
        executor.execute {
            val bitmap = runCatching {
                if (encoded.isNullOrBlank()) null else {
                    require(encoded.length <= 2_796_204)
                    val bytes = Base64.decode(encoded, Base64.DEFAULT)
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    var sample = 1
                    while (maxOf(options.outWidth, options.outHeight) / sample > 1024) sample *= 2
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }.getOrNull()
            main.post { if (!cleared) { fotoPreview = bitmap; done() } }
        }
    }
    override fun onCleared() {
        cleared = true; active?.cancel(); executor.shutdownNow()
        pendingPhotoPath?.let { File(it).delete() }
    }
}

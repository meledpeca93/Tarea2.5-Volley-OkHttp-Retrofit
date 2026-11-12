package com.melvinpena.comparativa.ui

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.melvinpena.comparativa.BuildConfig
import com.melvinpena.comparativa.UiStyle
import com.melvinpena.comparativa.data.remote.RegistroResumen
import com.melvinpena.comparativa.viewmodel.*
import java.io.File

class BitacoraActivity : ComponentActivity() {
    private val model: BitacoraViewModel by viewModels()
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = model.pendingPhotoPath?.let(::File)
        model.pendingPhotoPath = null
        if (success && file != null) model.photoResult(file) else file?.delete()
    }
    private val voice = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(model::voiceResult)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.state.observe(this) { render(it) }
        if (model.state.value is ScreenState.Idle) model.listar()
    }
    private fun render(state: ScreenState<BitacoraContent>) {
        val loading = state is ScreenState.Loading
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(UiStyle.canvas)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(24 + bars.left, 24 + bars.top, 24 + bars.right, 24 + bars.bottom)
            insets
        }
        root.addView(UiStyle.text(this, "Bitácora de campo", 25f, UiStyle.ink, true))
        root.addView(UiStyle.text(this, when(model.pantalla) {
            Pantalla.LISTADO -> "Registros del servidor"
            Pantalla.DETALLE -> "Detalle del registro"
            Pantalla.FORMULARIO -> "Título, fotografía y descripción por voz"
        }, 14f, UiStyle.muted))
        val progress = ProgressBar(this).apply { visibility = if (loading) View.VISIBLE else View.GONE }
        root.addView(progress)
        if (state is ScreenState.Error) {
            root.addView(UiStyle.text(this, state.message, 15f, Color.rgb(155, 35, 35)))
            if (state.retryable && model.canRetry) root.addView(button("Reintentar", model::reintentar))
            if (state.retryable && !model.canRetry) {
                root.addView(UiStyle.text(this,
                    "La operación pudo haberse completado. Revisa el listado antes de volver a enviarla.", 14f, UiStyle.muted))
                root.addView(button("Reintentar") {
                    AlertDialog.Builder(this).setTitle("Revisar antes de repetir")
                        .setMessage("La operación pudo completarse aunque no recibimos la respuesta. Puedes revisar el listado o volver a enviarla.")
                        .setPositiveButton("Revisar listado") { _, _ -> model.listar() }
                        .setNegativeButton("Cancelar", null)
                        .setNeutralButton("Enviar otra vez") { _, _ ->
                            if (model.pantalla == Pantalla.FORMULARIO) model.guardar() else model.eliminar()
                        }.show()
                })
            }
        }
        when (model.pantalla) {
            Pantalla.LISTADO -> {
                root.addView(button("Actualizar", model::listar))
                root.addView(button("Nuevo registro", model::nuevo))
                if (state is ScreenState.Empty) root.addView(UiStyle.text(this, "No hay registros. Crea el primero.", 16f, UiStyle.ink))
                val list = RecyclerView(this).apply {
                    layoutManager = LinearLayoutManager(this@BitacoraActivity)
                    adapter = RegistroAdapter(model.registros) { if (!model.busy) model.consultar(it) }
                }
                root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
                root.addView(button("Volver al inicio") { finish() })
            }
            Pantalla.DETALLE -> {
                val detail = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                model.detalle?.let { record ->
                    detail.addView(UiStyle.text(this, record.titulo, 22f, UiStyle.ink, true))
                    detail.addView(UiStyle.text(this, "Fecha: ${record.fechaCreacion}", 14f, UiStyle.muted))
                    detail.addView(UiStyle.text(this, record.descripcion, 17f, UiStyle.ink))
                    addPhoto(detail)
                    detail.addView(button("Editar", model::editar))
                    detail.addView(button("Eliminar") {
                        AlertDialog.Builder(this).setTitle("Eliminar registro")
                            .setMessage("¿Eliminar este registro del servidor?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Eliminar") { _, _ -> model.eliminar() }.show()
                    })
                }
                detail.addView(button("Volver al listado", model::listar))
                root.addView(ScrollView(this).apply { addView(detail) }, LinearLayout.LayoutParams(-1, 0, 1f))
            }
            Pantalla.FORMULARIO -> {
                val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                form.addView(UiStyle.text(this, "Título (3–100 caracteres)", 14f, UiStyle.ink))
                form.addView(field(model.titulo, "Título", true) { model.titulo = it })
                form.addView(UiStyle.text(this, "Descripción (obligatoria, máximo 500)", 14f, UiStyle.ink))
                form.addView(field(model.descripcion, "Descripción", false) { model.descripcion = it })
                form.addView(button("Dictar descripción") {
                    try {
                        voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-HN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe tu observación")
                        })
                    } catch (_: ActivityNotFoundException) { notice("No hay un reconocedor de voz instalado.") }
                      catch (_: Exception) { notice("No se pudo iniciar el reconocimiento de voz.") }
                })
                form.addView(button("Tomar fotografía", ::takePhoto))
                addPhoto(form)
                form.addView(button("Quitar fotografía", model::quitarFoto))
                form.addView(button("Guardar en el servidor", model::guardar))
                form.addView(button("Cancelar y volver al listado", model::listar))
                root.addView(ScrollView(this).apply { addView(form) }, LinearLayout.LayoutParams(-1, 0, 1f))
            }
        }
        fun disable(view: View) {
            if (view is Button || view is EditText) view.isEnabled = !loading
            if (view is ViewGroup) for (index in 0 until view.childCount) disable(view.getChildAt(index))
        }
        disable(root)
        setContentView(root)
    }
    private fun button(label: String, action: () -> Unit) = UiStyle.button(this, label, UiStyle.navy, Color.WHITE, action)
    private fun field(value: String, hintText: String, single: Boolean, changed: (String) -> Unit) = EditText(this).apply {
        hint = hintText; setText(value); isSingleLine = single
        if (!single) minLines = 3
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun addPhoto(parent: LinearLayout) {
        model.fotoPreview?.let { bitmap ->
            parent.addView(ImageView(this).apply {
                contentDescription = "Fotografía del registro"
                setImageBitmap(bitmap); adjustViewBounds = true; maxHeight = UiStyle.dp(this@BitacoraActivity, 300)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(-1, -2))
        } ?: parent.addView(UiStyle.text(this, "Sin fotografía disponible", 14f, UiStyle.muted))
    }
    private fun takePhoto() {
        try {
            val directory = File(cacheDir, "camera").apply { mkdirs() }
            val file = File.createTempFile("registro_", ".jpg", directory)
            model.pendingPhotoPath = file.path
            camera.launch(FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.files", file))
        } catch (_: ActivityNotFoundException) { cleanupPhoto(); notice("No hay una aplicación de cámara instalada.") }
          catch (_: Exception) { cleanupPhoto(); notice("No se pudo abrir la cámara.") }
    }
    private fun cleanupPhoto() { model.pendingPhotoPath?.let { File(it).delete() }; model.pendingPhotoPath = null }
    private fun notice(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

private class RegistroAdapter(private val rows: List<RegistroResumen>, private val open: (Int) -> Unit) :
    RecyclerView.Adapter<RegistroAdapter.Holder>() {
    class Holder(val label: TextView) : RecyclerView.ViewHolder(label)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(TextView(parent.context).apply {
        textSize = 17f; setPadding(16, 28, 16, 28); setTextColor(UiStyle.ink)
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun getItemCount() = rows.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.label.text = "${row.titulo}\n${row.descripcion}\n${row.fechaCreacion}"
        holder.label.setOnClickListener { open(row.id) }
    }
}

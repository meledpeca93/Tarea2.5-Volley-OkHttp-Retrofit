package com.melvinpena.comparativa.ui

import android.R
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
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
        window.statusBarColor = UiStyle.canvas
        window.navigationBarColor = UiStyle.canvas
        var sysUiFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) {
            sysUiFlags = sysUiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = sysUiFlags

        model.state.observe(this) { render(it) }
        if (model.state.value is ScreenState.Idle) model.listar()
    }

    private fun render(state: ScreenState<BitacoraContent>) {
        val loading = state is ScreenState.Loading
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStyle.dp(this@BitacoraActivity, 20),
                UiStyle.dp(this@BitacoraActivity, 16),
                UiStyle.dp(this@BitacoraActivity, 20),
                UiStyle.dp(this@BitacoraActivity, 24),
            )
            setBackgroundColor(UiStyle.canvas)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val padX = UiStyle.dp(this, 20)
            val padY = UiStyle.dp(this, 16)
            view.setPadding(padX + bars.left, padY + bars.top, padX + bars.right, padY + bars.bottom)
            insets
        }

        // Top Navigation Header
        val topBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val backButton = ImageButton(this).apply {
            contentDescription = "Volver"
            setImageResource(R.drawable.ic_media_previous)
            setColorFilter(UiStyle.ink)
            background = UiStyle.surface(this@BitacoraActivity, 14)
            setOnClickListener {
                if (model.pantalla != Pantalla.LISTADO) {
                    model.listar()
                } else {
                    finish()
                }
            }
        }
        topBar.addView(
            backButton,
            LinearLayout.LayoutParams(UiStyle.dp(this, 42), UiStyle.dp(this, 42)),
        )
        val topLabel = UiStyle.text(this, "  INICIO   /   BITÁCORA DE CAMPO", 11f, UiStyle.muted, bold = true)
        topBar.addView(topLabel)
        root.addView(topBar)

        root.addView(UiStyle.space(this, 16))
        root.addView(UiStyle.text(this, "Bitácora de Campo", 25f, UiStyle.ink, bold = true))
        root.addView(UiStyle.space(this, 4))
        root.addView(UiStyle.text(this, when (model.pantalla) {
            Pantalla.LISTADO -> "Registros sincrónicos almacenados en el servidor"
            Pantalla.DETALLE -> "Detalle completo de la observación seleccionada"
            Pantalla.FORMULARIO -> "Registro con título, fotografía y dictado de voz"
        }, 14f, UiStyle.muted))
        root.addView(UiStyle.space(this, 16))

        // Loading Bar
        val loadingBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = if (loading) View.VISIBLE else View.GONE
            setPadding(0, UiStyle.dp(this@BitacoraActivity, 8), 0, UiStyle.dp(this@BitacoraActivity, 8))
        }
        val spinner = ProgressBar(this).apply {
            indeterminateDrawable?.setTint(UiStyle.navy)
        }
        val loadingText = UiStyle.text(this, "Procesando información...", 13f, UiStyle.muted, bold = true).apply {
            setPadding(UiStyle.dp(this@BitacoraActivity, 10), 0, 0, 0)
        }
        loadingBar.addView(spinner, LinearLayout.LayoutParams(UiStyle.dp(this, 20), UiStyle.dp(this, 20)))
        loadingBar.addView(loadingText)
        root.addView(loadingBar)

        // Error Banner
        if (state is ScreenState.Error) {
            val alert = UiStyle.alertCard(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            alert.addView(UiStyle.text(this, state.message, 14f, UiStyle.errorText, bold = true))
            if (state.retryable && model.canRetry) {
                alert.addView(UiStyle.space(this, 8))
                alert.addView(UiStyle.button(this, "Reintentar", UiStyle.navy, Color.WHITE, model::reintentar))
            }
            if (state.retryable && !model.canRetry) {
                alert.addView(UiStyle.space(this, 6))
                alert.addView(UiStyle.text(this, "La operación pudo haberse completado. Revisa el listado antes de volver a enviarla.", 12f, UiStyle.muted))
                alert.addView(UiStyle.space(this, 8))
                alert.addView(UiStyle.outlinedButton(this, "Revisar / Reintentar", UiStyle.navy) {
                    AlertDialog.Builder(this)
                        .setTitle("Revisar antes de repetir")
                        .setMessage("La operación pudo completarse aunque no recibimos la respuesta. Puedes revisar el listado o volver a enviarla.")
                        .setPositiveButton("Revisar listado") { _, _ -> model.listar() }
                        .setNegativeButton("Cancelar", null)
                        .setNeutralButton("Enviar otra vez") { _, _ ->
                            if (model.pantalla == Pantalla.FORMULARIO) model.guardar() else model.eliminar()
                        }.show()
                })
            }
            root.addView(alert)
            root.addView(UiStyle.space(this, 12))
        }

        when (model.pantalla) {
            Pantalla.LISTADO -> {
                val actionsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val newBtn = UiStyle.button(this, "+ Nuevo registro", UiStyle.navy, Color.WHITE, model::nuevo).apply {
                    minHeight = UiStyle.dp(this@BitacoraActivity, 44)
                    minimumHeight = UiStyle.dp(this@BitacoraActivity, 44)
                    textSize = 13f
                    setPadding(UiStyle.dp(this@BitacoraActivity, 16), 0, UiStyle.dp(this@BitacoraActivity, 16), 0)
                }
                val refreshBtn = UiStyle.outlinedButton(this, "🔄 Actualizar", UiStyle.navy, model::listar).apply {
                    minHeight = UiStyle.dp(this@BitacoraActivity, 44)
                    minimumHeight = UiStyle.dp(this@BitacoraActivity, 44)
                    textSize = 13f
                    setPadding(UiStyle.dp(this@BitacoraActivity, 14), 0, UiStyle.dp(this@BitacoraActivity, 14), 0)
                }
                actionsRow.addView(newBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                actionsRow.addView(UiStyle.spaceHorizontal(this, 10))
                actionsRow.addView(refreshBtn)

                root.addView(actionsRow)
                root.addView(UiStyle.space(this, 14))

                if (state is ScreenState.Empty || model.registros.isEmpty()) {
                    val emptyCard = UiStyle.card(this, 20).apply {
                        gravity = Gravity.CENTER
                    }
                    emptyCard.addView(UiStyle.text(this, "📋", 32f, UiStyle.muted))
                    emptyCard.addView(UiStyle.space(this, 8))
                    emptyCard.addView(UiStyle.text(this, "No hay registros en la bitácora", 16f, UiStyle.ink, bold = true))
                    emptyCard.addView(UiStyle.space(this, 4))
                    emptyCard.addView(UiStyle.text(this, "Toca '+ Nuevo registro' para añadir la primera entrada.", 13f, UiStyle.muted))
                    root.addView(emptyCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                } else {
                    val list = RecyclerView(this).apply {
                        layoutManager = LinearLayoutManager(this@BitacoraActivity)
                        adapter = RegistroAdapter(model.registros) { if (!model.busy) model.consultar(it) }
                    }
                    root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                }
            }
            Pantalla.DETALLE -> {
                val detailCard = UiStyle.card(this, 20)
                model.detalle?.let { record ->
                    detailCard.addView(UiStyle.text(this, "DETALLE DE REGISTRO #${record.id}", 11f, UiStyle.teal, bold = true))
                    detailCard.addView(UiStyle.space(this, 10))
                    detailCard.addView(UiStyle.text(this, record.titulo, 22f, UiStyle.ink, bold = true))
                    detailCard.addView(UiStyle.space(this, 4))
                    detailCard.addView(UiStyle.text(this, "📅 Creado: ${record.fechaCreacion}", 13f, UiStyle.muted))
                    detailCard.addView(UiStyle.space(this, 14))
                    detailCard.addView(UiStyle.text(this, record.descripcion, 15f, UiStyle.ink).apply {
                        setLineSpacing(UiStyle.dp(this@BitacoraActivity, 3).toFloat(), 1f)
                    })
                    detailCard.addView(UiStyle.space(this, 16))
                    addPhoto(detailCard)
                    detailCard.addView(UiStyle.space(this, 18))

                    val btnRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }
                    val editBtn = UiStyle.button(this, "✏️ Editar", UiStyle.navy, Color.WHITE, model::editar).apply {
                        minHeight = UiStyle.dp(this@BitacoraActivity, 44)
                        minimumHeight = UiStyle.dp(this@BitacoraActivity, 44)
                        textSize = 13f
                    }
                    val deleteBtn = UiStyle.outlinedButton(this, "🗑️ Eliminar", UiStyle.errorText) {
                        AlertDialog.Builder(this).setTitle("Eliminar registro")
                            .setMessage("¿Eliminar este registro del servidor?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Eliminar") { _, _ -> model.eliminar() }.show()
                    }.apply {
                        minHeight = UiStyle.dp(this@BitacoraActivity, 44)
                        minimumHeight = UiStyle.dp(this@BitacoraActivity, 44)
                        textSize = 13f
                    }
                    btnRow.addView(editBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    btnRow.addView(UiStyle.spaceHorizontal(this, 10))
                    btnRow.addView(deleteBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

                    detailCard.addView(btnRow)
                }
                detailCard.addView(UiStyle.space(this, 10))
                val backBtn = UiStyle.outlinedButton(this, "← Volver al listado", UiStyle.navy, model::listar)
                detailCard.addView(backBtn)

                val scroll = ScrollView(this).apply {
                    setBackgroundColor(UiStyle.canvas)
                    isFillViewport = true
                    addView(detailCard)
                }
                root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            }
            Pantalla.FORMULARIO -> {
                val formCard = UiStyle.card(this, 20)
                formCard.addView(UiStyle.text(this, "INFORMACIÓN DE LA OBSERVACIÓN", 11f, UiStyle.teal, bold = true))
                formCard.addView(UiStyle.space(this, 14))

                formCard.addView(UiStyle.text(this, "Título (3–100 caracteres)", 14f, UiStyle.ink, bold = true))
                formCard.addView(UiStyle.space(this, 6))
                formCard.addView(field(model.titulo, "Ingresa un título descriptivo", true) { model.titulo = it })

                formCard.addView(UiStyle.space(this, 14))
                formCard.addView(UiStyle.text(this, "Descripción (obligatoria, máximo 500)", 14f, UiStyle.ink, bold = true))
                formCard.addView(UiStyle.space(this, 6))
                formCard.addView(field(model.descripcion, "Describe los hallazgos u observaciones...", false) { model.descripcion = it })

                formCard.addView(UiStyle.space(this, 10))
                val voiceBtn = UiStyle.outlinedButton(this, "🎙️ Dictar por voz", UiStyle.navy) {
                    try {
                        voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-HN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe tu observación")
                        })
                    } catch (_: ActivityNotFoundException) { notice("No hay un reconocedor de voz instalado.") }
                      catch (_: Exception) { notice("No se pudo iniciar el reconocimiento de voz.") }
                }.apply {
                    minHeight = UiStyle.dp(this@BitacoraActivity, 42)
                    minimumHeight = UiStyle.dp(this@BitacoraActivity, 42)
                    textSize = 13f
                }
                formCard.addView(voiceBtn)

                formCard.addView(UiStyle.space(this, 16))
                formCard.addView(UiStyle.text(this, "Evidencia fotográfica", 14f, UiStyle.ink, bold = true))
                formCard.addView(UiStyle.space(this, 6))

                val photoBtn = UiStyle.outlinedButton(this, "📷 Tomar fotografía", UiStyle.navy, ::takePhoto).apply {
                    minHeight = UiStyle.dp(this@BitacoraActivity, 42)
                    minimumHeight = UiStyle.dp(this@BitacoraActivity, 42)
                    textSize = 13f
                }
                formCard.addView(photoBtn)
                formCard.addView(UiStyle.space(this, 10))
                addPhoto(formCard)

                if (model.fotoPreview != null) {
                    formCard.addView(UiStyle.space(this, 8))
                    val removePhotoBtn = UiStyle.outlinedButton(this, "🗑️ Quitar fotografía", UiStyle.errorText, model::quitarFoto).apply {
                        minHeight = UiStyle.dp(this@BitacoraActivity, 38)
                        minimumHeight = UiStyle.dp(this@BitacoraActivity, 38)
                        textSize = 12f
                    }
                    formCard.addView(removePhotoBtn)
                }

                formCard.addView(UiStyle.space(this, 20))
                val saveBtn = UiStyle.button(this, "💾 Guardar en el servidor", UiStyle.navy, Color.WHITE, model::guardar)
                formCard.addView(saveBtn)

                formCard.addView(UiStyle.space(this, 10))
                val cancelBtn = UiStyle.outlinedButton(this, "Cancelar y volver", UiStyle.navy, model::listar)
                formCard.addView(cancelBtn)

                val scroll = ScrollView(this).apply {
                    setBackgroundColor(UiStyle.canvas)
                    isFillViewport = true
                    addView(formCard)
                }
                root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        }

        fun disable(view: View) {
            if (view is Button || view is EditText) view.isEnabled = !loading
            if (view is ViewGroup) for (index in 0 until view.childCount) disable(view.getChildAt(index))
        }
        disable(root)
        setContentView(root)
    }

    private fun field(value: String, hintText: String, single: Boolean, changed: (String) -> Unit) = EditText(this).apply {
        hint = hintText
        setText(value)
        isSingleLine = single
        if (!single) {
            minLines = 3
            gravity = Gravity.TOP
        }
        UiStyle.field(this@BitacoraActivity, this)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun addPhoto(parent: LinearLayout) {
        model.fotoPreview?.let { bitmap ->
            val photoCard = UiStyle.card(this, 8).apply {
                gravity = Gravity.CENTER
                background = UiStyle.surface(this@BitacoraActivity, 14)
            }
            val imageView = ImageView(this).apply {
                contentDescription = "Fotografía del registro"
                setImageBitmap(bitmap)
                adjustViewBounds = true
                maxHeight = UiStyle.dp(this@BitacoraActivity, 260)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            photoCard.addView(imageView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            parent.addView(photoCard)
        } ?: run {
            val emptyPhotoBox = UiStyle.card(this, 12).apply {
                gravity = Gravity.CENTER
                background = UiStyle.surface(this@BitacoraActivity, 12, Color.rgb(243, 246, 249))
            }
            emptyPhotoBox.addView(UiStyle.text(this, "📷 Sin fotografía adjunta", 13f, UiStyle.muted))
            parent.addView(emptyPhotoBox)
        }
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

    class Holder(val container: LinearLayout, val title: TextView, val desc: TextView, val date: TextView) :
        RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        val container = UiStyle.card(context, 14).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = UiStyle.dp(context, 10)
            }
        }

        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleText = UiStyle.text(context, "", 15f, UiStyle.ink, bold = true)
        val arrowText = UiStyle.text(context, "→", 16f, UiStyle.muted, bold = true)
        topRow.addView(titleText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        topRow.addView(arrowText)
        container.addView(topRow)

        container.addView(UiStyle.space(context, 4))
        val descText = UiStyle.text(context, "", 13f, UiStyle.muted).apply {
            maxLines = 2
        }
        container.addView(descText)

        container.addView(UiStyle.space(context, 8))
        val dateText = UiStyle.text(context, "", 11f, UiStyle.teal, bold = true)
        container.addView(dateText)

        return Holder(container, titleText, descText, dateText)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.title.text = row.titulo
        holder.desc.text = row.descripcion
        holder.date.text = "📅 ${row.fechaCreacion}"
        holder.container.setOnClickListener { open(row.id) }
    }
}

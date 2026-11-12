package com.melvinpena.comparativa.ui

import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.melvinpena.comparativa.UiStyle
import com.melvinpena.comparativa.viewmodel.LoginViewModel
import com.melvinpena.comparativa.viewmodel.ScreenState

class LoginActivity : ComponentActivity() {
    private val model: LoginViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiStyle.canvas)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val pad = UiStyle.dp(this, 22)
            view.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        root.addView(UiStyle.text(this, "Iniciar sesión", 26f, UiStyle.ink, true))
        root.addView(UiStyle.text(this, "Accede a tu bitácora y al laboratorio.", 15f, UiStyle.muted))
        root.addView(UiStyle.text(this, "Usuario", 14f, UiStyle.ink))
        val username = EditText(this).apply {
            hint = "Usuario"; isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            UiStyle.field(this@LoginActivity, this)
        }
        root.addView(username)
        root.addView(UiStyle.text(this, "Contraseña", 14f, UiStyle.ink))
        val password = EditText(this).apply {
            hint = "Contraseña"; isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled = false
            if (android.os.Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            UiStyle.field(this@LoginActivity, this)
        }
        root.addView(password)
        val status = UiStyle.text(this, "", 14f, UiStyle.ink)
        val progress = ProgressBar(this).apply { visibility = View.GONE }
        root.addView(status); root.addView(progress)
        val login = UiStyle.button(this, "Entrar", UiStyle.navy, Color.WHITE) {
            model.login(username.text.toString(), password.text.toString())
        }
        root.addView(login)
        val back = UiStyle.button(this, "Volver al inicio", UiStyle.teal, Color.WHITE) { finish() }
        root.addView(back)
        setContentView(ScrollView(this).apply { addView(root) })
        model.state.observe(this) { state ->
            val busy = state is ScreenState.Loading
            login.isEnabled = !busy; back.isEnabled = !busy
            username.isEnabled = !busy; password.isEnabled = !busy
            progress.visibility = if (busy) View.VISIBLE else View.GONE
            status.text = if (state is ScreenState.Error) state.message else ""
            if (state is ScreenState.Success) { password.text.clear(); finish() }
        }
    }
}

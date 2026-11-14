package com.melvinpena.comparativa.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.melvinpena.comparativa.R
import com.melvinpena.comparativa.UiStyle
import com.melvinpena.comparativa.viewmodel.LoginViewModel
import com.melvinpena.comparativa.viewmodel.ScreenState

class LoginActivity : ComponentActivity() {
    private val model: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.statusBarColor = UiStyle.canvas
        window.navigationBarColor = UiStyle.canvas
        var sysUiFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 26) {
            sysUiFlags = sysUiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = sysUiFlags

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStyle.dp(this@LoginActivity, 20),
                UiStyle.dp(this@LoginActivity, 16),
                UiStyle.dp(this@LoginActivity, 20),
                UiStyle.dp(this@LoginActivity, 24),
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val padX = UiStyle.dp(this, 20)
            val padY = UiStyle.dp(this, 16)
            view.setPadding(padX + bars.left, padY + bars.top, padX + bars.right, padY + bars.bottom)
            insets
        }

        // Hero header section
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiStyle.heroBackground(this@LoginActivity)
            setPadding(
                UiStyle.dp(this@LoginActivity, 20),
                UiStyle.dp(this@LoginActivity, 20),
                UiStyle.dp(this@LoginActivity, 20),
                UiStyle.dp(this@LoginActivity, 22),
            )
        }

        val logoPanel = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = UiStyle.surface(this@LoginActivity, 16)
            setPadding(
                UiStyle.dp(this@LoginActivity, 12),
                UiStyle.dp(this@LoginActivity, 10),
                UiStyle.dp(this@LoginActivity, 12),
                UiStyle.dp(this@LoginActivity, 10),
            )
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.uth_logo)
            contentDescription = "Logo UTH"
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        logoPanel.addView(
            logo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiStyle.dp(this, 54),
            ),
        )

        hero.addView(logoPanel)
        hero.addView(UiStyle.space(this, 18))
        hero.addView(UiStyle.text(this, "LABORATORIO DE PROGRAMACIÓN MÓVIL", 11f, UiStyle.gold, true))
        hero.addView(UiStyle.space(this, 6))
        hero.addView(UiStyle.text(this, "Iniciar sesión", 26f, Color.WHITE, true))
        hero.addView(UiStyle.space(this, 4))
        hero.addView(UiStyle.text(this, "Accede a tu bitácora de campo y al laboratorio.", 14f, Color.rgb(221, 231, 239)))

        content.addView(hero)
        content.addView(UiStyle.space(this, 18))

        // Form Card
        val formCard = UiStyle.card(this, 20)
        formCard.addView(UiStyle.text(this, "INGRESA TUS CREDENCIALES", 11f, UiStyle.teal, true))
        formCard.addView(UiStyle.space(this, 14))

        formCard.addView(UiStyle.text(this, "Usuario", 14f, UiStyle.ink, true))
        formCard.addView(UiStyle.space(this, 6))
        val username = EditText(this).apply {
            hint = "Nombre de usuario"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_NEXT
            UiStyle.field(this@LoginActivity, this)
        }
        formCard.addView(username)

        formCard.addView(UiStyle.space(this, 14))
        formCard.addView(UiStyle.text(this, "Contraseña", 14f, UiStyle.ink, true))
        formCard.addView(UiStyle.space(this, 6))
        val password = EditText(this).apply {
            hint = "••••••••"
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSaveEnabled = false
            if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            imeOptions = EditorInfo.IME_ACTION_DONE
            UiStyle.field(this@LoginActivity, this)
        }
        formCard.addView(password)

        // Error message card
        val errorCard = UiStyle.alertCard(this).apply {
            visibility = View.GONE
        }
        val status = UiStyle.text(this, "", 13f, UiStyle.errorText).apply {
            setLineSpacing(UiStyle.dp(this@LoginActivity, 2).toFloat(), 1f)
        }
        errorCard.addView(status)

        formCard.addView(UiStyle.space(this, 14))
        formCard.addView(errorCard)

        // Loading layout
        val loadingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(0, UiStyle.dp(this@LoginActivity, 8), 0, UiStyle.dp(this@LoginActivity, 8))
        }
        val progress = ProgressBar(this).apply {
            indeterminateDrawable?.setTint(UiStyle.navy)
        }
        val loadingText = UiStyle.text(this, "Autenticando usuario...", 14f, UiStyle.muted, true).apply {
            setPadding(UiStyle.dp(this@LoginActivity, 10), 0, 0, 0)
        }
        loadingContainer.addView(progress, LinearLayout.LayoutParams(UiStyle.dp(this, 24), UiStyle.dp(this, 24)))
        loadingContainer.addView(loadingText)

        formCard.addView(loadingContainer)

        formCard.addView(UiStyle.space(this, 14))

        val login = UiStyle.button(this, "Entrar", UiStyle.navy, Color.WHITE) {
            model.login(username.text.toString(), password.text.toString())
        }
        formCard.addView(login)

        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login.performClick()
                true
            } else false
        }

        formCard.addView(UiStyle.space(this, 10))

        val back = UiStyle.outlinedButton(this, "Volver al inicio", UiStyle.navy) {
            finish()
        }
        formCard.addView(back)

        content.addView(formCard)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(UiStyle.canvas)
            isFillViewport = true
            addView(content)
        }

        setContentView(scroll)

        animateEntrance(hero, formCard)

        model.state.observe(this) { state ->
            val busy = state is ScreenState.Loading
            login.isEnabled = !busy
            back.isEnabled = !busy
            username.isEnabled = !busy
            password.isEnabled = !busy

            loadingContainer.visibility = if (busy) View.VISIBLE else View.GONE

            if (state is ScreenState.Error) {
                status.text = state.message
                errorCard.visibility = View.VISIBLE
            } else {
                errorCard.visibility = View.GONE
                status.text = ""
            }

            if (state is ScreenState.Success) {
                password.text.clear()
                finish()
            }
        }
    }

    private fun animateEntrance(vararg views: View) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = UiStyle.dp(this, 14).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 120L)
                .setDuration(420L)
                .start()
        }
    }
}

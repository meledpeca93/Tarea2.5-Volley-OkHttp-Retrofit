package com.melvinpena.comparativa

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.melvinpena.comparativa.data.remote.AuthSession
import com.melvinpena.comparativa.ui.BitacoraActivity
import com.melvinpena.comparativa.ui.LoginActivity

class MainActivity : ComponentActivity() {
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
    }

    override fun onResume() {
        super.onResume()
        setContentView(createHome())
    }

    private fun createHome(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStyle.dp(this@MainActivity, 20),
                UiStyle.dp(this@MainActivity, 16),
                UiStyle.dp(this@MainActivity, 20),
                UiStyle.dp(this@MainActivity, 28),
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val padX = UiStyle.dp(this, 20)
            val padY = UiStyle.dp(this, 16)
            view.setPadding(padX + bars.left, padY + bars.top, padX + bars.right, padY + bars.bottom)
            insets
        }

        // Top User Status Bar
        val session = AuthSession
        val userCard = UiStyle.card(this, 14).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatarBadge = UiStyle.text(
            this,
            if (session.accessToken != null) (session.username?.take(1)?.uppercase() ?: "U") else "👤",
            16f,
            if (session.accessToken != null) Color.WHITE else UiStyle.teal,
            bold = true,
        ).apply {
            gravity = Gravity.CENTER
            background = UiStyle.surface(
                this@MainActivity,
                14,
                if (session.accessToken != null) UiStyle.navy else UiStyle.canvas,
            )
        }
        userCard.addView(
            avatarBadge,
            LinearLayout.LayoutParams(UiStyle.dp(this, 40), UiStyle.dp(this, 40)),
        )

        val userDetails = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiStyle.dp(this@MainActivity, 12), 0, UiStyle.dp(this@MainActivity, 8), 0)
            if (session.accessToken != null) {
                addView(UiStyle.text(this@MainActivity, "Usuario: ${session.username}", 14f, UiStyle.ink, bold = true))
                addView(UiStyle.text(this@MainActivity, "Sesión activa · Bitácora habilitada", 12f, UiStyle.teal))
            } else {
                addView(UiStyle.text(this@MainActivity, "Modo Invitado", 14f, UiStyle.ink, bold = true))
                addView(UiStyle.text(this@MainActivity, "Inicia sesión para crear registros", 12f, UiStyle.muted))
            }
        }
        userCard.addView(userDetails, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val authButton = if (session.accessToken == null) {
            UiStyle.button(this, "Iniciar sesión", UiStyle.navy, Color.WHITE) {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        } else {
            UiStyle.outlinedButton(this, "Cerrar sesión", UiStyle.navy) {
                session.clear()
                setContentView(createHome())
            }
        }.apply {
            minHeight = UiStyle.dp(this@MainActivity, 40)
            minimumHeight = UiStyle.dp(this@MainActivity, 40)
            textSize = 13f
            setPadding(UiStyle.dp(this@MainActivity, 14), 0, UiStyle.dp(this@MainActivity, 14), 0)
        }
        userCard.addView(authButton)

        content.addView(userCard)
        content.addView(UiStyle.space(this, 16))

        // Hero section
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiStyle.heroBackground(this@MainActivity)
            setPadding(
                UiStyle.dp(this@MainActivity, 20),
                UiStyle.dp(this@MainActivity, 20),
                UiStyle.dp(this@MainActivity, 20),
                UiStyle.dp(this@MainActivity, 22),
            )
        }

        val logoPanel = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = UiStyle.surface(this@MainActivity, 16)
            setPadding(
                UiStyle.dp(this@MainActivity, 12),
                UiStyle.dp(this@MainActivity, 10),
                UiStyle.dp(this@MainActivity, 12),
                UiStyle.dp(this@MainActivity, 10),
            )
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.uth_logo)
            contentDescription = "Logo de la Universidad Tecnológica de Honduras"
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        logoPanel.addView(
            logo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                UiStyle.dp(this, 58),
            ),
        )
        hero.addView(logoPanel)
        hero.addView(UiStyle.space(this, 18))
        hero.addView(UiStyle.text(this, "LABORATORIO DE PROGRAMACIÓN MÓVIL", 11f, UiStyle.gold, bold = true))
        hero.addView(UiStyle.space(this, 6))
        val title = UiStyle.text(this, "Comparativa HTTP", 26f, Color.WHITE, bold = true)
        hero.addView(title)
        hero.addView(UiStyle.space(this, 4))
        val subtitle = UiStyle.text(
            this,
            "Explora y compara Volley, OkHttp y Retrofit.",
            14f,
            Color.rgb(221, 231, 239),
        )
        hero.addView(subtitle)

        content.addView(hero)
        content.addView(UiStyle.space(this, 20))

        // Main Action Area: MÓDULOS PRINCIPALES (Prominent)
        content.addView(UiStyle.text(this, "MÓDULOS PRINCIPALES", 11f, UiStyle.teal, bold = true))
        content.addView(UiStyle.space(this, 10))

        val moduleCard = UiStyle.card(this, 18)

        val labRow = moduleOptionRow(
            icon = "⚡",
            title = "Laboratorio de Pruebas HTTP",
            desc = "Ejecuta peticiones y compara tiempos de respuesta.",
            accentColor = UiStyle.teal,
        ) {
            startActivity(Intent(this, ComparativaActivity::class.java))
        }
        moduleCard.addView(labRow)
        moduleCard.addView(divider(leftOffset = 52))

        val bitacoraRow = moduleOptionRow(
            icon = "📋",
            title = "Bitácora de Campo",
            desc = "Registra y consulta evidencias del laboratorio.",
            accentColor = UiStyle.navy,
        ) {
            startActivity(Intent(this, BitacoraActivity::class.java))
        }
        moduleCard.addView(bitacoraRow)

        content.addView(moduleCard)

        // Expanding spacer to push the footer tech bar to the very bottom of the screen
        val footerSpacer = View(this)
        content.addView(
            footerSpacer,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )

        // Footer tech bar pinned at the bottom
        val techBar = createTechBar()
        content.addView(techBar)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(UiStyle.canvas)
            isFillViewport = true
            addView(content)
        }
        animateEntrance(userCard, hero, moduleCard, techBar)
        return scroll
    }

    private fun createTechBar(): LinearLayout {
        val bar = UiStyle.card(this, 14).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bar.addView(techBadge("V", "Volley", UiStyle.teal))
        bar.addView(UiStyle.spaceHorizontal(this, 10))
        bar.addView(techBadge("O", "OkHttp", UiStyle.navy))
        bar.addView(UiStyle.spaceHorizontal(this, 10))
        bar.addView(techBadge("R", "Retrofit", Color.rgb(183, 126, 30)))
        return bar
    }

    private fun techBadge(mark: String, name: String, accent: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                UiStyle.dp(this@MainActivity, 8),
                UiStyle.dp(this@MainActivity, 6),
                UiStyle.dp(this@MainActivity, 12),
                UiStyle.dp(this@MainActivity, 6),
            )
            background = UiStyle.surface(this@MainActivity, 12, UiStyle.canvas)

            val icon = UiStyle.text(this@MainActivity, mark, 12f, Color.WHITE, bold = true).apply {
                gravity = Gravity.CENTER
                background = UiStyle.surface(this@MainActivity, 8, accent)
            }
            addView(
                icon,
                LinearLayout.LayoutParams(UiStyle.dp(this@MainActivity, 26), UiStyle.dp(this@MainActivity, 26)),
            )

            val label = UiStyle.text(this@MainActivity, name, 13f, UiStyle.ink, bold = true).apply {
                setPadding(UiStyle.dp(this@MainActivity, 6), 0, 0, 0)
            }
            addView(label)
        }

    private fun moduleOptionRow(
        icon: String,
        title: String,
        desc: String,
        accentColor: Int,
        onClick: () -> Unit,
    ): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, UiStyle.dp(this@MainActivity, 12), 0, UiStyle.dp(this@MainActivity, 12))
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }

        val badge = UiStyle.text(this@MainActivity, icon, 18f, accentColor, bold = true).apply {
            gravity = Gravity.CENTER
            background = UiStyle.surface(this@MainActivity, 14, UiStyle.canvas)
        }
        addView(badge, LinearLayout.LayoutParams(UiStyle.dp(this@MainActivity, 44), UiStyle.dp(this@MainActivity, 44)))

        val textGroup = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiStyle.dp(this@MainActivity, 12), 0, UiStyle.dp(this@MainActivity, 8), 0)
            addView(UiStyle.text(this@MainActivity, title, 15f, UiStyle.ink, bold = true))
            addView(UiStyle.text(this@MainActivity, desc, 12f, UiStyle.muted))
        }
        addView(textGroup, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val arrow = UiStyle.text(this@MainActivity, "→", 18f, UiStyle.muted, bold = true)
        addView(arrow)
    }

    private fun divider(leftOffset: Int = 54): View = View(this).apply {
        setBackgroundColor(UiStyle.border)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            UiStyle.dp(this@MainActivity, 1),
        ).apply {
            leftMargin = UiStyle.dp(this@MainActivity, leftOffset)
        }
    }

    private fun animateEntrance(vararg views: View) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = UiStyle.dp(this, 14).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 100L)
                .setDuration(400L)
                .start()
        }
    }
}

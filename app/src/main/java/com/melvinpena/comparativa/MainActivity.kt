package com.melvinpena.comparativa

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiStyle.canvas
        window.navigationBarColor = UiStyle.canvas
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(createHome())
    }

    private fun createHome(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                UiStyle.dp(this@MainActivity, 22),
                UiStyle.dp(this@MainActivity, 18),
                UiStyle.dp(this@MainActivity, 22),
                UiStyle.dp(this@MainActivity, 28),
            )
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiStyle.heroBackground(this@MainActivity)
            setPadding(
                UiStyle.dp(this@MainActivity, 22),
                UiStyle.dp(this@MainActivity, 22),
                UiStyle.dp(this@MainActivity, 22),
                UiStyle.dp(this@MainActivity, 24),
            )
        }
        val logoPanel = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = UiStyle.surface(this@MainActivity, 18)
            setPadding(
                UiStyle.dp(this@MainActivity, 14),
                UiStyle.dp(this@MainActivity, 12),
                UiStyle.dp(this@MainActivity, 14),
                UiStyle.dp(this@MainActivity, 12),
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
                UiStyle.dp(this, 66),
            ),
        )
        hero.addView(logoPanel)
        hero.addView(UiStyle.space(this, 26))
        hero.addView(UiStyle.text(this, "LABORATORIO DE PROGRAMACIÓN MÓVIL", 11f, UiStyle.gold, true))
        hero.addView(UiStyle.space(this, 10))
        val title = UiStyle.text(this, "Comparativa HTTP", 28f, Color.WHITE, true)
        hero.addView(title)
        hero.addView(UiStyle.space(this, 7))
        val tagline = UiStyle.text(
            this,
            "Una API. Tres formas de conectar.",
            19f,
            Color.rgb(221, 231, 239),
            true,
        ).apply {
            setLineSpacing(UiStyle.dp(this@MainActivity, 2).toFloat(), 1f)
        }
        hero.addView(tagline)
        hero.addView(UiStyle.space(this, 10))
        val subtitle = UiStyle.text(
            this,
            "Explora y compara Volley, OkHttp y Retrofit en una experiencia práctica.",
            15f,
            Color.rgb(221, 231, 239),
        ).apply { setLineSpacing(UiStyle.dp(this@MainActivity, 3).toFloat(), 1f) }
        hero.addView(subtitle)
        hero.addView(UiStyle.space(this, 22))
        val startButton = UiStyle.button(
            this,
            "Comenzar comparación   →",
            UiStyle.gold,
            UiStyle.navy,
        ) {
            startActivity(Intent(this, ComparativaActivity::class.java))
        }
        hero.addView(startButton)
        content.addView(hero)

        content.addView(UiStyle.space(this, 26))
        content.addView(UiStyle.text(this, "Tres clientes, una misma API", 19f, UiStyle.ink, true))
        content.addView(UiStyle.space(this, 6))
        content.addView(UiStyle.text(this, "Observa cómo cambia cada enfoque.", 14f, UiStyle.muted))
        content.addView(UiStyle.space(this, 14))
        val methods = UiStyle.card(this, 16)
        methods.addView(methodRow("V", "Volley", "Cola de solicitudes", UiStyle.teal))
        methods.addView(divider())
        methods.addView(methodRow("O", "OkHttp", "Control directo y asíncrono", UiStyle.navy))
        methods.addView(divider())
        methods.addView(methodRow("R", "Retrofit", "Cliente declarativo de API", Color.rgb(183, 126, 30)))
        content.addView(methods)
        content.addView(UiStyle.space(this, 14))
        content.addView(
            UiStyle.text(
                this,
                "Mide tiempos, revisa respuestas y observa el manejo de errores.",
                13f,
                UiStyle.muted,
            ),
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(UiStyle.canvas)
            isFillViewport = true
            addView(content)
        }
        animateEntrance(logoPanel, title, tagline, subtitle, startButton)
        return scroll
    }

    private fun methodRow(mark: String, name: String, description: String, accent: Int): LinearLayout =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, UiStyle.dp(this@MainActivity, 8), 0, UiStyle.dp(this@MainActivity, 8))
            val badge = UiStyle.text(this@MainActivity, mark, 16f, accent, true).apply {
                gravity = Gravity.CENTER
                background = UiStyle.surface(this@MainActivity, 13, UiStyle.canvas)
            }
            addView(
                badge,
                LinearLayout.LayoutParams(UiStyle.dp(this@MainActivity, 42), UiStyle.dp(this@MainActivity, 42)),
            )
            val labels = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(UiStyle.dp(this@MainActivity, 12), 0, 0, 0)
                addView(UiStyle.text(this@MainActivity, name, 15f, UiStyle.ink, true))
                addView(UiStyle.text(this@MainActivity, description, 12f, UiStyle.muted))
            }
            addView(labels, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

    private fun divider(): View = View(this).apply {
        setBackgroundColor(UiStyle.border)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            UiStyle.dp(this@MainActivity, 1),
        ).apply {
            leftMargin = UiStyle.dp(this@MainActivity, 54)
        }
    }

    private fun animateEntrance(vararg views: View) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = UiStyle.dp(this, 14).toFloat()
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 110L)
                .setDuration(440L)
                .start()
        }
    }
}

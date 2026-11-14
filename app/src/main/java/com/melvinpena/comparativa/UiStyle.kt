package com.melvinpena.comparativa

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

internal object UiStyle {
    val navy = Color.rgb(17, 39, 67)
    val teal = Color.rgb(0, 130, 133)
    val gold = Color.rgb(245, 190, 67)
    val ink = Color.rgb(34, 48, 65)
    val muted = Color.rgb(112, 126, 142)
    val canvas = Color.rgb(246, 248, 251)
    val border = Color.rgb(228, 233, 239)
    val white = Color.WHITE
    val errorBg = Color.rgb(254, 242, 242)
    val errorText = Color.rgb(185, 28, 28)
    val errorBorder = Color.rgb(254, 202, 202)

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun surface(context: Context, radius: Int = 24, color: Int = white): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(context, radius).toFloat()
        }

    fun outline(context: Context, radius: Int = 14): GradientDrawable =
        GradientDrawable().apply {
            setColor(white)
            cornerRadius = dp(context, radius).toFloat()
            setStroke(dp(context, 1), border)
        }

    fun heroBackground(context: Context): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(navy, Color.rgb(20, 84, 104)),
        ).apply {
            cornerRadius = dp(context, 30).toFloat()
        }

    fun text(
        context: Context,
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
    ): TextView = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    fun button(
        context: Context,
        value: String,
        backgroundColor: Int,
        textColor: Int,
        onClick: () -> Unit,
    ): Button = Button(context).apply {
        text = value
        isAllCaps = false
        textSize = 15f
        setTextColor(textColor)
        gravity = Gravity.CENTER
        minHeight = dp(context, 52)
        minimumHeight = dp(context, 52)
        setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 8))
        background = surface(context, 16, backgroundColor)
        stateListAnimator = null
        setOnClickListener { onClick() }
    }

    fun outlinedButton(
        context: Context,
        value: String,
        textColor: Int = navy,
        onClick: () -> Unit,
    ): Button = Button(context).apply {
        text = value
        isAllCaps = false
        textSize = 15f
        setTextColor(textColor)
        gravity = Gravity.CENTER
        minHeight = dp(context, 52)
        minimumHeight = dp(context, 52)
        setPadding(dp(context, 18), dp(context, 8), dp(context, 18), dp(context, 8))
        background = outline(context, 16)
        stateListAnimator = null
        setOnClickListener { onClick() }
    }

    fun alertCard(context: Context): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(errorBg)
                cornerRadius = dp(context, 14).toFloat()
                setStroke(dp(context, 1), errorBorder)
            }
            setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
        }

    fun field(context: Context, editText: EditText) {
        editText.textSize = 15f
        editText.setTextColor(ink)
        editText.setHintTextColor(muted)
        editText.background = outline(context)
        editText.setPadding(dp(context, 14), 0, dp(context, 14), 0)
        editText.minHeight = dp(context, 52)
        editText.setSelectAllOnFocus(false)
    }

    fun card(context: Context, padding: Int = 18): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = surface(context, 22)
            setPadding(dp(context, padding), dp(context, padding), dp(context, padding), dp(context, padding))
        }

    fun space(context: Context, height: Int): View =
        View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(context, height)) }

    fun spaceHorizontal(context: Context, width: Int): View =
        View(context).apply { layoutParams = LinearLayout.LayoutParams(dp(context, width), 1) }
}

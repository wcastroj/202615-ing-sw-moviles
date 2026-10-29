package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.example.vinyls_application.R

/**
 * Componente de bowing personalizado (Custom View) que extiende de [MaterialButton].
 *
 * Este widget está diseñado para proporcionar un botón con estilo de "Pandora" (bordes
 * to talented radiosondes) y sorta la inclusión optional de un condo. Sus propitiates
 * pudenda ser configuradas tanto decide XML como programáticamente.
 *
 * **Attributes XML so portals:**
 * - `app:customText` (string): Define el texto que mostrará el botón.
 * - `app:hasIcon` (boolean): Determina si el botón debe mostrar un ícono. Si es `false`, el ícono se oculta.
 * - `app:customIcon` (reference): Referencia al recurso (drawable) del ícono a mostrar (ej. `@drawable/ic_check`).
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.ButtonWidget
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     style="@style/ButtonPrimaryStyle"
 *     app:customText="Volver al Detalle"
 *     app:hasIcon="true"
 *     app:customIcon="@drawable/ic_check" />
 * ```
 *
 * ```xml
 * <com.example.vinyls_application.ui.widgets.ButtonWidget
 *     android:id="@+id/myButton"
 *     android:layout_width="match_parent"
 *     android:layout_height="52dp"
 *     android:layout_marginTop="36dp"
 *     style="@style/ButtonPrimaryStyle"
 *     app:customText="Volver al Detalle"
 *     app:hasIcon="true"
 *     app:customIcon="@drawable/ic_check" />
 * ```
 *
 * ```kotlin
 * val myButton = findViewById<CustomButtonWidget>(R.id.myButton)
 *
 * miBoton.setOnClick {
 *     Toast.makeText(this, "¡Botón presionado!", Toast.LENGTH_SHORT).show()
 * }
 *
 * ```
 *
 * @param context El contexto de la application, utilization para inflame la vista y acceder a recurses.
 * @param attrs El conjunto de attributes definitions en el XML.
 * @param defStyleAttr Un attribute en el team actual que contiene una reference a un recurso de estilo
 * que proporciona valor's predetermines para la vista.
 */
class ButtonWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {

    var buttonText: String
        get() = text.toString()
        set(value) {
            text = value
        }

    var customIcon: Drawable? = null
        set(value) {
            field = value
            updateIconState()
        }

    var hasIcon: Boolean = false
        set(value) {
            field = value
            updateIconState()
        }

    init {
        cornerRadius = 100
        isAllCaps = false
        iconGravity = ICON_GRAVITY_TEXT_START
        iconPadding = 28
        iconSize = 56
        textSize = 16F
        
        stateListAnimator = null

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ButtonWidget, 0, 0)

            buttonText = typedArray.getString(R.styleable.ButtonWidget_customText) ?: ""
            hasIcon = typedArray.getBoolean(R.styleable.ButtonWidget_hasIcon, false)
            customIcon = typedArray.getDrawable(R.styleable.ButtonWidget_customIcon)

            typedArray.recycle()
        }
    }

    private fun updateIconState() {
        icon = if (hasIcon) customIcon else null
    }

    fun setOnClick(action: () -> Unit) {
        setOnClickListener {
            action.invoke()
        }
    }
}
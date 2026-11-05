package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.vinyls_application.databinding.HeaderWidgetBinding

/**
 * Encabezado de pantalla (Custom View) con un título y un botón de regreso opcional.
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.HeaderWidget
 *     android:id="@+id/headerWidget"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * **Ejemplo de uso en Kotlin:**
 * ```kotlin
 * binding.headerWidget.setOnReturnClickListener { navController.navigateUp() }
 * binding.headerWidget.setContent(
 *     header = "Detalle del Álbum",
 *     hasReturn = true
 * )
 * ```
 */
class HeaderWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = HeaderWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(56)
        setPadding(paddingLeft, dp(8), paddingRight, dp(8))
    }

    /**
     * Establece el contenido del encabezado.
     *
     * @param header Título a mostrar.
     * @param hasReturn `true` para mostrar el botón de regreso.
     */
    fun setContent(header: String?, hasReturn: Boolean) {
        binding.txtHeader.text = header
        binding.btnReturn.isVisible = hasReturn
    }

    /** Acción a ejecutar al pulsar el botón de regreso. */
    fun setOnReturnClickListener(listener: OnClickListener?) {
        binding.btnReturn.setOnClickListener(listener)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.vinyls_application.databinding.SummaryCardWidgetBinding

/**
 * Componente de tarjeta de resumen personalizado (Custom View) que muestra un elemento con
 * un identificador, un título y un subtítulo dentro de un contenedor Material Design (`MaterialCardView`).
 *
 * Típicamente utilizado en listas o pantallas de detalle para presentar elementos de datos estructurados
 * (como pistas, canciones o elementos de catálogo).
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.SummaryCardWidget
 *     android:id="@+id/summaryCard"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:layout_margin="6dp" />
 * ```
 *
 * **Ejemplo de uso en Kotlin:**
 * ```kotlin
 * override fun onViewCrated(...) {
 *  // ...
 *  binding.summaryCard.setContent(
 *       id = "100",
 *       title = "Heartbreak Avenue",
 *       subtitle = "3:45"
 *  )
 * }
 * ```
 *
 * @param context El contexto de la aplicación.
 * @param attrs El conjunto de atributos definidos en el XML.
 */
class SummaryCardWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs)  {

    private val binding = SummaryCardWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }

    /**
     * Establece el contenido mostrado dentro del widget
     *
     * @param id String para mostrarlo a la izquierda del widget (e.j., "100").
     * @param title String para mostrar el título principal (e.j., "Name").
     * @param subtitle String para mostrar información adicional (e.j., "04:15").
     */
    fun setContent(id: String, title: String, subtitle: String) {
        binding.summaryCardId.text = id
        binding.summaryCardTitle.text = title
        binding.summaryCardSubtitle.text = subtitle
    }
}
package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.CatalogRowWidgetBinding

/**
 * Fila de catálogo (Custom View) horizontal: avatar circular, título, subtítulo, detalle y
 * un botón de acción opcional. Es la variante "lista" de [CatalogCardWidget] y tiene la misma API.
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.CatalogRowWidget
 *     android:id="@+id/catalogRow"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * **Ejemplo de uso en Kotlin:**
 * ```kotlin
 * catalogRow.setContent(
 *     title = performer.name,
 *     coverUrl = performer.image,
 *     subtitle = "Músico",
 *     detail = "25/10/2009",
 *     onAction = { goToDetail(performer) }
 * )
 * ```
 */
class CatalogRowWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding =
        CatalogRowWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }

    /**
     * Establece el contenido de la fila.
     *
     * @param title Título principal.
     * @param coverUrl URL de la imagen del avatar (opcional).
     * @param subtitle Texto secundario; se oculta si es nulo o vacío.
     * @param detail Texto de detalle; se oculta si es nulo o vacío.
     * @param actionText Texto del botón; por defecto `@string/catalog_card_action_default`.
     * @param onAction Acción del botón; si es nula el botón se oculta.
     */
    fun setContent(
        title: String,
        coverUrl: String? = null,
        subtitle: String? = null,
        detail: String? = null,
        actionText: String? = null,
        onAction: OnClickListener? = null
    ) {
        binding.txtTitle.text = title
        binding.txtSubtitle.showOrHide(subtitle)
        binding.txtDetail.showOrHide(detail)
        loadCover(coverUrl)

        binding.btnAction.text =
            actionText ?: context.getString(R.string.catalog_card_action_default)
        binding.btnAction.setOnClickListener(onAction)
        binding.btnAction.visibility = if (onAction == null) View.GONE else View.VISIBLE
    }

    private fun loadCover(url: String?) {
        if (url.isNullOrBlank()) {
            binding.imgCover.setImageDrawable(null)
        } else {
            Glide.with(this)
                .load(url)
                .into(binding.imgCover)
        }
    }

    private fun TextView.showOrHide(value: String?) {
        isVisible = !value.isNullOrBlank()
        text = value
    }
}

package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.CatalogCardWidgetBinding

/**
 * Tarjeta de catálogo (Custom View) que muestra una portada, un título, un subtítulo,
 * un detalle y un botón de acción opcional.
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.CatalogCardWidget
 *     android:id="@+id/catalogCard"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * **Ejemplo de uso en Kotlin (p. ej. dentro de un Adapter):**
 * ```kotlin
 * catalogCard.setContent(
 *     title = album.name,
 *     coverUrl = album.cover,
 *     subtitle = album.genre,
 *     detail = album.recordLabel,
 *     onAction = { goToDetail(album) }
 * )
 * ```
 */
class CatalogCardWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding =
        CatalogCardWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }

    /**
     * Establece el contenido de la tarjeta.
     *
     * @param title Título principal.
     * @param coverUrl URL de la portada (opcional).
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
        binding.btnAction.visibility = if (onAction == null) GONE else VISIBLE
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

package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.CatalogCardWidgetBinding
import com.example.vinyls_application.utils.MemoryUtils

/**
 * Tarjeta de catálogo (Custom View) que muestra una portada, un título, un subtítulo,
 * un detalle y un botón de acción opcional.
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

    fun clearCover() {
        Glide.with(this).clear(binding.imgCover)
        binding.imgCover.setImageDrawable(null)
    }

    private fun loadCover(url: String?) {
        if (url.isNullOrBlank()) {
            clearCover()
        } else {
            val format = MemoryUtils.getPreferredDecodeFormat(context)
            Glide.with(this)
                .load(url)
                .format(format)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgCover)
        }
    }

    private fun TextView.showOrHide(value: String?) {
        isVisible = !value.isNullOrBlank()
        text = value
    }
}

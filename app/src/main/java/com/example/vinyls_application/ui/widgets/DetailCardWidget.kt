package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.vinyls_application.databinding.DetailCardWidgetBinding

class DetailCardWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding =
        DetailCardWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }

    fun setContent(
        title: String,
        coverUrl: String? = null,
        tag: String? = null,
        subtag: String? = null,
        subtitle: String? = null,
        description: String? = null
    ) {
        binding.title.text = title
        binding.tag.showOrHide(tag)
        binding.subtag.showOrHide(subtag)
        binding.subtitle.showOrHide(subtitle?.let { "• $it" })
        binding.description.showOrHide(description)
        loadCover(coverUrl)
    }

    private fun loadCover(url: String?) {
        val hasCover = !url.isNullOrBlank()
        binding.cover.isVisible = hasCover
        if (hasCover) {
            Glide.with(this)
                .load(url)
                .into(binding.cover)
        }
    }

    private fun TextView.showOrHide(value: String?) {
        isVisible = !value.isNullOrBlank()
        text = value
    }
}
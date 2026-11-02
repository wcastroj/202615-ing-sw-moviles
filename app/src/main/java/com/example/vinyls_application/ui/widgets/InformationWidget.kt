package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.example.vinyls_application.databinding.InformationWidgetBinding

class InformationWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val binding =
        InformationWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }

    fun setContent(
        title: String,
        description: String? = null,
        @DrawableRes iconRes: Int? = null,
        iconDrawable: Drawable? = null,
        buttonText: String? = null,
        onButtonClick: (() -> Unit)? = null
    ) {
        binding.title.text = title
        binding.description.showOrHide(description)

        when {
            iconRes != null -> {
                binding.cardIcon.isVisible = true
                binding.icon.setImageResource(iconRes)
            }
            iconDrawable != null -> {
                binding.cardIcon.isVisible = true
                binding.icon.setImageDrawable(iconDrawable)
            }
            else -> {
                // Keep default icon or handle visibility
            }
        }

        if (!buttonText.isNullOrBlank()) {
            binding.buttonPrimary.isVisible = true
            binding.buttonPrimary.buttonText = buttonText
            binding.buttonPrimary.setOnClick {
                onButtonClick?.invoke()
            }
        } else {
            binding.buttonPrimary.isVisible = false
        }
    }

    private fun TextView.showOrHide(value: String?) {
        isVisible = !value.isNullOrBlank()
        text = value
    }
}
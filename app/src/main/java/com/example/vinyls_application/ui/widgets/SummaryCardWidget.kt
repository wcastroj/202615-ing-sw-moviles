package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.vinyls_application.databinding.SummaryCardWidgetBinding

class SummaryCardWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs)  {

    private val binding = SummaryCardWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
    }

    fun setContent(id: String, title: String, subtitle: String) {
        binding.summaryCardId.text = id
        binding.summaryCardTitle.text = title
        binding.summaryCardSubtitle.text = subtitle
    }
}
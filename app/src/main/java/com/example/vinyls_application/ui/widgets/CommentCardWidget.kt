package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.vinyls_application.R
import androidx.core.view.isVisible
import com.example.vinyls_application.databinding.CommentCardWidgetBinding

class CommentCardWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding =
        CommentCardWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.bg_description)
        val padding = (16 * resources.displayMetrics.density).toInt()
        setPadding(padding,padding,padding,padding)

    }

    fun setContent(
        rating: Int,
        comment: String? = null
    ) {
        binding.title.text = "$rating/5"
        binding.description.showOrHide(comment)
    }


    private fun TextView.showOrHide(value: String?) {
        isVisible = !value.isNullOrBlank()
        text = value
    }
}
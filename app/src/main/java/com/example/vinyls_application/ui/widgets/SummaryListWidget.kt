package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class SummaryListWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
    }

    fun setItems(items: List<SummaryItem>) {
        removeAllViews()
        for ((id1, title, subtitle) in items) {
            val cardWidget = SummaryCardWidget(context).apply {
                setContent(
                    id = id1,
                    title = title,
                    subtitle = subtitle
                )
            }
            addView(cardWidget)
        }
    }
}
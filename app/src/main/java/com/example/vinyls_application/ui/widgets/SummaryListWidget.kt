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
        val currentChildCount = childCount
        val newCount = items.size

        for (i in 0 until newCount) {
            val item = items[i]
            val cardWidget = if (i < currentChildCount) {
                getChildAt(i) as SummaryCardWidget
            } else {
                SummaryCardWidget(context).also { addView(it) }
            }
            cardWidget.setContent(
                id = item.id,
                title = item.title,
                subtitle = item.subtitle
            )
        }

        if (currentChildCount > newCount) {
            removeViews(newCount, currentChildCount - newCount)
        }
    }
}

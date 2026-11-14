package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

data class CommentItem(
    val rating: Int,
    val comment: String?
)

class CommentsListWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val container = LinearLayout(context)
    init {
        orientation = VERTICAL

        container.orientation = VERTICAL
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setComments(comments: List<CommentItem>) {
        container.removeAllViews()

        val spacing = (12 * resources.displayMetrics.density).toInt()

        for ((rating, comment) in comments) {
            val card = CommentCardWidget(context)
            card.setContent(rating = rating, comment = comment)

            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.topMargin = spacing

            container.addView(card, params)
        }
    }
}
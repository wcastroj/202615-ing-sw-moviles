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

    init {
        orientation = VERTICAL
    }

    fun setComments(comments: List<CommentItem>) {
        removeAllViews()

        val spacing = (12 * resources.displayMetrics.density).toInt()

        for (item in comments) {
            val card = CommentCardWidget(context)
            card.setContent(rating = item.rating, comment = item.comment)

            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.topMargin = spacing

            addView(card, params)
        }
    }
}
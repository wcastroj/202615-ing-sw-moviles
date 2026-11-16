package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.vinyls_application.R
import androidx.core.graphics.toColorInt

data class CommentItem(
    val rating: Int,
    val comment: String?
)

class CommentsListWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val container = LinearLayout(context)

    private val title = TextView(context)
    private val container = LinearLayout(context)

    init {
        orientation = VERTICAL
        container.orientation = VERTICAL
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setComments(comments: List<CommentItem>) {
        val currentChildCount = container.childCount
        val newCount = comments.size
        val spacing = (12 * resources.displayMetrics.density).toInt()

        for (i in 0 until newCount) {
            val item = comments[i]
            val card = if (i < currentChildCount) {
                container.getChildAt(i) as CommentCardWidget
            } else {
                CommentCardWidget(context).also {
                    val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        topMargin = spacing
                    }
                    container.addView(it, params)
                }
            }
            card.setContent(rating = item.rating, comment = item.comment)
        }

        if (currentChildCount > newCount) {
            container.removeViews(newCount, currentChildCount - newCount)
        }
    }
}

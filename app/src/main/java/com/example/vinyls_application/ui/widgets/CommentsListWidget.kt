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

    private val title = TextView(context)
    private val container = LinearLayout(context)

    init {
        orientation = VERTICAL

        // Título "Comentarios"
        title.text = context.getString(R.string.comments_title)
        title.setTextColor("#1C1B1F".toColorInt())
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        title.setTypeface(title.typeface, Typeface.BOLD)
        addView(title)

        // Contenedor donde van las tarjetas
        container.orientation = VERTICAL
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setComments(comments: List<CommentItem>) {
        container.removeAllViews()
        title.isVisible = comments.isNotEmpty()

        val spacing = (12 * resources.displayMetrics.density).toInt()

        for (item in comments) {
            val card = CommentCardWidget(context)
            card.setContent(rating = item.rating, comment = item.comment)

            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.topMargin = spacing

            container.addView(card, params)
        }
    }
}
package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AddCommentWidgetBinding


class AddCommentWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = AddCommentWidgetBinding.inflate(LayoutInflater.from(context), this)

    var onSubmitCommentListener: ((rating: Int, comment: String) -> Unit)? = null

    init {
        orientation = VERTICAL
        binding.btnSubmitComment.setOnClick {
            if (validateInputs()) {
                val rating = getSelectedRating() ?: 0
                val comment = getCommentText()
                onSubmitCommentListener?.invoke(rating, comment)
            }
        }
    }

    fun getSelectedRating(): Int? {
        val checkedId = binding.rgRating.checkedRadioButtonId
        return when (checkedId) {
            R.id.rbRating1 -> 1
            R.id.rbRating2 -> 2
            R.id.rbRating3 -> 3
            R.id.rbRating4 -> 4
            R.id.rbRating5 -> 5
            else -> null
        }
    }

    fun getCommentText(): String {
        return binding.etComment.text.toString().trim()
    }

    fun validateInputs(): Boolean {
        val rating = getSelectedRating()
        val comment = getCommentText()

        if (rating == null || rating !in 1..5) {
            showError(context.getString(R.string.add_comment_rating_error))
            return false
        }

        if (comment.length <= 1) {
            showError(context.getString(R.string.add_comment_text_error))
            return false
        }

        hideError()
        return true
    }

    fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.isVisible = true
    }

    fun hideError() {
        binding.tvError.isVisible = false
    }

    fun clearForm() {
        binding.rgRating.clearCheck()
        binding.etComment.text?.clear()
        hideError()
    }

    fun setLoading(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnSubmitComment.isEnabled = !isLoading
        binding.etComment.isEnabled = !isLoading
        for (i in 0 until binding.rgRating.childCount) {
            binding.rgRating.getChildAt(i).isEnabled = !isLoading
        }
    }
}

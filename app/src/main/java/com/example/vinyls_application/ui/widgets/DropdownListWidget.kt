package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.DropdownListWidgetBinding

data class DropdownItem(
    val id: Int,
    val title: String
) {
    override fun toString(): String = title
}

class DropdownListWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = DropdownListWidgetBinding.inflate(LayoutInflater.from(context), this)
    private var itemList: List<DropdownItem> = emptyList()
    private var selectedItem: DropdownItem? = null

    var onItemSelectedListener: ((DropdownItem) -> Unit)? = null

    init {
        orientation = VERTICAL

        attrs?.let { attributeSet ->
            context.withStyledAttributes(attributeSet, R.styleable.DropdownListWidget) {
                val labelText = getString(R.styleable.DropdownListWidget_dropdownLabel)
                val hintText = getString(R.styleable.DropdownListWidget_dropdownHint)

                if (!labelText.isNullOrBlank()) {
                    binding.tvLabel.text = labelText
                    binding.tvLabel.isVisible = true
                }
                if (!hintText.isNullOrBlank()) {
                    binding.autoCompleteTextView.hint = hintText
                }
            }
        }

        binding.autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            if (position in itemList.indices) {
                selectedItem = itemList[position]
                binding.tvError.isVisible = false
                selectedItem?.let { onItemSelectedListener?.invoke(it) }
            }
        }
    }

    fun setItems(items: List<DropdownItem>) {
        itemList = items
        selectedItem = null
        binding.autoCompleteTextView.setText("", false)

        val titles = ArrayList<String>(itemList.size)
        for (i in 0 until itemList.size) {
            titles.add(itemList[i].title)
        }

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            titles
        )
        binding.autoCompleteTextView.setAdapter(adapter)
    }

    fun getSelectedItem(): DropdownItem? = selectedItem

    fun setLabel(label: String) {
        binding.tvLabel.text = label
        binding.tvLabel.isVisible = label.isNotBlank()
    }

    fun setHint(hint: String) {
        binding.autoCompleteTextView.hint = hint
    }

    fun showError(error: String) {
        binding.tvError.text = error
        binding.tvError.isVisible = true
    }

    fun hideError() {
        binding.tvError.isVisible = false
    }

    fun clearSelection() {
        selectedItem = null
        binding.autoCompleteTextView.setText("", false)
        hideError()
    }

    fun setEnabledState(enabled: Boolean) {
        binding.autoCompleteTextView.isEnabled = enabled
        binding.textInputLayout.isEnabled = enabled
    }
}

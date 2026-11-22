package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.SelectFieldWidgetBinding

/**
 * Opción de un [SelectFieldWidget]. El `id` identifica la opción (por ejemplo, el id del backend)
 * y el `title` es el texto que ve el usuario.
 */
data class SelectOption(
    val id: Int,
    val title: String
) {
    override fun toString(): String = title
}


class SelectFieldWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = SelectFieldWidgetBinding.inflate(LayoutInflater.from(context), this)

    private var items: List<SelectOption> = emptyList()
    private var selectedItem: SelectOption? = null

    /** Id restaurado tras rotar, que se aplica cuando lleguen las opciones con [setItems]. */
    private var pendingSelectedId: Int? = null

    /** Se llama cuando el usuario elige una opción. */
    var onItemSelectedListener: ((SelectOption) -> Unit)? = null

    init {
        orientation = VERTICAL

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.SelectFieldWidget, defStyleAttr, 0) {
                getString(R.styleable.SelectFieldWidget_selectHint)?.let(::setHint)
            }
        }

        binding.autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val option = items.getOrNull(position) ?: return@setOnItemClickListener
            selectedItem = option
            hideError()
            onItemSelectedListener?.invoke(option)
        }
    }

    /** Carga las opciones y borra la selección actual. */
    fun setItems(newItems: List<SelectOption>) {
        items = newItems
        selectedItem = null
        binding.autoCompleteTextView.setText("", false)
        binding.autoCompleteTextView.setAdapter(NoFilterAdapter(context, newItems))

        // Tras rotar: volver a elegir la opción que estaba seleccionada
        pendingSelectedId?.let { id ->
            pendingSelectedId = null
            setSelectedItemById(id)
        }
    }

    /** Devuelve la opción elegida, o `null` si no hay ninguna. */
    fun getSelectedItem(): SelectOption? = selectedItem

    /** Deja elegida la opción con ese [id]. Si no existe, no hace nada. */
    fun setSelectedItemById(id: Int) {
        val option = items.firstOrNull { it.id == id } ?: return
        selectedItem = option
        binding.autoCompleteTextView.setText(option.title, false)
    }

    /** Borra la selección, el texto y el error. */
    fun clearSelection() {
        selectedItem = null
        binding.autoCompleteTextView.setText("", false)
        hideError()
    }

    /** Cambia el texto gris de dentro del campo. */
    fun setHint(hint: String) {
        binding.autoCompleteTextView.hint = hint
    }

    /** Muestra el error de Material: borde rojo y mensaje debajo del campo. */
    fun showError(message: String) {
        binding.textInputLayout.error = message
    }

    /** Quita el error. */
    fun hideError() {
        binding.textInputLayout.error = null
    }

    /** Bloquea o desbloquea el campo (`isEnabled = false` lo deja gris y sin responder). */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.textInputLayout.isEnabled = enabled
        binding.autoCompleteTextView.isEnabled = enabled
    }

    // --- Estado al rotar ---
    // Los hijos tienen ids fijos (textInputLayout, autoCompleteTextView). Si una pantalla tiene dos de estos
    // widgets, Android mezclaría sus estados. Por eso el widget guarda su propio estado y no el de sus hijos.

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
        dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        dispatchThawSelfOnly(container)
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable(KEY_SUPER, super.onSaveInstanceState())
        selectedItem?.let { putInt(KEY_SELECTED_ID, it.id) }
        binding.textInputLayout.error?.let { putString(KEY_ERROR, it.toString()) }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is Bundle) {
            super.onRestoreInstanceState(state)
            return
        }
        // getParcelable(String) está deprecado en API 33, pero la versión tipada no existe en APIs anteriores
        @Suppress("DEPRECATION")
        super.onRestoreInstanceState(state.getParcelable(KEY_SUPER))
        if (state.containsKey(KEY_SELECTED_ID)) {
            val id = state.getInt(KEY_SELECTED_ID)
            if (items.isEmpty()) pendingSelectedId = id else setSelectedItemById(id)
        }
        state.getString(KEY_ERROR)?.let(::showError)
    }

    /**
     * Adapter que nunca filtra: siempre muestra todas las opciones, aunque el campo ya tenga
     * un texto elegido (si no, al reabrir la lista solo aparecería la opción seleccionada).
     */
    private class NoFilterAdapter(
        context: Context,
        private val options: List<SelectOption>
    ) : ArrayAdapter<SelectOption>(context, android.R.layout.simple_list_item_1, options) {

        private val noFilter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                values = options
                count = options.size
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }

        override fun getFilter(): Filter = noFilter
    }

    private companion object {
        const val KEY_SUPER = "superState"
        const val KEY_SELECTED_ID = "selectedId"
        const val KEY_ERROR = "error"
    }
}

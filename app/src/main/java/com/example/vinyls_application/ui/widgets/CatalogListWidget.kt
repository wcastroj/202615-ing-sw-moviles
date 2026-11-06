package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vinyls_application.R

/**
 * Elemento que muestra cada tarjeta ([CatalogCardWidget] o [CatalogRowWidget]) dentro de un
 * [CatalogListWidget].
 */
data class CatalogItem(
    val id: Int,
    val title: String,
    val coverUrl: String? = null,
    val subtitle: String? = null,
    val detail: String? = null
)

/**
 * Lista (Custom View) de elementos de catálogo con dos modos de presentación:
 *
 * - [Mode.GRID] (por defecto): cuadrícula de 2 columnas de [CatalogCardWidget] (p. ej. álbumes).
 * - [Mode.LIST]: lista vertical de una columna de [CatalogRowWidget] (p. ej. artistas).
 *
 * Extiende de [RecyclerView], por lo que conserva el reciclado de vistas; el adapter y el
 * `LayoutManager` los gestiona el propio widget.
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.CatalogListWidget
 *     android:id="@+id/performersRv"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:catalogMode="list"
 *     tools:listitem="@layout/catalog_row_widget" />
 * ```
 *
 * **Ejemplo de uso en Kotlin:**
 * ```kotlin
 * binding.albumsRv.setOnItemClickListener { item -> goToDetail(item.id) }
 * binding.albumsRv.setItems(albums.map { CatalogItem(it.albumId, it.name, it.cover, it.genre, it.recordLabel) })
 * ```
 */
class CatalogListWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    /** Modo de presentación de la lista. */
    enum class Mode { GRID, LIST }

    private val catalogAdapter = CatalogAdapter()

    /** Modo actual; al cambiarlo se actualiza el `LayoutManager` y el tipo de tarjeta. */
    var mode: Mode = Mode.GRID
        set(value) {
            field = value
            layoutManager = createLayoutManager(value)
            catalogAdapter.mode = value
        }

    init {
        val attrMode = context.obtainStyledAttributes(attrs, R.styleable.CatalogListWidget).run {
            try {
                Mode.values()[getInt(R.styleable.CatalogListWidget_catalogMode, Mode.GRID.ordinal)]
            } finally {
                recycle()
            }
        }
        adapter = catalogAdapter
        mode = attrMode
    }

    /** Reemplaza los elementos mostrados en la lista. */
    fun setItems(items: List<CatalogItem>) {
        catalogAdapter.items = items
    }

    /** Acción al pulsar una tarjeta (o su botón de acción). */
    fun setOnItemClickListener(listener: ((CatalogItem) -> Unit)?) {
        catalogAdapter.onItemClick = listener
    }

    private fun createLayoutManager(mode: Mode): LayoutManager = when (mode) {
        Mode.GRID -> GridLayoutManager(context, DEFAULT_SPAN_COUNT)
        Mode.LIST -> LinearLayoutManager(context)
    }

    private class CatalogAdapter : Adapter<CatalogAdapter.ViewHolder>() {

        /**
         * Guarda la vista y cómo pintarle un [CatalogItem], para no depender de si es una
         * [CatalogCardWidget] o una [CatalogRowWidget].
         */
        class ViewHolder(
            view: View,
            val bind: (item: CatalogItem, onClick: OnClickListener) -> Unit
        ) : RecyclerView.ViewHolder(view)

        var mode: Mode = Mode.GRID
            set(value) {
                if (field == value) return
                field = value
                notifyDataSetChanged()
            }

        var items: List<CatalogItem> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        var onItemClick: ((CatalogItem) -> Unit)? = null

        // El tipo de vista depende del modo, así el pool de reciclado no mezcla tarjetas y filas
        override fun getItemViewType(position: Int): Int = mode.ordinal

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return when (Mode.values()[viewType]) {
                Mode.GRID -> {
                    val card = CatalogCardWidget(parent.context).apply { this.layoutParams = layoutParams }
                    ViewHolder(card) { item, onClick ->
                        card.setContent(
                            title = item.title,
                            coverUrl = item.coverUrl,
                            subtitle = item.subtitle,
                            detail = item.detail,
                            onAction = onClick
                        )
                    }
                }
                Mode.LIST -> {
                    val row = CatalogRowWidget(parent.context).apply { this.layoutParams = layoutParams }
                    ViewHolder(row) { item, onClick ->
                        row.setContent(
                            title = item.title,
                            coverUrl = item.coverUrl,
                            subtitle = item.subtitle,
                            detail = item.detail,
                            onAction = onClick
                        )
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val onClick = OnClickListener { onItemClick?.invoke(item) }
            holder.bind(item, onClick)
            holder.itemView.setOnClickListener(onClick)
            holder.itemView.setTag(R.id.tag_album_id, item.id)
        }

        override fun getItemCount(): Int = items.size
    }

    private companion object {
        const val DEFAULT_SPAN_COUNT = 2
    }
}
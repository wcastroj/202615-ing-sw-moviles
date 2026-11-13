package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vinyls_application.R

/**
 * Elemento que muestra cada [CatalogCardWidget] dentro de un [CatalogListWidget].
 */
data class CatalogItem(
    val id: Int,
    val title: String,
    val coverUrl: String? = null,
    val subtitle: String? = null,
    val detail: String? = null
)

/**
 * Cuadrícula (Custom View) de 2 columnas de [CatalogCardWidget]. Se usa en los catálogos
 * de álbumes y de artistas.
 *
 * Extiende de [RecyclerView], por lo que conserva el reciclado de vistas; el adapter y el
 * `LayoutManager` los gestiona el propio widget.
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.CatalogListWidget
 *     android:id="@+id/albumsRv"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     tools:listitem="@layout/catalog_card_widget"
 *     tools:spanCount="2"
 *     tools:layoutManager="androidx.recyclerview.widget.GridLayoutManager" />
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

    private val catalogAdapter = CatalogAdapter()

    init {
        layoutManager = GridLayoutManager(context, DEFAULT_SPAN_COUNT)
        adapter = catalogAdapter
    }

    /** Reemplaza los elementos mostrados en la cuadrícula. */
    fun setItems(items: List<CatalogItem>) {
        catalogAdapter.items = items
    }

    /** Acción al pulsar una tarjeta (o su botón de acción). */
    fun setOnItemClickListener(listener: ((CatalogItem) -> Unit)?) {
        catalogAdapter.onItemClick = listener
    }

    private class CatalogAdapter : Adapter<CatalogAdapter.ViewHolder>() {

        class ViewHolder(val card: CatalogCardWidget) : RecyclerView.ViewHolder(card)

        var items: List<CatalogItem> = emptyList()
            set(value) {
                val diffCallback = object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int = field.size
                    override fun getNewListSize(): Int = value.size
                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        field[oldItemPosition].id == value[newItemPosition].id
                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                        field[oldItemPosition] == value[newItemPosition]
                }
                val diffResult = DiffUtil.calculateDiff(diffCallback)
                field = value
                diffResult.dispatchUpdatesTo(this)
            }

        var onItemClick: ((CatalogItem) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = CatalogCardWidget(parent.context).apply {
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
            }
            return ViewHolder(card)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val onClick = OnClickListener { onItemClick?.invoke(item) }
            holder.card.setContent(
                title = item.title,
                coverUrl = item.coverUrl,
                subtitle = item.subtitle,
                detail = item.detail,
                onAction = onClick
            )
            holder.itemView.setOnClickListener(onClick)
            holder.itemView.setTag(R.id.tag_album_id, item.id)
        }

        override fun getItemCount(): Int = items.size
    }

    private companion object {
        const val DEFAULT_SPAN_COUNT = 2
    }
}

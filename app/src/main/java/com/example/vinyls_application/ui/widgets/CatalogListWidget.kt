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
 */
data class CatalogItem(
    val id: Int,
    val title: String,
    val coverUrl: String? = null,
    val subtitle: String? = null,
    val detail: String? = null
)

/**
 *
 * Extiende de [RecyclerView], por lo que conserva el reciclado de vistas; el adapter y el
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.CatalogListWidget
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
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


        var items: List<CatalogItem> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        var onItemClick: ((CatalogItem) -> Unit)? = null

        // El tipo de vista depende del modo, así el pool de reciclado no mezcla tarjetas y filas
        override fun getItemViewType(position: Int): Int = mode.ordinal

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
                    }
                }
                            title = item.title,
                            coverUrl = item.coverUrl,
                            subtitle = item.subtitle,
                            detail = item.detail,
                            onAction = onClick
                        )
            holder.itemView.setTag(R.id.tag_album_id, item.id)
        }

        override fun getItemCount(): Int = items.size
    }

    private companion object {
        const val DEFAULT_SPAN_COUNT = 2
    }
}

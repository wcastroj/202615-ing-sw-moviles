package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
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

    /** Reemplaza los elementos mostrados en la cuadrícula usando DiffUtil de ListAdapter. */
    fun setItems(items: List<CatalogItem>) {
        catalogAdapter.submitList(items)
    }

    /** Acción al pulsar una tarjeta (o su botón de acción). */
    fun setOnItemClickListener(listener: ((CatalogItem) -> Unit)?) {
        catalogAdapter.onItemClick = listener
    }

    private class CatalogAdapter : ListAdapter<CatalogItem, CatalogAdapter.ViewHolder>(
        AsyncDifferConfig.Builder(CatalogItemDiffCallback)
            .setBackgroundThreadExecutor { it.run() }
            .build()
    ) {

        class ViewHolder(val card: CatalogCardWidget) : RecyclerView.ViewHolder(card) {
            var clickListener: OnClickListener? = null
        }

        var items: List<CatalogItem>
            get() = currentList
            set(value) {
                submitList(value)
            }

        var onItemClick: ((CatalogItem) -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val card = CatalogCardWidget(parent.context).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val holder = ViewHolder(card)
            val listener = OnClickListener {
                val pos = holder.adapterPosition
                if (pos != NO_POSITION && pos < currentList.size) {
                    onItemClick?.invoke(getItem(pos))
                }
            }
            holder.clickListener = listener
            holder.itemView.setOnClickListener(listener)
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.card.setContent(
                title = item.title,
                coverUrl = item.coverUrl,
                subtitle = item.subtitle,
                detail = item.detail,
                onAction = if (onItemClick != null) holder.clickListener else null
            )
            holder.itemView.setTag(R.id.tag_album_id, item.id)
        }

        override fun onViewRecycled(holder: ViewHolder) {
            super.onViewRecycled(holder)
            holder.card.clearCover()
        }
    }

    private object CatalogItemDiffCallback : DiffUtil.ItemCallback<CatalogItem>() {
        override fun areItemsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean =
            oldItem == newItem
    }

    private companion object {
        const val DEFAULT_SPAN_COUNT = 2
    }
}

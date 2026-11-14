package com.example.vinyls_application.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.ui.widgets.SummaryCardWidget

class RelatedAlbumsAdapter : ListAdapter<Album, RelatedAlbumsAdapter.AlbumViewHolder>(AlbumDiffCallback) {

    var albums: List<Album>
        get() = currentList
        set(value) {
            submitList(value)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val widget = SummaryCardWidget(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return AlbumViewHolder(widget)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = getItem(position)
        holder.widget.setContent(
            id = album.albumId.toString(),
            title = album.name,
            subtitle = if (album.releaseDate.length >= 10) album.releaseDate.substring(0, 10) else album.releaseDate
        )
    }

    override fun onViewRecycled(holder: AlbumViewHolder) {
        super.onViewRecycled(holder)
    }

    class AlbumViewHolder(val widget: SummaryCardWidget) : RecyclerView.ViewHolder(widget)

    private object AlbumDiffCallback : DiffUtil.ItemCallback<Album>() {
        override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean =
            oldItem.albumId == newItem.albumId

        override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean =
            oldItem == newItem
    }
}

package com.example.vinyls_application.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.ui.widgets.SummaryCardWidget

class RelatedAlbumsAdapter : RecyclerView.Adapter<RelatedAlbumsAdapter.AlbumViewHolder>() {

    var albums: List<Album> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
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
        val album = albums[position]
        holder.widget.setContent(
            id = album.albumId.toString(),
            title = album.name,
            subtitle = album.releaseDate.take(10)
        )
    }

    override fun getItemCount(): Int = albums.size

    class AlbumViewHolder(val widget: SummaryCardWidget) : RecyclerView.ViewHolder(widget)
}
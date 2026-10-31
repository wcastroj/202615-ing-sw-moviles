package com.example.vinyls_application.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.CatalogCardWidgetBinding
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.ui.AlbumFragmentDirections

class AlbumsAdapter : RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder>(){
    class AlbumViewHolder(val viewDataBinding: CatalogCardWidgetBinding) :
        RecyclerView.ViewHolder(viewDataBinding.root) {
        companion object {
            @LayoutRes
            val LAYOUT = R.layout.catalog_card_widget
        }
    }
    var Albums :List<Album> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val withDataBinding: CatalogCardWidgetBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            AlbumViewHolder.LAYOUT,
            parent,
            false)
        return AlbumViewHolder(withDataBinding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = Albums[position]
        val goToDetail = View.OnClickListener { view ->
            val action = AlbumFragmentDirections.actionAlbumFragmentToDetailFragment(album.albumId)
            // Navigate using that action
            view.findNavController().navigate(action)
        }
        holder.viewDataBinding.also {
            it.coverUrl = album.cover
            it.title = album.name
            it.subtitle = album.genre
            it.detail = album.recordLabel
            it.onAction = goToDetail
        }
        holder.viewDataBinding.root.setOnClickListener(goToDetail)
    }

    override fun getItemCount(): Int {
        return Albums.size
    }


}



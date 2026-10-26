package com.example.vinyls_application.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AlbumItemBinding
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.ui.AlbumFragmentDirections

class AlbumsAdapter : RecyclerView.Adapter<AlbumsAdapter.AlbumViewHolder>(){
    class AlbumViewHolder(val viewDataBinding: AlbumItemBinding) :
        RecyclerView.ViewHolder(viewDataBinding.root) {
        companion object {
            @LayoutRes
            val LAYOUT = R.layout.album_item
        }
    }
    var Albums :List<Album> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val withDataBinding: AlbumItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            AlbumViewHolder.LAYOUT,
            parent,
            false)
        return AlbumViewHolder(withDataBinding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.viewDataBinding.also {
            it.album = Albums[position]
        }
        holder.viewDataBinding.root.setOnClickListener {
            val action = AlbumFragmentDirections.actionAlbumFragmentToCommentFragment(Albums[position].albumId)
            // Navigate using that action
            holder.viewDataBinding.root.findNavController().navigate(action)
        }
    }

    override fun getItemCount(): Int {
        return Albums.size
    }


}



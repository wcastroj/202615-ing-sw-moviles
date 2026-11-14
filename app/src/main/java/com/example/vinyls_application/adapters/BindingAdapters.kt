package com.example.vinyls_application.adapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.vinyls_application.utils.MemoryUtils

@BindingAdapter("imageUrl")
fun bindImage(imgView: ImageView, imgUrl: String?) {
    if (imgUrl.isNullOrBlank()) {
        Glide.with(imgView.context).clear(imgView)
        imgView.setImageDrawable(null)
    } else {
        val format = MemoryUtils.getPreferredDecodeFormat(imgView.context)
        Glide.with(imgView.context)
            .load(imgUrl)
            .format(format)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imgView)
    }
}

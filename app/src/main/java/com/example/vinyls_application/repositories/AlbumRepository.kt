package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.VolleyError
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.network.NetworkServiceAdapter

class AlbumRepository(val application: Application) {
    fun getAlbums(callback: (List<Album>) -> Unit, onError: (VolleyError) -> Unit) {
        NetworkServiceAdapter.getInstance(application).getAlbums(
            {
                callback(it)
            }, {
                onError(it)
            })
    }
}
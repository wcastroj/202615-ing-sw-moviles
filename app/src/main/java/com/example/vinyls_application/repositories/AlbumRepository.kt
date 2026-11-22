package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.VolleyError
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.network.NetworkServiceAdapter

class AlbumRepository(val application: Application) {

    /**
     * Devuelve los álbumes ya parseados. El adapter parsea fuera del hilo principal.
     * Si la petición falla lanza la [VolleyError], que el ViewModel atrapa.
     */
    suspend fun getAlbums(): List<Album> =
        NetworkServiceAdapter.getInstance(application).getAlbums()
}

package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.VolleyError
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.network.NetworkServiceAdapter


class AlbumDetailRepository (val application: Application) {

    /** Devuelve el detalle del álbum o lanza la [VolleyError] si la petición falla. */
    suspend fun refreshData(id: Int): AlbumDetail =
        NetworkServiceAdapter.getInstance(application).getAlbumDetail(id)

    /**
     * Publica el comentario. Si la función termina sin lanzar, el POST fue exitoso.
     * El JSON de respuesta no se usa.
     */
    suspend fun postComment(albumId: Int, comment: String, rating: Int) {
        NetworkServiceAdapter.getInstance(application).postComment(albumId, comment, rating)
    }
}

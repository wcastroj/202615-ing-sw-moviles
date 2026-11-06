package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.VolleyError
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.network.NetworkServiceAdapter

class PerformerRepository(val application: Application) {

    /**
     * Entrega en una sola lista las bandas (GET /bands) y los músicos (GET /musicians),
     * ordenada por nombre.
     *
     * Las peticiones se encadenan: primero bandas y, cuando responden, músicos. Si cualquiera
     * de las dos falla se llama a [onError] y nunca se entrega una lista a medias.
     */
    fun getPerformers(callback: (List<Performer>) -> Unit, onError: (VolleyError) -> Unit) {
        val adapter = NetworkServiceAdapter.getInstance(application)
        adapter.getBands({ bands ->
            adapter.getMusicians({ musicians ->
                callback((bands + musicians).sortedBy { it.name.lowercase() })
            }, onError)
        }, onError)
    }
}

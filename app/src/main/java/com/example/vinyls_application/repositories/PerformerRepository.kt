package com.example.vinyls_application.repositories

import android.app.Application
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.network.NetworkServiceAdapter
import kotlinx.coroutines.withContext

class PerformerRepository(val application: Application) {

    /**
     * Devuelve en una sola lista las bandas (GET /bands) y los músicos (GET /musicians),
     * ordenada por nombre.
     *
     * Las peticiones son secuenciales: primero bandas y después músicos. Si cualquiera falla,
     * la excepción sube al llamador y nunca se entrega una lista a medias. Si /bands falla,
     * ni siquiera se piden los músicos.
     */
    suspend fun getPerformers(): List<Performer> {
        val adapter = NetworkServiceAdapter.getInstance(application)
        val bands = adapter.getBands()
        val musicians = adapter.getMusicians()
        // Unir y ordenar es trabajo de CPU: se hace fuera del hilo principal, en el mismo
        // dispatcher que usa el adapter para parsear (así las pruebas solo lo reemplazan ahí)
        return withContext(adapter.dispatcher) {
            (bands + musicians).sortedBy { it.name.lowercase() }
        }
    }
}

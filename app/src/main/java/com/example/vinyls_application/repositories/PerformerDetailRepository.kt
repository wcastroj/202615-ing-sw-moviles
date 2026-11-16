package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.VolleyError
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter

class PerformerDetailRepository(val application: Application) {

    /** Devuelve la banda o el músico, o lanza la [VolleyError] si la petición falla. */
    suspend fun getPerformer(id: Int, type: PerformerType): Performer =
        NetworkServiceAdapter.getInstance(application).getPerformer(id, type)
}

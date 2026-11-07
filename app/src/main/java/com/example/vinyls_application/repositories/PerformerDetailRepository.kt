package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.VolleyError
import com.example.vinyls_application.models.PerformerBase
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter

class PerformerDetailRepository(val application: Application) {
    fun getPerformer(
        id: Int,
        type: PerformerType,
        callback: (PerformerBase) -> Unit,
        onError: (VolleyError) -> Unit
    ) {
        NetworkServiceAdapter.getInstance(application).getPerformer(
            id, type,
            { callback(it) },
            { onError(it) })
    }
}
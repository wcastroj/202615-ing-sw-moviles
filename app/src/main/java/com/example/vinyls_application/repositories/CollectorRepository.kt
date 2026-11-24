package com.example.vinyls_application.repositories

import android.app.Application
import com.example.vinyls_application.models.Collector
import com.example.vinyls_application.network.NetworkServiceAdapter

class CollectorRepository(val application: Application) {

    suspend fun getCollectors(): List<Collector> =
        NetworkServiceAdapter.getInstance(application).getCollectors()
}

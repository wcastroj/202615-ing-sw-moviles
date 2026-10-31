package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.VolleyError
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.network.NetworkServiceAdapter


class AlbumDetailRepository (val application: Application) {
    fun refreshData(id:Int,callback:(AlbumDetail)-> Unit, onError:(VolleyError)-> Unit){
        NetworkServiceAdapter.getInstance(application).getAlbumDetail(id,
            {
            callback(it)
        },{
            onError(it)
        })
    }
}
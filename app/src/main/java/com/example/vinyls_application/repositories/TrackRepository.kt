package com.example.vinyls_application.repositories

import android.app.Application
import com.example.vinyls_application.models.Track
import com.example.vinyls_application.network.NetworkServiceAdapter

class TrackRepository(val application: Application) {

    /** Si la función termina sin lanzar, la canción se asoció al álbum. */
    suspend fun associateTrack(albumId: Int, track: Track) {
        NetworkServiceAdapter.getInstance(application).associateTrack(albumId, track)
    }
}

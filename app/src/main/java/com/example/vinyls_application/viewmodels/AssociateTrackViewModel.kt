package com.example.vinyls_application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinyls_application.models.Track
import com.example.vinyls_application.repositories.TrackRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Es un estado y no un evento: al rotar la pantalla se vuelve a dibujar sin repetir acciones. */
enum class AssociateTrackState { EDITING, SAVING, SAVE_FAILED, ASSOCIATED }

class AssociateTrackViewModel(
    application: Application,
    private val albumId: Int,
    private val trackRepository: TrackRepository
) : AndroidViewModel(application) {

    private val _state = MutableLiveData(AssociateTrackState.EDITING)
    val state: LiveData<AssociateTrackState>
        get() = _state

    /**
     * Asocia la canción al álbum.
     *
     * @return false si ya se está guardando; así un doble toque no crea la canción dos veces.
     */
    fun associate(track: Track): Boolean {
        if (_state.value != AssociateTrackState.EDITING) return false

        _state.value = AssociateTrackState.SAVING
        viewModelScope.launch {
            try {
                trackRepository.associateTrack(albumId, track)
                _state.value = AssociateTrackState.ASSOCIATED
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AssociateTrackState.SAVE_FAILED
            }
        }
        return true
    }

    /** Al cerrar la pantalla de error se vuelve al formulario con los datos que ya tenía. */
    fun onSaveErrorDismissed() {
        if (_state.value == AssociateTrackState.SAVE_FAILED) {
            _state.value = AssociateTrackState.EDITING
        }
    }

    class Factory(val app: Application, val albumId: Int) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AssociateTrackViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AssociateTrackViewModel(app, albumId, TrackRepository(app)) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}

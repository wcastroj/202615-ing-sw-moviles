package com.example.vinyls_application.viewmodels
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.repositories.AlbumRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AlbumViewModel(application: Application, private val albumsRepository: AlbumRepository) :  AndroidViewModel(application) {

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>>
        get() = _albums
    private var _eventNetworkError = MutableLiveData(false)

    val eventNetworkError: LiveData<Boolean>
        get() = _eventNetworkError

    private var _isNetworkErrorShown = MutableLiveData(false)

    val isNetworkErrorShown: LiveData<Boolean>
        get() = _isNetworkErrorShown

    init {
        getAlbumsFromNetwork()
    }

    /**
     * viewModelScope corre en el hilo principal y se cancela solo cuando se destruye el ViewModel.
     * La espera de red y el parseo ocurren fuera del hilo principal (en el adapter), así que aquí
     * solo se actualizan los LiveData con .value.
     */
    private fun getAlbumsFromNetwork() {
        viewModelScope.launch {
            try {
                _albums.value = albumsRepository.getAlbums()
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false
            } catch (e: CancellationException) {
                throw e // la cancelación no es un error de red: se deja propagar
            } catch (e: Exception) {
                _eventNetworkError.value = true
            }
        }
    }

    fun onNetworkErrorShown() {
        _isNetworkErrorShown.value = true
    }

    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlbumViewModel(app, AlbumRepository(app)) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}

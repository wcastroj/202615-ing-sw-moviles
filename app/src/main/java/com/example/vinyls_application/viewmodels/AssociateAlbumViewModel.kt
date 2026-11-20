package com.example.vinyls_application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.repositories.AlbumRepository
import com.example.vinyls_application.repositories.PerformerDetailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssociateAlbumViewModel(
    application: Application,
    private val performerId: Int,
    private val performerType: PerformerType
) : AndroidViewModel(application) {

    private val performerRepository = PerformerDetailRepository(application)
    private val albumRepository = AlbumRepository(application)

    private val _availableAlbums = MutableLiveData<List<Album>>()
    val availableAlbums: LiveData<List<Album>> get() = _availableAlbums

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _eventNetworkError = MutableLiveData<Boolean>(false)
    val eventNetworkError: LiveData<Boolean> get() = _eventNetworkError

    private val _associationSuccess = MutableLiveData<Boolean>(false)
    val associationSuccess: LiveData<Boolean> get() = _associationSuccess

    private val _isNetworkErrorShown = MutableLiveData<Boolean>(false)
    val isNetworkErrorShown: LiveData<Boolean> get() = _isNetworkErrorShown

    init {
        loadAvailableAlbums()
    }

    fun loadAvailableAlbums() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val performer = performerRepository.getPerformer(performerId, performerType)
                val allAlbums = albumRepository.getAlbums()

                val associatedIds = HashSet<Int>(performer.albums.size)
                for (i in 0 until performer.albums.size) {
                    associatedIds.add(performer.albums[i].albumId)
                }

                val filteredAlbums = withContext(Dispatchers.Default) {
                    val result = ArrayList<Album>()
                    for (i in 0 until allAlbums.size) {
                        val album = allAlbums[i]
                        if (album.albumId !in associatedIds) {
                            result.add(album)
                        }
                    }
                    result
                }

                _availableAlbums.value = filteredAlbums
                _isLoading.value = false
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _eventNetworkError.value = true
            }
        }
    }

    fun associateAlbum(albumId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                performerRepository.addAlbumToPerformer(performerId, albumId, performerType)
                _isLoading.value = false
                _associationSuccess.value = true
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _eventNetworkError.value = true
            }
        }
    }

    fun onAssociationSuccessHandled() {
        _associationSuccess.value = false
    }

    fun onNetworkErrorShown() {
        _isNetworkErrorShown.value = true
    }

    fun retry() {
        _isNetworkErrorShown.value = false
        loadAvailableAlbums()
    }

    class Factory(
        val app: Application,
        val performerId: Int,
        val performerType: PerformerType
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AssociateAlbumViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AssociateAlbumViewModel(app, performerId, performerType) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}

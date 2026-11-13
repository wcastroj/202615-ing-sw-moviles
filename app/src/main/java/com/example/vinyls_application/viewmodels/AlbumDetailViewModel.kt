package com.example.vinyls_application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.repositories.AlbumDetailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AlbumDetailViewModel(application: Application, private val albumId: Int) : AndroidViewModel(application){

    private val _album = MutableLiveData<AlbumDetail>()
    val album : LiveData<AlbumDetail>
        get() = _album
    private var _eventNetworkError = MutableLiveData<Boolean>(false)

    val eventNetworkError: LiveData<Boolean>
        get() = _eventNetworkError

    private var _isNetworkErrorShown = MutableLiveData<Boolean>(false)
    private val albumRepository = AlbumDetailRepository(application)

    val isNetworkErrorShown: LiveData<Boolean>
        get() = _isNetworkErrorShown

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _commentPostedSuccess = MutableLiveData<Boolean>(false)
    val commentPostedSuccess: LiveData<Boolean>
        get() = _commentPostedSuccess

    init {
        refreshDataFromNetwork()
    }

    fun refreshDataFromNetwork() {
        viewModelScope.launch {
            try {
                _album.value = albumRepository.refreshData(albumId)
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _eventNetworkError.value = true
            }
        }
    }

    fun addComment(comment: String, rating: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                albumRepository.postComment(albumId, comment, rating)
                // Mismo orden que antes: primero se reactiva el formulario y luego se avisa el éxito
                _isLoading.value = false
                _commentPostedSuccess.value = true
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false
                refreshDataFromNetwork()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _eventNetworkError.value = true
            }
        }
    }

    fun onCommentPostedSuccessHandled() {
        _commentPostedSuccess.value = false
    }

    fun onNetworkErrorShown() {
        _isNetworkErrorShown.value = true
    }

    // Se reinicia isNetworkErrorShown para que el error se vuelva a mostrar si el reintento falla
    fun retry() {
        _isNetworkErrorShown.value = false
        refreshDataFromNetwork()
    }
    class Factory(val app: Application, val albumId: Int) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlbumDetailViewModel(app,albumId) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}

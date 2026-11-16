package com.example.vinyls_application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.repositories.PerformerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


class PerformerViewModel(application: Application, private val performerRepository: PerformerRepository) :  AndroidViewModel(application) {

    private val _performers = MutableLiveData<List<Performer>>()
    val performers: LiveData<List<Performer>>
        get() = _performers
    private var _eventNetworkError = MutableLiveData(false)

    val eventNetworkError: LiveData<Boolean>
        get() = _eventNetworkError

    private var _isNetworkErrorShown = MutableLiveData(false)

    val isNetworkErrorShown: LiveData<Boolean>
        get() = _isNetworkErrorShown

    init {
        getPerformersFromNetwork()
    }
    /** Trae bandas y músicos ya unidos en una sola lista desde el repositorio. */
    private fun getPerformersFromNetwork() {
        viewModelScope.launch {
            try {
                _performers.value = performerRepository.getPerformers()
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false
            } catch (e: CancellationException) {
                throw e
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
            if (modelClass.isAssignableFrom(PerformerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PerformerViewModel(app, PerformerRepository(app)) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}

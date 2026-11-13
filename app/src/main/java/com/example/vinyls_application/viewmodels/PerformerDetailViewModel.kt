package com.example.vinyls_application.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinyls_application.models.PerformerBase
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.repositories.PerformerDetailRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PerformerDetailViewModel(
    application: Application,
    private val id: Int,
    private val type: PerformerType,
    private val repository: PerformerDetailRepository = PerformerDetailRepository(application)
) : AndroidViewModel(application) {

    private val _performer = MutableLiveData<PerformerBase>()
    val performer: LiveData<PerformerBase> get() = _performer

    private val _eventNetworkError = MutableLiveData(false)
    val eventNetworkError: LiveData<Boolean> get() = _eventNetworkError

    private val _isNetworkErrorShown = MutableLiveData(false)
    val isNetworkErrorShown: LiveData<Boolean> get() = _isNetworkErrorShown

    init {
        getPerformerDetail()
    }

    fun getPerformerDetail() {
        viewModelScope.launch {
            try {
                _performer.value = repository.getPerformer(id, type)
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

    class Factory(
        val app: Application,
        val id: Int,
        val type: PerformerType
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PerformerDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PerformerDetailViewModel(app, id, type) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}

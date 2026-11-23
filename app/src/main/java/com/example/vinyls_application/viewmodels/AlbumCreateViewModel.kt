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
import com.example.vinyls_application.utils.AlbumFormErrors
import com.example.vinyls_application.utils.AlbumFormValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Es un estado y no un evento: al rotar la pantalla se vuelve a dibujar sin repetir acciones. */
enum class AlbumCreateState { EDITING, SAVING, SAVE_FAILED, CREATED }

/**
 * Los valores del formulario viven en sus campos, que Android ya conserva al rotar. Aquí solo se
 * guardan los errores y el estado, y el texto se lee una sola vez al guardar.
 */
class AlbumCreateViewModel(
    application: Application,
    private val albumRepository: AlbumRepository
) : AndroidViewModel(application) {

    // Géneros y sellos son enums fijos: el formulario está listo sin consultar el backend
    private val _state = MutableLiveData(AlbumCreateState.EDITING)
    val state: LiveData<AlbumCreateState>
        get() = _state

    private val _errors = MutableLiveData(AlbumFormErrors())
    val errors: LiveData<AlbumFormErrors>
        get() = _errors

    private val currentErrors: AlbumFormErrors
        get() = _errors.value ?: AlbumFormErrors()

    fun onCoverChanged(value: CharSequence) {
        val error = AlbumFormValidator.validateCover(value)
        if (currentErrors.cover != error) _errors.value = currentErrors.copy(cover = error)
    }

    fun onNameChanged(value: CharSequence) {
        val error = AlbumFormValidator.validateName(value)
        if (currentErrors.name != error) _errors.value = currentErrors.copy(name = error)
    }

    fun onRecordLabelChanged(value: CharSequence) {
        val error = AlbumFormValidator.validateRequired(value)
        if (currentErrors.recordLabel != error) _errors.value = currentErrors.copy(recordLabel = error)
    }

    fun onGenreChanged(value: CharSequence) {
        val error = AlbumFormValidator.validateRequired(value)
        if (currentErrors.genre != error) _errors.value = currentErrors.copy(genre = error)
    }

    fun onReleaseDateChanged(value: CharSequence) {
        val error = AlbumFormValidator.validateRequired(value)
        if (currentErrors.releaseDate != error) _errors.value = currentErrors.copy(releaseDate = error)
    }

    fun onDescriptionChanged(value: CharSequence) {
        val error = AlbumFormValidator.validateDescription(value)
        if (currentErrors.description != error) _errors.value = currentErrors.copy(description = error)
    }

    /**
     * Valida todo el formulario y, si es correcto, crea el álbum.
     *
     * @return false si hay errores o si ya se está guardando; así un doble toque no crea dos álbumes.
     */
    fun save(album: Album): Boolean {
        if (_state.value != AlbumCreateState.EDITING) return false

        val errors = AlbumFormValidator.validate(album)
        if (errors != currentErrors) _errors.value = errors
        if (errors.hasErrors) return false

        _state.value = AlbumCreateState.SAVING
        viewModelScope.launch {
            try {
                albumRepository.createAlbum(album)
                _state.value = AlbumCreateState.CREATED
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = AlbumCreateState.SAVE_FAILED
            }
        }
        return true
    }

    fun onSaveErrorDismissed() {
        if (_state.value == AlbumCreateState.SAVE_FAILED) {
            _state.value = AlbumCreateState.EDITING
        }
    }

    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumCreateViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlbumCreateViewModel(app, AlbumRepository(app)) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}

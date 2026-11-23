package com.example.vinyls_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AlbumCreateFragmentBinding
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.models.Genre
import com.example.vinyls_application.models.RecordLabel
import com.example.vinyls_application.utils.AlbumDateFormatter
import com.example.vinyls_application.utils.AlbumFieldError
import com.example.vinyls_application.utils.AlbumFormErrors
import com.example.vinyls_application.viewmodels.AlbumCreateState
import com.example.vinyls_application.viewmodels.AlbumCreateViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout

/**
 * Formulario para crear un álbum (HU05). Solo llega aquí el coleccionista.
 *
 * No muestra la portada a propósito: así el formulario no carga ningún bitmap en memoria.
 */
class AlbumCreateFragment : Fragment() {

    private var _binding: AlbumCreateFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlbumCreateViewModel

    /** Últimos errores dibujados, para tocar solo los campos cuyo error cambió. */
    private var renderedErrors: AlbumFormErrors? = null

    /** Mientras se guarda no se puede salir: el álbum podría crearse sin que la lista se entere. */
    private val blockBackWhileSaving = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AlbumCreateFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AlbumCreateViewModel.Factory(requireActivity().application)
        )[AlbumCreateViewModel::class.java]

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockBackWhileSaving)

        binding.successInformationWidget.setContent(
            title = getString(R.string.album_create_success_title),
            description = getString(R.string.album_create_success_description),
            iconRes = R.drawable.ic_album_created,
            buttonText = getString(R.string.album_create_success_button),
            onButtonClick = ::returnToAlbums
        )

        binding.btnSaveAlbum.setOnClick(::saveAlbum)
        binding.etReleaseDate.setOnClickListener { showDatePicker() }
        findDatePicker()?.let(::listenToDatePicker) // Tras rotar, el selector sigue abierto

        viewModel.errors.observe(viewLifecycleOwner, ::showErrors)
        viewModel.state.observe(viewLifecycleOwner, ::showState)
    }

    /**
     * Android restaura el texto de los campos justo antes de este método. Si se escucharan desde
     * antes, al rotar la pantalla los campos vacíos sin tocar mostrarían "obligatorio".
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Las listas se asignan después de restaurar el texto; si no, quedarían filtradas por él
        binding.actvGenre.setAdapter(optionsAdapter(Genre.apiValues))
        binding.actvRecordLabel.setAdapter(optionsAdapter(RecordLabel.apiValues))
        binding.etCover.doAfterTextChanged { viewModel.onCoverChanged(it ?: "") }
        binding.etName.doAfterTextChanged { viewModel.onNameChanged(it ?: "") }
        binding.actvRecordLabel.doAfterTextChanged { viewModel.onRecordLabelChanged(it ?: "") }
        binding.actvGenre.doAfterTextChanged { viewModel.onGenreChanged(it ?: "") }
        binding.etReleaseDate.doAfterTextChanged { viewModel.onReleaseDateChanged(it ?: "") }
        binding.etDescription.doAfterTextChanged { viewModel.onDescriptionChanged(it ?: "") }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        findDatePicker()?.clearOnPositiveButtonClickListeners()
        renderedErrors = null
        _binding = null
    }

    private fun optionsAdapter(values: List<String>) =
        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, values)

    private fun showErrors(errors: AlbumFormErrors) {
        val previous = renderedErrors
        if (previous?.cover != errors.cover) binding.tilCover.showError(errors.cover)
        if (previous?.name != errors.name) binding.tilName.showError(errors.name)
        if (previous?.recordLabel != errors.recordLabel) binding.tilRecordLabel.showError(errors.recordLabel)
        if (previous?.genre != errors.genre) binding.tilGenre.showError(errors.genre)
        if (previous?.releaseDate != errors.releaseDate) binding.tilReleaseDate.showError(errors.releaseDate)
        if (previous?.description != errors.description) binding.tilDescription.showError(errors.description)
        renderedErrors = errors
    }

    private fun TextInputLayout.showError(fieldError: AlbumFieldError) {
        error = errorMessageRes(fieldError)?.let(::getString)
    }

    @StringRes
    private fun errorMessageRes(error: AlbumFieldError): Int? = when (error) {
        AlbumFieldError.NONE -> null
        AlbumFieldError.REQUIRED -> R.string.album_create_error_required
        AlbumFieldError.INVALID_COVER -> R.string.album_create_error_cover
        AlbumFieldError.NAME_TOO_LONG -> R.string.album_create_error_name_length
        AlbumFieldError.DESCRIPTION_TOO_LONG -> R.string.album_create_error_description_length
    }

    private fun showState(state: AlbumCreateState) {
        binding.layoutContent.isVisible =
            state == AlbumCreateState.EDITING || state == AlbumCreateState.SAVING
        binding.progressSaving.isVisible = state == AlbumCreateState.SAVING
        binding.successInformationWidget.isVisible = state == AlbumCreateState.CREATED
        binding.errorInformationWidget.isVisible = state == AlbumCreateState.SAVE_FAILED

        setFormEnabled(state == AlbumCreateState.EDITING)
        blockBackWhileSaving.isEnabled = state == AlbumCreateState.SAVING

        when (state) {
            AlbumCreateState.SAVE_FAILED -> showErrorScreen(
                R.string.album_create_save_error_description,
                R.string.album_create_back_to_form,
                viewModel::onSaveErrorDismissed
            )
            // Se avisa aquí para que la lista se recargue sin importar cómo se salga de esta pantalla
            AlbumCreateState.CREATED -> findNavController().previousBackStackEntry
                ?.savedStateHandle?.set(KEY_ALBUM_CREATED, true)
            else -> Unit
        }
    }

    private fun showErrorScreen(
        @StringRes descriptionRes: Int,
        @StringRes buttonRes: Int,
        onButtonClick: () -> Unit
    ) {
        binding.errorInformationWidget.setContent(
            title = getString(R.string.album_create_error_title),
            description = getString(descriptionRes),
            iconRes = R.drawable.ic_signal_off,
            buttonText = getString(buttonRes),
            onButtonClick = onButtonClick
        )
    }

    private fun setFormEnabled(isEnabled: Boolean) {
        binding.tilCover.isEnabled = isEnabled
        binding.tilName.isEnabled = isEnabled
        binding.tilRecordLabel.isEnabled = isEnabled
        binding.tilGenre.isEnabled = isEnabled
        binding.tilReleaseDate.isEnabled = isEnabled
        binding.tilDescription.isEnabled = isEnabled
        binding.btnSaveAlbum.isEnabled = isEnabled
    }

    private fun saveAlbum() {
        hideKeyboard()
        val album = Album(
            name = binding.etName.trimmedText(),
            cover = binding.etCover.trimmedText(),
            releaseDate = AlbumDateFormatter.toIso(binding.etReleaseDate.trimmedText()),
            description = binding.etDescription.trimmedText(),
            genre = binding.actvGenre.trimmedText(),
            recordLabel = binding.actvRecordLabel.trimmedText()
        )
        if (!viewModel.save(album)) scrollToFirstError()
    }

    private fun TextView.trimmedText() = text.toString().trim()

    /** "Guardar" está al final del formulario: sin esto el error podría quedar fuera de la vista. */
    private fun scrollToFirstError() {
        val errors = viewModel.errors.value ?: return
        val label = when {
            errors.cover != AlbumFieldError.NONE -> binding.labelCover
            errors.name != AlbumFieldError.NONE -> binding.labelName
            errors.recordLabel != AlbumFieldError.NONE -> binding.labelRecordLabel
            errors.genre != AlbumFieldError.NONE -> binding.labelGenre
            errors.releaseDate != AlbumFieldError.NONE -> binding.labelReleaseDate
            errors.description != AlbumFieldError.NONE -> binding.labelDescription
            else -> return
        }
        binding.layoutContent.smoothScrollTo(0, label.top)
    }

    private fun hideKeyboard() {
        ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun showDatePicker() {
        if (findDatePicker() != null) return // Evita abrirlo dos veces con un doble toque

        val selection = AlbumDateFormatter.toMillis(binding.etReleaseDate.trimmedText())
            ?: MaterialDatePicker.todayInUtcMilliseconds()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.album_create_release_date_picker_title)
            .setTheme(R.style.ThemeOverlay_Vinyls_DatePicker)
            .setSelection(selection)
            .build()
        listenToDatePicker(picker)
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    private fun listenToDatePicker(picker: MaterialDatePicker<Long>) {
        picker.addOnPositiveButtonClickListener { utcMillis ->
            binding.etReleaseDate.setText(AlbumDateFormatter.toDisplay(utcMillis))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun findDatePicker() =
        childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<Long>

    private fun returnToAlbums() {
        findNavController().popBackStack()
    }

    companion object {
        /** Clave con la que la lista de álbumes se entera de que debe recargarse. */
        const val KEY_ALBUM_CREATED = "album_created"
        private const val DATE_PICKER_TAG = "album_release_date_picker"
    }
}

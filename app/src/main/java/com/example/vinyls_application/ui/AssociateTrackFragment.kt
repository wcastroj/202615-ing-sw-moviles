package com.example.vinyls_application.ui

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AssociateTrackFragmentBinding
import com.example.vinyls_application.models.Track
import com.example.vinyls_application.utils.TrackFieldError
import com.example.vinyls_application.viewmodels.AssociateTrackState
import com.example.vinyls_application.viewmodels.AssociateTrackViewModel

class AssociateTrackFragment : Fragment(), ComponentCallbacks2 {

    private var _binding: AssociateTrackFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AssociateTrackViewModel

    /** Álbum al que se asociará la canción (llega desde el detalle del álbum). */
    private val args: AssociateTrackFragmentArgs by navArgs()

    /** Mientras se envía la canción no se puede salir (ni con el header ni con el atrás del sistema). */
    private val blockBackWhileSaving = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = Unit
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AssociateTrackFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AssociateTrackViewModel.Factory(requireActivity().application, args.albumId)
        )[AssociateTrackViewModel::class.java]

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockBackWhileSaving)

        binding.successInformationWidget.setContent(
            title = getString(R.string.associate_track_success_title),
            description = getString(R.string.associate_track_success_description),
            iconRes = R.drawable.ic_album_created,
            buttonText = getString(R.string.associate_track_success_button),
            onButtonClick = ::returnToAlbumDetail
        )

        binding.errorInformationWidget.setContent(
            title = getString(R.string.album_create_error_title),
            description = getString(R.string.associate_track_save_error_description),
            iconRes = R.drawable.ic_signal_off,
            buttonText = getString(R.string.album_create_back_to_form),
            onButtonClick = viewModel::onSaveErrorDismissed
        )

        binding.btnAssociateTrack.setOnClick(::associateTrack)

        viewModel.nameError.observe(viewLifecycleOwner, ::showNameError)
        viewModel.durationError.observe(viewLifecycleOwner, ::showDurationError)
        viewModel.state.observe(viewLifecycleOwner, ::showState)
    }

    /**
     * Android restaura el texto de los campos justo antes de este método. Si se escucharan desde
     * antes, al rotar la pantalla el campo vacío sin tocar mostraría "obligatorio".
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding.etTrackName.doAfterTextChanged { viewModel.onNameChanged(it ?: "") }
        binding.etTrackDuration.doAfterTextChanged { viewModel.onDurationChanged(it ?: "") }
    }

    private fun showDurationError(error: TrackFieldError) {
        binding.tilTrackDuration.error = when (error) {
            TrackFieldError.REQUIRED -> getString(R.string.album_create_error_required)
            TrackFieldError.INVALID_DURATION -> getString(R.string.associate_track_error_duration_format)
            TrackFieldError.ZERO_DURATION -> getString(R.string.associate_track_error_duration_zero)
            else -> null
        }
    }

    private fun showNameError(error: TrackFieldError) {
        binding.tilTrackName.error = when (error) {
            TrackFieldError.NONE -> null
            TrackFieldError.REQUIRED -> getString(R.string.album_create_error_required)
            TrackFieldError.NAME_TOO_LONG -> getString(R.string.associate_track_error_name_length)
            TrackFieldError.INVALID_CHARACTERS -> getString(R.string.associate_track_error_name_characters)
            TrackFieldError.ONLY_SYMBOLS -> getString(R.string.associate_track_error_name_only_symbols)
            TrackFieldError.INVALID_DURATION, TrackFieldError.ZERO_DURATION -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        blockBackWhileSaving.remove()
        _binding = null
    }

    override fun onTrimMemory(level: Int) {
        context?.let { ctx ->
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                Glide.get(ctx).clearMemory()
            } else {
                Glide.get(ctx).trimMemory(level)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        context?.let { Glide.get(it).clearMemory() }
    }

    private fun showState(state: AssociateTrackState) {
        binding.layoutContent.isVisible =
            state == AssociateTrackState.EDITING || state == AssociateTrackState.SAVING
        binding.successInformationWidget.isVisible = state == AssociateTrackState.ASSOCIATED
        binding.errorInformationWidget.isVisible = state == AssociateTrackState.SAVE_FAILED
        binding.progressSaving.isVisible = state == AssociateTrackState.SAVING

        setFormEnabled(state == AssociateTrackState.EDITING)
        blockBackWhileSaving.isEnabled = state == AssociateTrackState.SAVING

        when (state) {
            AssociateTrackState.ASSOCIATED -> {
                showToast(R.string.associate_track_success_toast)
                // Se avisa aquí para que el detalle se recargue sin importar cómo se salga de esta pantalla
                findNavController().previousBackStackEntry
                    ?.savedStateHandle?.set(KEY_TRACK_ASSOCIATED, true)
            }
            AssociateTrackState.SAVE_FAILED -> showToast(R.string.associate_track_error_toast)
            else -> Unit
        }
    }

    private fun showToast(@StringRes messageRes: Int) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun returnToAlbumDetail() {
        findNavController().popBackStack()
    }

    private fun setFormEnabled(isEnabled: Boolean) {
        binding.tilTrackName.isEnabled = isEnabled
        binding.tilTrackDuration.isEnabled = isEnabled
        binding.btnAssociateTrack.isEnabled = isEnabled
    }

    private fun hideKeyboard() {
        ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun associateTrack() {
        hideKeyboard()
        val track = Track(
            name = binding.etTrackName.trimmedText(),
            duration = binding.etTrackDuration.trimmedText()
        )
        viewModel.associate(track)
    }

    private fun TextView.trimmedText() = text.toString().trim()

    companion object {
        const val KEY_TRACK_ASSOCIATED = "track_associated"
    }
}

package com.example.vinyls_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AssociateTrackFragmentBinding
import com.example.vinyls_application.models.Track
import com.example.vinyls_application.viewmodels.AssociateTrackState
import com.example.vinyls_application.viewmodels.AssociateTrackViewModel

/**
 * Pantalla para asociar una canción a un álbum.
 *
 * Por ahora solo muestra la pantalla con los campos nombre (`etTrackName`) y duración
 * (`etTrackDuration`). Falta: validación, errores y la acción del botón `btnAssociateTrack`.
 */
class AssociateTrackFragment : Fragment() {

    private var _binding: AssociateTrackFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AssociateTrackViewModel

    /** Álbum al que se asociará la canción (llega desde el detalle del álbum). */
    private val args: AssociateTrackFragmentArgs by navArgs()

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

        viewModel.state.observe(viewLifecycleOwner, ::showState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showState(state: AssociateTrackState) {
        binding.layoutContent.isVisible =
            state == AssociateTrackState.EDITING || state == AssociateTrackState.SAVING
        binding.successInformationWidget.isVisible = state == AssociateTrackState.ASSOCIATED
        binding.errorInformationWidget.isVisible = state == AssociateTrackState.SAVE_FAILED

        when (state) {
            AssociateTrackState.ASSOCIATED -> showToast(R.string.associate_track_success_toast)
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

    private fun associateTrack() {
        val track = Track(
            name = binding.etTrackName.trimmedText(),
            duration = binding.etTrackDuration.trimmedText()
        )
        viewModel.associate(track)
    }

    private fun TextView.trimmedText() = text.toString().trim()
}

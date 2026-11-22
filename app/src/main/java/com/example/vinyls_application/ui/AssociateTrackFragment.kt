package com.example.vinyls_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.vinyls_application.databinding.AssociateTrackFragmentBinding
import com.example.vinyls_application.models.Track
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

        binding.btnAssociateTrack.setOnClick(::associateTrack)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

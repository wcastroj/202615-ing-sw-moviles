package com.example.vinyls_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.PerformerDetailFragmentBinding
import com.example.vinyls_application.models.PerformerBase
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.ui.adapters.RelatedAlbumsAdapter
import com.example.vinyls_application.viewmodels.PerformerDetailViewModel

class PerformerDetailFragment : Fragment() {

    private var _binding: PerformerDetailFragmentBinding? = null
    private val binding get() = _binding!!
    private val args: PerformerDetailFragmentArgs by navArgs()
    private lateinit var viewModel: PerformerDetailViewModel
    private lateinit var albumsAdapter: RelatedAlbumsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PerformerDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = args.id
        val type = args.type

        viewModel = ViewModelProvider(
            this,
            PerformerDetailViewModel.Factory(
                requireActivity().application,
                id,
                type
            )
        )[PerformerDetailViewModel::class.java]

        albumsAdapter = RelatedAlbumsAdapter()
        binding.rvRelatedAlbums.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = albumsAdapter
        }

        binding.errorInformationWidget.setContent(
            title = "¡Ups! Algo se\ndesafinó en la señal",
            description = "Algo inesperado ha ocurrido.\nIntenta más tarde.",
            iconRes = R.drawable.ic_signal_off
        )

        binding.errorInformationWidget.setOnClickListener {
            binding.layoutError.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
            viewModel.getPerformerDetail()
        }

        viewModel.performer.observe(viewLifecycleOwner, Observer<PerformerBase> { performer ->
            performer.apply {
                val typeTag = if (type == PerformerType.BAND) "Banda" else "Músico/a"
                val prizesCount = "${performerPrizes.size} Premios"
                val albumsCount = "${albums.size} Álbumes"

                binding.detailCard.setContent(
                    title = name,
                    coverUrl = image,
                    tag = typeTag,
                    subtag = prizesCount,
                    subtitle = albumsCount,
                    description = description
                )

                if (albums.isEmpty()) {
                    binding.rvRelatedAlbums.visibility = View.GONE
                    binding.txtEmptyAlbums.visibility = View.VISIBLE
                } else {
                    binding.rvRelatedAlbums.visibility = View.VISIBLE
                    binding.txtEmptyAlbums.visibility = View.GONE
                    albumsAdapter.albums = albums
                }
            }
        })

        viewModel.eventNetworkError.observe(viewLifecycleOwner, Observer<Boolean> { isNetworkError ->
            if (isNetworkError) onNetworkError()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onNetworkError() {
        if (!viewModel.isNetworkErrorShown.value!!) {
            binding.layoutContent.visibility = View.GONE
            binding.layoutError.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_LONG).show()
            viewModel.onNetworkErrorShown()
        }
    }
}
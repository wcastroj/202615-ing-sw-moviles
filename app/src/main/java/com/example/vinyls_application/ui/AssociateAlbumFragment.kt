package com.example.vinyls_application.ui

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AssociateAlbumFragmentBinding
import com.example.vinyls_application.ui.widgets.DropdownItem
import com.example.vinyls_application.viewmodels.AssociateAlbumViewModel

class AssociateAlbumFragment : Fragment(), ComponentCallbacks2 {

    private var _binding: AssociateAlbumFragmentBinding? = null
    private val binding get() = _binding!!
    private val args: AssociateAlbumFragmentArgs by navArgs()
    private lateinit var viewModel: AssociateAlbumViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AssociateAlbumFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val performerId = args.id
        val performerName = args.name
        val performerType = args.type

        viewModel = ViewModelProvider(
            this,
            AssociateAlbumViewModel.Factory(
                requireActivity().application,
                performerId,
                performerType
            )
        )[AssociateAlbumViewModel::class.java]

        binding.tvTitle.text = getString(R.string.associate_album_title_format, performerName)

        binding.errorInformationWidget.setContent(
            title = getString(R.string.error_title_default),
            description = getString(R.string.error_description_default),
            iconRes = R.drawable.ic_signal_off
        )

        binding.errorInformationWidget.setOnClickListener {
            binding.layoutError.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
            viewModel.retry()
        }

        binding.emptyAlbumsInformationWidget.setContent(
            title = getString(R.string.associate_no_albums_title),
            description = getString(R.string.associate_no_albums_description),
            iconRes = R.drawable.sinalbum
        )

        binding.btnSubmitAssociate.setOnClick {
            val selected = binding.dropdownAlbums.getSelectedItem()
            if (selected == null) {
                binding.dropdownAlbums.showError(getString(R.string.associate_album_select_error))
            } else {
                binding.dropdownAlbums.hideError()
                viewModel.associateAlbum(selected.id)
            }
        }

        viewModel.availableAlbums.observe(viewLifecycleOwner, Observer { albums ->
            if (albums.isNullOrEmpty()) {
                binding.dropdownAlbums.setEnabledState(false)
                binding.btnSubmitAssociate.isEnabled = false
                binding.layoutEmptyAlbums.isVisible = true
            } else {
                binding.layoutEmptyAlbums.isVisible = false
                binding.dropdownAlbums.setEnabledState(true)
                binding.btnSubmitAssociate.isEnabled = true

                val dropdownItems = ArrayList<DropdownItem>(albums.size)
                for (i in 0 until albums.size) {
                    val album = albums[i]
                    dropdownItems.add(DropdownItem(album.albumId, album.name))
                }
                binding.dropdownAlbums.setItems(dropdownItems)
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.isVisible = isLoading
            val hasAlbums = !viewModel.availableAlbums.value.isNullOrEmpty()
            binding.btnSubmitAssociate.isEnabled = !isLoading && hasAlbums
            binding.dropdownAlbums.setEnabledState(!isLoading && hasAlbums)
        })

        viewModel.associationSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), getString(R.string.associate_album_success), Toast.LENGTH_SHORT).show()
                viewModel.onAssociationSuccessHandled()
                findNavController().navigateUp()
            }
        })

        viewModel.eventNetworkError.observe(viewLifecycleOwner, Observer { isNetworkError ->
            if (isNetworkError) onNetworkError()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

    private fun onNetworkError() {
        if (!viewModel.isNetworkErrorShown.value!!) {
            binding.layoutContent.visibility = View.GONE
            binding.layoutError.visibility = View.VISIBLE
            Toast.makeText(requireContext(), getString(R.string.network_error_toast), Toast.LENGTH_LONG).show()
            viewModel.onNetworkErrorShown()
        }
    }
}

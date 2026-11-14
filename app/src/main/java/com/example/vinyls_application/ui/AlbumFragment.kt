package com.example.vinyls_application.ui

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AlbumFragmentBinding
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.ui.widgets.CatalogItem
import com.example.vinyls_application.viewmodels.AlbumViewModel

/**
 * Catálogo de álbumes en cuadrícula.
 */
class AlbumFragment : Fragment(), ComponentCallbacks2 {
    private var _binding: AlbumFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AlbumViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AlbumFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Al pulsar una tarjeta del catálogo se abre el detalle del álbum
        binding.albumsRv.setOnItemClickListener { item ->
            val action = AlbumFragmentDirections.actionAlbumFragmentToDetailFragment(item.id)
            findNavController().navigate(action)
        }

        binding.errorInformationWidget.setContent(
            title = "¡Ups! Algo se\ndesafinó en la señal",
            description = "Algo inesperado ha ocurrido.\nIntenta más tarde.",
            iconRes = R.drawable.ic_signal_off
        )

        binding.emptyInformationWidget.setContent(
            title = "No se encontraron álbumes",
            description = "Por ahora no hay álbumes disponibles",
            iconRes = R.drawable.sinalbum
        )

        binding.errorInformationWidget.setOnClickListener {
            binding.layoutError.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
        }

        val activity = requireNotNull(this.activity) {
            "You can only access the viewModel after onViewCreated()"
        }
        activity.actionBar?.title = getString(R.string.title_albums)

        viewModel = ViewModelProvider(this, AlbumViewModel.Factory(activity.application))[AlbumViewModel::class.java]

        viewModel.albums.observe(viewLifecycleOwner, Observer<List<Album>> { list ->
            val items = ArrayList<CatalogItem>(list.size)
            for (i in 0 until list.size) {
                val album = list[i]
                items.add(
                    CatalogItem(
                        id = album.albumId,
                        title = album.name,
                        coverUrl = album.cover,
                        subtitle = album.genre,
                        detail = album.recordLabel
                    )
                )
            }
            binding.albumsRv.setItems(items)
            binding.layoutError.visibility = View.GONE
            if (list.isNotEmpty()) {
                binding.layoutContent.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
            } else {
                binding.layoutContent.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
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
            binding.layoutEmpty.visibility = View.GONE
            binding.layoutError.visibility = View.VISIBLE
            Toast.makeText(activity, "Network Error", Toast.LENGTH_LONG).show()
            viewModel.onNetworkErrorShown()
        }
    }
}

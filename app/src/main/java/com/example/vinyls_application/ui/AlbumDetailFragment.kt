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
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AlbumDetailFragmentBinding
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.ui.widgets.CommentItem
import com.example.vinyls_application.ui.widgets.SummaryItem
import com.example.vinyls_application.viewmodels.AlbumDetailViewModel

class AlbumDetailFragment : Fragment() {

    private var _binding: AlbumDetailFragmentBinding? = null
    private val binding get() = _binding!!
    private val args: AlbumDetailFragmentArgs by navArgs()
    private lateinit var viewModel: AlbumDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AlbumDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val albumId = args.albumId
        viewModel = ViewModelProvider(
            this,
            AlbumDetailViewModel.Factory(requireActivity().application, albumId)
        )[AlbumDetailViewModel::class.java]

        binding.errorInformationWidget.setContent(
            title = "¡Ups! Algo se\ndesafinó en la señal",
            description = "Algo inesperado ha ocurrido.\nIntenta más tarde.",
            iconRes = R.drawable.ic_signal_off
        )

        binding.errorInformationWidget.setOnClickListener {
            binding.layoutError.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
            viewModel.retry()
        }

        viewModel.album.observe(viewLifecycleOwner, Observer<AlbumDetail> { albumDetail ->
            albumDetail.apply {

                binding.detailCard.setContent(
                    title = this.name,
                    coverUrl = this.cover,
                    tag = this.genre,
                    subtag = this.recordLabel,
                    subtitle = this.tracks.size.toString() + " Canciones",
                    description = this.description
                )

                val trackItems = this.tracks.map { track ->
                    SummaryItem(
                        id = track.trackId.toString(),
                        title = track.name,
                        subtitle = track.duration
                    )
                }
                binding.tracksListWidget.setItems(trackItems)

                val performerItems = this.performers.map { performer ->
                    SummaryItem(
                        id = performer.id.toString(),
                        title = performer.name,
                        subtitle = performer.birthDate ?: performer.creationDate ?: ""
                    )
                }
                binding.performersListWidget.setItems(performerItems)

                val listComment = mutableListOf<CommentItem>()
                val comments = this.comments
                for (i in 0 until comments.size) {
                    val comment = comments[i]
                    val newComment = CommentItem(rating = comment.rating.toIntOrNull() ?: 0, comment = comment.description)
                    listComment.add(newComment)
                }
                binding.commentsList.setComments(listComment)
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
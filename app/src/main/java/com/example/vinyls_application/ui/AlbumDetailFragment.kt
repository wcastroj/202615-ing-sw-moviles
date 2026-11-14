package com.example.vinyls_application.ui

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.AlbumDetailFragmentBinding
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.ui.LoginFragment.Companion.KEY_IS_COLLECTOR
import com.example.vinyls_application.ui.widgets.CommentItem
import com.example.vinyls_application.ui.widgets.SummaryItem
import com.example.vinyls_application.viewmodels.AlbumDetailViewModel

class AlbumDetailFragment : Fragment(), ComponentCallbacks2 {

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
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val isCollector = sharedPref.getBoolean(KEY_IS_COLLECTOR, false)

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

        binding.buttonAssociateTrack.isVisible = isCollector
        binding.addCommentWidget.isVisible = isCollector

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

                val tracks = this.tracks
                val trackItems = ArrayList<SummaryItem>(tracks.size)
                for (i in 0 until tracks.size) {
                    val track = tracks[i]
                    trackItems.add(
                        SummaryItem(
                            id = track.trackId.toString(),
                            title = track.name,
                            subtitle = track.duration
                        )
                    )
                }
                binding.tracksListWidget.setItems(trackItems)

                val performers = this.performers
                val performerItems = ArrayList<SummaryItem>(performers.size)
                for (i in 0 until performers.size) {
                    val performer = performers[i]
                    performerItems.add(
                        SummaryItem(
                            id = performer.id.toString(),
                            title = performer.name,
                            subtitle = performer.date
                        )
                    )
                }
                binding.performersListWidget.setItems(performerItems)

                val comments = this.comments
                val listComment = ArrayList<CommentItem>(comments.size)
                for (i in 0 until comments.size) {
                    val comment = comments[i]
                    listComment.add(
                        CommentItem(
                            rating = comment.rating.toIntOrNull() ?: 0,
                            comment = comment.description
                        )
                    )
                }
                binding.commentsList.setComments(listComment)
                binding.titleComment.isVisible = isCollector || listComment.isNotEmpty()
            }

        })

        binding.addCommentWidget.onSubmitCommentListener = { rating, comment ->
            viewModel.addComment(comment, rating)
        }

        viewModel.isLoading.observe(viewLifecycleOwner, Observer<Boolean> { isLoading ->
            binding.addCommentWidget.setLoading(isLoading)
        })

        viewModel.commentPostedSuccess.observe(viewLifecycleOwner, Observer<Boolean> { isSuccess ->
            if (isSuccess) {
                binding.addCommentWidget.clearForm()
                Toast.makeText(requireContext(), getString(R.string.add_comment_success), Toast.LENGTH_SHORT).show()
                viewModel.onCommentPostedSuccessHandled()
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
            binding.layoutError.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_LONG).show()
            viewModel.onNetworkErrorShown()
        }
    }
}

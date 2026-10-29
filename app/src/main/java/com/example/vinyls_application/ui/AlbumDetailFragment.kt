package com.example.vinyls_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.vinyls_application.databinding.AlbumDetailFragmentBinding
import com.example.vinyls_application.ui.widgets.CommentItem

class AlbumDetailFragment : Fragment() {

    private var _binding: AlbumDetailFragmentBinding? = null
    private val binding get() = _binding!!
    private val args: AlbumDetailFragmentArgs by navArgs()

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

        // Datos de prueba (después vendrán de la API)
        binding.detailCard.setContent(
            title = "Endless Journey",
            coverUrl = "https://picsum.photos/600",
            tag = "Rock",
            subtag = "Echo Records",
            subtitle = "3 Canciones",
            description = "Una atmósfera envolvente con paisajes sonoros que exploran la libertad."
        )

        binding.commentsList.setComments(
            listOf(
                CommentItem(rating = 3, comment = "Muy buena producción."),
                CommentItem(rating = 5, comment = "De mis favoritos."),
                CommentItem(rating = 4, comment = null)
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
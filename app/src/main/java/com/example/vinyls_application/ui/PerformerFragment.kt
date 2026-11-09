package com.example.vinyls_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.PerformerFragmentBinding
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.ui.widgets.CatalogItem
import com.example.vinyls_application.viewmodels.PerformerViewModel

/**
 * Catálogo de artistas (bandas y músicos) en cuadrícula usando
 * [com.example.vinyls_application.ui.widgets.CatalogListWidget], igual que el catálogo de álbumes.
 */
class PerformerFragment : Fragment() {
    private var _binding: PerformerFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PerformerViewModel
    private var performerTypeMap: Map<Int, PerformerType> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PerformerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.performersRv.setOnItemClickListener { catalogItem ->
            val type = performerTypeMap[catalogItem.id]
            if (type != null) {
                val action =
                    PerformerFragmentDirections.actionPerformerFragmentToPerformerDetailFragment(
                        id = catalogItem.id,
                        type = type
                    )
                findNavController().navigate(action)
            }
        }

        binding.errorInformationWidget.setContent(
            title = "¡Ups! Algo se\ndesafinó en la señal",
            description = "Algo inesperado ha ocurrido.\nIntenta más tarde.",
            iconRes = R.drawable.ic_signal_off
        )

        binding.emptyInformationWidget.setContent(
            title = getString(R.string.performers_empty_title),
            description = getString(R.string.performers_empty_description),
            iconRes = R.drawable.sinalbum
        )

        binding.errorInformationWidget.setOnClickListener {
            binding.layoutError.visibility = View.GONE
            binding.layoutContent.visibility = View.VISIBLE
        }

        val activity = requireNotNull(this.activity) {
            "You can only access the viewModel after onViewCreated()"
        }

        viewModel = ViewModelProvider(
            this,
            PerformerViewModel.Factory(activity.application)
        )[PerformerViewModel::class.java]

        viewModel.performers.observe(viewLifecycleOwner) { list ->
            performerTypeMap = list.associate { it.id to it.type }
            binding.performersRv.setItems(list.map { it.toCatalogItem() })
            binding.layoutError.visibility = View.GONE
            if (list.isNotEmpty()) {
                binding.layoutContent.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
            } else {
                binding.layoutContent.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            }
        }

        viewModel.eventNetworkError.observe(viewLifecycleOwner) { isNetworkError ->
            if (isNetworkError) onNetworkError()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Un músico muestra su `birthDate` y una banda su `creationDate`. */
    private fun Performer.toCatalogItem() = CatalogItem(
        id = id,
        title = name,
        coverUrl = image,
        subtitle = getString(
            when (type) {
                PerformerType.MUSICIAN -> R.string.performer_type_musician
                PerformerType.BAND -> R.string.performer_type_band
            }
        ),
        detail = formatPerformerDate(birthDate ?: creationDate)
    )

    private fun onNetworkError() {
        if (!viewModel.isNetworkErrorShown.value!!) {
            binding.layoutContent.visibility = View.GONE
            binding.layoutEmpty.visibility = View.GONE
            binding.layoutError.visibility = View.VISIBLE
            Toast.makeText(activity, "Network Error", Toast.LENGTH_LONG).show()
            viewModel.onNetworkErrorShown()
        }
    }

    companion object {
        private val ISO_DATE = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

        /**
         * Convierte la fecha ISO del backend (`"2009-10-25T05:00:00.000Z"`) a `"25/10/2009"`.
         * Solo usa la parte de la fecha para que la zona horaria no la corra un día.
         * Si no tiene ese formato se devuelve tal cual.
         */
        @VisibleForTesting
        fun formatPerformerDate(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            val match = ISO_DATE.find(raw) ?: return raw
            val (year, month, day) = match.destructured
            return "$day/$month/$year"
        }
    }
}

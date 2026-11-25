package com.example.vinyls_application.ui

import android.content.ComponentCallbacks2
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.CollectorFragmentBinding
import com.example.vinyls_application.models.Collector
import com.example.vinyls_application.ui.widgets.CatalogItem
import com.example.vinyls_application.viewmodels.CollectorViewModel

class CollectorFragment : Fragment(), ComponentCallbacks2 {
    private var _binding: CollectorFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CollectorViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CollectorFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("ReplaceManualRangeWithIndicesCalls")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.collectorsRv.setOnItemClickListener {}

        binding.errorInformationWidget.setContent(
            title = "¡Ups! Algo se\ndesafinó en la señal",
            description = "Algo inesperado ha ocurrido.\nIntenta más tarde.",
            iconRes = R.drawable.ic_signal_off
        )

        binding.emptyInformationWidget.setContent(
            title = getString(R.string.collectors_empty_title),
            description = getString(R.string.collectors_empty_description),
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
            CollectorViewModel.Factory(activity.application)
        )[CollectorViewModel::class.java]

        viewModel.collectors.observe(viewLifecycleOwner) { list ->
            val catalogItems = ArrayList<CatalogItem>(list.size)
            for (i in 0 until list.size) {
                val collector = list[i]
                catalogItems.add(collector.toCatalogItem())
            }
            binding.collectorsRv.setItems(catalogItems)
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

    private fun Collector.toCatalogItem() = CatalogItem(
        id = collectorId,
        title = name,
        coverUrl = null,
        subtitle = telephone,
        detail = email
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
}

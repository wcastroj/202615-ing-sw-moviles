package com.example.vinyls_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.LoginFragmentBinding
import android.content.Context
import android.util.Log

class LoginFragment : Fragment() {

    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!
    companion object {
        const val KEY_IS_COLLECTOR = "is_collector"
    }
    private fun saveRole(isCollector: Boolean) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putBoolean(KEY_IS_COLLECTOR, isCollector)
            apply()
        }
        Log.d("LoginFragment", "is_collector guardado = ${sharedPref.getBoolean(KEY_IS_COLLECTOR, false)}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LoginFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardVisitor.setOnEnterClickListener {
            saveRole(isCollector = false)
            findNavController().navigate(R.id.action_loginFragment_to_albumFragment)
        }

        binding.cardCollector.setOnEnterClickListener {
            saveRole(isCollector = true)
            findNavController().navigate(R.id.action_loginFragment_to_albumFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
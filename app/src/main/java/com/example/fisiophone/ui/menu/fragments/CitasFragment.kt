package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentCitasBinding
import kotlinx.coroutines.launch

class CitasFragment : Fragment() {

    private var _binding: FragmentCitasBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CitasViewModel by viewModels()
    private lateinit var citaAdapter: CitaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvCitas.layoutManager = LinearLayoutManager(requireContext())
        citaAdapter = CitaAdapter(emptyList(), true) { cita ->
            viewModel.deleteCita(cita)
        }
        binding.rvCitas.adapter = citaAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.isPatient.collect { isPatient ->
                        // Recrear el adapter con el rol correcto
                        citaAdapter = CitaAdapter(viewModel.citas.value, isPatient) { cita ->
                            viewModel.deleteCita(cita)
                        }
                        binding.rvCitas.adapter = citaAdapter
                    }
                }

                launch {
                    viewModel.citas.collect { citas ->
                        if (citas.isEmpty()) {
                            binding.layoutEmptyCitas.visibility = View.VISIBLE
                            citaAdapter.updateList(emptyList())
                        } else {
                            binding.layoutEmptyCitas.visibility = View.GONE
                            citaAdapter.updateList(citas)
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.pbCitas.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.messages.collect { result ->
                        result.onSuccess { msg ->
                            if (msg == "DELETED") {
                                Toast.makeText(requireContext(), getString(R.string.cita_cancelada), Toast.LENGTH_SHORT).show()
                            }
                        }.onFailure { e ->
                            Toast.makeText(requireContext(), getString(R.string.error_con_mensaje, e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

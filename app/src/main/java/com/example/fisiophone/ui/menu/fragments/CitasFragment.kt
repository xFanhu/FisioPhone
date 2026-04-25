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
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        binding.cardDatePicker.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar fecha")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val date = Date(selection)
                viewModel.updateSelectedDate(date)
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.isPatient.collect { isPatient ->
                        binding.cardDatePicker.visibility = if (isPatient) View.GONE else View.VISIBLE
                        // Recrear el adapter con el rol correcto
                        citaAdapter = CitaAdapter(viewModel.citas.value, isPatient) { cita ->
                            viewModel.deleteCita(cita)
                        }
                        binding.rvCitas.adapter = citaAdapter
                    }
                }

                launch {
                    viewModel.selectedDate.collect { dateStr ->
                        if (dateStr != null) {
                            try {
                                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = sdfIn.parse(dateStr)
                                if (date != null) {
                                    // Comprobar si es hoy
                                    val todayStr = sdfIn.format(Date())
                                    if (dateStr == todayStr) {
                                        binding.tvSelectedDateFilter.text = "Hoy"
                                    } else {
                                        val sdfOut = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        binding.tvSelectedDateFilter.text = "Citas del ${sdfOut.format(date)}"
                                    }
                                }
                            } catch (e: Exception) {}
                        }
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

package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentAddCitaBinding
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddCitaFragment : Fragment() {

    private var _binding: FragmentAddCitaBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AddCitaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCitaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        observeViewModel()
        
        // Restaurar visibilidad si el ViewModel ya tenía datos (por rotación)
        if (viewModel.selectedPhysio != null) {
            binding.tvSelectDateLabel.visibility = View.VISIBLE
            binding.btnOpenCalendar.visibility = View.VISIBLE
        }
        if (viewModel.selectedDate != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvSelectedDateDisplay.text = getString(R.string.dia_seleccionado, sdf.format(viewModel.selectedDate!!))
            binding.tvSelectedDateDisplay.visibility = View.VISIBLE
        }
        if (viewModel.selectedTime != null) {
            binding.btnConfirmBooking.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.autoCompletePhysio.setOnItemClickListener { _, _, position, _ ->
            val physios = viewModel.physios.value
            if (position in physios.indices) {
                viewModel.selectPhysio(physios[position])
                resetStep2()
                
                val treatments = physios[position].treatments
                if (treatments.isNotEmpty()) {
                    binding.tvSelectTreatmentLabel.visibility = View.VISIBLE
                    binding.cgTreatments.visibility = View.VISIBLE
                    binding.cgTreatments.removeAllViews()
                    for (treatment in treatments) {
                        val chip = Chip(requireContext()).apply {
                            text = treatment
                            isCheckable = true
                            setOnClickListener {
                                viewModel.selectTreatment(treatment)
                                binding.tvSelectDateLabel.visibility = View.VISIBLE
                                binding.btnOpenCalendar.visibility = View.VISIBLE
                            }
                        }
                        binding.cgTreatments.addView(chip)
                    }
                } else {
                    // Fallback si el fisio no configuró tratamientos
                    viewModel.selectTreatment(getString(R.string.sesion_fisio_default))
                    binding.tvSelectDateLabel.visibility = View.VISIBLE
                    binding.btnOpenCalendar.visibility = View.VISIBLE
                }
            }
        }

        binding.btnOpenCalendar.setOnClickListener {
            showDatePicker()
        }

        binding.btnConfirmBooking.setOnClickListener {
            viewModel.confirmBooking()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.physios.collect { physios ->
                        val names = physios.map { "${it.nombre} ${it.apellidos}" }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        binding.autoCompletePhysio.setAdapter(adapter)
                    }
                }
                
                launch {
                    viewModel.availableSlots.collect { slots ->
                        binding.cgTimeSlots.removeAllViews()
                        if (slots.isNotEmpty()) {
                            slots.forEach { time -> addTimeChip(time) }
                            binding.tvSelectTimeLabel.visibility = View.VISIBLE
                            binding.cgTimeSlots.visibility = View.VISIBLE
                            
                            // Si ya había una hora seleccionada, marcar el chip
                            val selectedTime = viewModel.selectedTime
                            if (selectedTime != null) {
                                for (i in 0 until binding.cgTimeSlots.childCount) {
                                    val chip = binding.cgTimeSlots.getChildAt(i) as Chip
                                    if (chip.text == selectedTime) {
                                        chip.isChecked = true
                                        break
                                    }
                                }
                            }
                        } else if (viewModel.selectedDate != null) {
                            // Opcional: mostrar mensaje si no hay huecos ese día pero es día laborable
                        }
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.btnConfirmBooking.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.bookingResult.collect { result ->
                        result.onSuccess {
                            Toast.makeText(requireContext(), getString(R.string.cita_exito), Toast.LENGTH_LONG).show()
                            parentFragmentManager.beginTransaction()
                                .replace(com.example.fisiophone.R.id.fragmentHost, CitasFragment())
                                .commit()
                        }.onFailure { e ->
                            when (e.message) {
                                "NO_WORKING_DAY" -> Toast.makeText(requireContext(), getString(R.string.error_dia_no_laborable), Toast.LENGTH_LONG).show()
                                "NO_SCHEDULE" -> Toast.makeText(requireContext(), getString(R.string.error_sin_horario), Toast.LENGTH_SHORT).show()
                                else -> Toast.makeText(requireContext(), getString(R.string.error_datos_paciente, e.message), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.seleccionar_fecha))
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            viewModel.selectDate(date)
            
            if (viewModel.selectedDate != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.tvSelectedDateDisplay.text = getString(R.string.dia_seleccionado, sdf.format(date))
                binding.tvSelectedDateDisplay.visibility = View.VISIBLE
            }
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun addTimeChip(time: String) {
        val chip = Chip(requireContext()).apply {
            text = time
            isCheckable = true
            setOnClickListener {
                viewModel.selectTime(time)
                binding.btnConfirmBooking.visibility = View.VISIBLE
            }
        }
        binding.cgTimeSlots.addView(chip)
    }

    private fun resetStep2() {
        binding.tvSelectTreatmentLabel.visibility = View.GONE
        binding.cgTreatments.visibility = View.GONE
        binding.tvSelectDateLabel.visibility = View.GONE
        binding.btnOpenCalendar.visibility = View.GONE
        binding.tvSelectedDateDisplay.visibility = View.GONE
        binding.tvSelectTimeLabel.visibility = View.GONE
        binding.cgTimeSlots.visibility = View.GONE
        binding.btnConfirmBooking.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

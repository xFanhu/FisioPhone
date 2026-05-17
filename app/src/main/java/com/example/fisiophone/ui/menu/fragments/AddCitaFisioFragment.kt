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
import com.example.fisiophone.databinding.FragmentAddCitaFisioBinding
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CompositeDateValidator
import com.example.fisiophone.utils.WorkingDaysValidator

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddCitaFisioFragment : Fragment() {

    private var _binding: FragmentAddCitaFisioBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AddCitaFisioViewModel by viewModels()

    companion object {
        private const val ARG_CITA_ID = "cita_id"
        fun newInstance(citaId: String? = null) = AddCitaFisioFragment().apply {
            arguments = Bundle().apply {
                if (citaId != null) putString(ARG_CITA_ID, citaId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCitaFisioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString(ARG_CITA_ID)?.let { citaId ->
            viewModel.setupEditMode(citaId)
            binding.tvAddCitaTitle.text = getString(R.string.modificar_cita)
            binding.btnConfirmBooking.text = getString(R.string.btn_actualizar_reserva)
        }
        
        setupListeners()
        observeViewModel()
        restoreUIState()
    }

    private fun restoreUIState() {
        if (viewModel.selectedPatient != null) {
            if (viewModel.currentUserRole == "administrador") {
                binding.tvSelectPhysioLabel.visibility = View.VISIBLE
                binding.tilPhysioSelector.visibility = View.VISIBLE
            }
            if (viewModel.selectedPhysio != null) {
                binding.tvSelectTreatmentLabel.visibility = View.VISIBLE
                binding.cgTreatments.visibility = View.VISIBLE
            }
        }
        
        if (viewModel.selectedDate != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvSelectedDateDisplay.text = getString(R.string.dia_seleccionado, sdf.format(viewModel.selectedDate!!))
            binding.tvSelectedDateDisplay.visibility = View.VISIBLE
            
            // Aseguramos que los controles de fecha y hora sean visibles en edición
            binding.tvSelectDateLabel.visibility = View.VISIBLE
            binding.btnOpenCalendar.visibility = View.VISIBLE
        }
        
        // El botón de confirmar solo se ve si hay una hora elegida
        binding.btnConfirmBooking.visibility = if (viewModel.selectedTime != null) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        binding.autoCompletePatient.setOnItemClickListener { _, _, position, _ ->
            val patients = viewModel.patients.value
            if (position in patients.indices) {
                viewModel.selectPatient(patients[position])
                resetStepsAfterPatient()
                
                if (viewModel.currentUserRole == "administrador") {
                    binding.tvSelectPhysioLabel.visibility = View.VISIBLE
                    binding.tilPhysioSelector.visibility = View.VISIBLE
                } else if (viewModel.selectedPhysio != null) {
                    // Cuando eres usuario fisio, el fisio se autoselecciona
                    showTreatments(viewModel.selectedPhysio!!.treatments)
                }
            }
        }

        binding.autoCompletePhysio.setOnItemClickListener { _, _, position, _ ->
            val physios = viewModel.physios.value
            if (position in physios.indices) {
                viewModel.selectPhysio(physios[position])
                resetStepsAfterPhysio()
                showTreatments(physios[position].treatments)
            }
        }

        binding.btnOpenCalendar.setOnClickListener {
            showDatePicker()
        }

        binding.btnConfirmBooking.setOnClickListener {
            viewModel.confirmBooking()
        }
    }
    
    private fun showTreatments(treatments: List<String>) {
        if (treatments.isNotEmpty()) {
            binding.tvSelectTreatmentLabel.visibility = View.VISIBLE
            binding.cgTreatments.visibility = View.VISIBLE
            binding.cgTreatments.removeAllViews()
            for (treatment in treatments) {
                val chip = Chip(requireContext()).apply {
                    text = treatment
                    isCheckable = true
                    isChecked = (treatment == viewModel.selectedTreatment)
                    setOnClickListener {
                        viewModel.selectTreatment(treatment)
                        binding.tvSelectDateLabel.visibility = View.VISIBLE
                        binding.btnOpenCalendar.visibility = View.VISIBLE
                        
                        // Si ya tenemos una fecha (en modo edición), cargamos las horas directamente
                        viewModel.selectedDate?.let { date ->
                            viewModel.fetchAvailableSlots(date, skipCurrentTimeCheck = true)
                        }
                    }
                }
                binding.cgTreatments.addView(chip)
            }
        } else {
            viewModel.selectTreatment(getString(R.string.sesion_fisio_default))
            binding.tvSelectDateLabel.visibility = View.VISIBLE
            binding.btnOpenCalendar.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.patients.collect { patients ->
                        val names = patients.map { "${it.nombre} ${it.apellidos}" }
                        val adapter = object : ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            names
                        ) {
                            override fun getFilter(): android.widget.Filter {
                                return object : android.widget.Filter() {
                                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                                        val results = FilterResults()
                                        results.values = names
                                        results.count = names.size
                                        return results
                                    }
                                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                                        notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                        binding.autoCompletePatient.setAdapter(adapter)
                        

                        if (viewModel.isEditMode && viewModel.selectedPatient != null) {
                            binding.autoCompletePatient.setText("${viewModel.selectedPatient!!.nombre} ${viewModel.selectedPatient!!.apellidos}", false)
                        }
                    }
                }
                
                launch {
                    viewModel.physios.collect { physios ->
                        val names = physios.map { "${it.nombre} ${it.apellidos}" }
                        val adapter = object : ArrayAdapter<String>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            names
                        ) {
                            override fun getFilter(): android.widget.Filter {
                                return object : android.widget.Filter() {
                                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                                        val results = FilterResults()
                                        results.values = names
                                        results.count = names.size
                                        return results
                                    }
                                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                                        notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                        binding.autoCompletePhysio.setAdapter(adapter)


                        if (viewModel.isEditMode && viewModel.selectedPhysio != null) {
                            binding.autoCompletePhysio.setText("${viewModel.selectedPhysio!!.nombre} ${viewModel.selectedPhysio!!.apellidos}", false)
                            showTreatments(viewModel.selectedPhysio!!.treatments)
                        }
                    }
                }
                
                launch {
                    viewModel.availableSlots.collect { slots ->
                        if (slots == null) return@collect
                        
                        binding.cgTimeSlots.removeAllViews()
                        if (slots.isNotEmpty()) {
                            slots.forEach { time -> addTimeChip(time) }
                            binding.tvSelectTimeLabel.visibility = View.VISIBLE
                            binding.cgTimeSlots.visibility = View.VISIBLE
                            binding.tvNoSlotsError.visibility = View.GONE
                            
                            viewModel.selectedTime?.let { selected ->
                                for (i in 0 until binding.cgTimeSlots.childCount) {
                                    val chip = binding.cgTimeSlots.getChildAt(i) as Chip
                                    if (chip.text == selected) {
                                        chip.isChecked = true
                                        break
                                    }
                                }
                                binding.btnConfirmBooking.visibility = View.VISIBLE
                            }
                        } else if (viewModel.selectedDate != null) {
                            binding.tvSelectTimeLabel.visibility = View.GONE
                            binding.cgTimeSlots.visibility = View.GONE
                            binding.tvNoSlotsError.visibility = View.VISIBLE
                            binding.btnConfirmBooking.visibility = View.GONE
                        }
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.btnConfirmBooking.isEnabled = !isLoading
                        
                        // Si termina de cargar y estamos en edición, forzamos la actualización de la UI
                        if (!isLoading && viewModel.isEditMode) {
                            restoreUIState()
                            
                            // Forzamos el texto en los autocompletables por si ya se habían cargado las listas antes
                            viewModel.selectedPatient?.let {
                                binding.autoCompletePatient.setText("${it.nombre} ${it.apellidos}", false)
                            }
                            viewModel.selectedPhysio?.let {
                                binding.autoCompletePhysio.setText("${it.nombre} ${it.apellidos}", false)
                                showTreatments(it.treatments)
                            }
                        }
                    }
                }

                launch {
                    viewModel.bookingResult.collect { result ->
                        result.onSuccess {
                            val msg = if (viewModel.isEditMode) R.string.cita_actualizada_exito else R.string.cita_exito
                            Toast.makeText(requireContext(), getString(msg), Toast.LENGTH_LONG).show()
                            parentFragmentManager.popBackStack()
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
        val workDays = viewModel.workDays.value
        
        val validators = mutableListOf<CalendarConstraints.DateValidator>()
        validators.add(DateValidatorPointForward.now()) // Dias pasados
        validators.add(WorkingDaysValidator(workDays))  // Solo días laborables
        
        val constraints = CalendarConstraints.Builder()
            .setValidator(CompositeDateValidator.allOf(validators))
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

    private fun resetStepsAfterPatient() {
        binding.tvSelectPhysioLabel.visibility = View.GONE
        binding.tilPhysioSelector.visibility = View.GONE
        resetStepsAfterPhysio()
    }

    private fun resetStepsAfterPhysio() {
        binding.tvSelectTreatmentLabel.visibility = View.GONE
        binding.cgTreatments.visibility = View.GONE
        binding.tvSelectDateLabel.visibility = View.GONE
        binding.btnOpenCalendar.visibility = View.GONE
        binding.tvSelectedDateDisplay.visibility = View.GONE
        binding.tvSelectTimeLabel.visibility = View.GONE
        binding.cgTimeSlots.visibility = View.GONE
        binding.tvNoSlotsError.visibility = View.GONE
        binding.btnConfirmBooking.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

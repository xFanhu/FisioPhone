package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentCitasBinding
import com.example.fisiophone.ui.menu.MainActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragmento encargado de mostrar la lista de citas del usuario.
 * Filtra por rol (paciente/fisio) y por fecha si es fisio.
 */
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
        
        citaAdapter = CitaAdapter(
            emptyList(),
            viewModel.isPatient.value,
            onItemClick = { cita ->
                if (!viewModel.isPatient.value) {
                    showCitaOptionsDialog(cita)
                }
            },
            onDeleteClick = { cita ->
                confirmDeleteCita(cita)
            },
            onFinishClick = { cita ->
                confirmFinishCita(cita)
            }
        )
        binding.rvCitas.adapter = citaAdapter

        // Selector de fecha (solo visible para fisioterapeutas)
        binding.cardDatePicker.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.seleccionar_fecha))
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val date = Date(selection)
                viewModel.updateSelectedDate(date)
            }
            picker.show(parentFragmentManager, "DATE_PICKER")
        }

        binding.btnEmptyAddCita.setOnClickListener {
            (activity as? MainActivity)?.let { mainActivity ->
                val bottomNav = mainActivity.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
                bottomNav?.selectedItemId = R.id.nav_add
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                launch {
                    viewModel.isPatient.collect { isPatient ->
                        binding.cardDashboard.visibility = if (isPatient) View.GONE else View.VISIBLE
                        binding.cardDatePicker.visibility = if (isPatient) View.GONE else View.VISIBLE
                        citaAdapter.updateRole(isPatient)
                        
                        if (isPatient) {
                            binding.tvEmptyAddCita.text = getString(R.string.nueva_cita)
                            binding.ivEmptyAddCita.setImageResource(R.drawable.ic_add)
                        } else {
                            binding.tvEmptyAddCita.text = getString(R.string.configurar_horario)
                            binding.ivEmptyAddCita.setImageResource(R.drawable.ic_citas)
                        }
                    }
                }

                launch {
                    viewModel.selectedDate.collect { dateStr ->
                        if (dateStr != null) {
                            try {
                                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = sdfIn.parse(dateStr)
                                if (date != null) {
                                    val todayStr = sdfIn.format(Date())
                                    if (dateStr == todayStr) {
                                        binding.tvSelectedDateFilter.text = getString(R.string.hoy)
                                    } else {
                                        val sdfOut = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        binding.tvSelectedDateFilter.text = getString(R.string.citas_del, sdfOut.format(date))
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }

                launch {
                    viewModel.citas.collect { citas ->
                        val isPatient = viewModel.isPatient.value
                        
                        if (citas.isEmpty()) {
                            binding.layoutEmptyCitas.visibility = View.VISIBLE
                            citaAdapter.updateList(emptyList())
                            binding.cardDashboard.visibility = View.GONE
                        } else {
                            binding.layoutEmptyCitas.visibility = View.GONE
                            citaAdapter.updateList(citas)
                            
                            // Actualizar Dashboard si es Fisio
                            if (!isPatient) {
                                binding.cardDashboard.visibility = View.VISIBLE
                                val pending = citas.count { it.status == "booked" }
                                val done = citas.count { it.status == "done" }
                                binding.tvPendingCount.text = pending.toString()
                                binding.tvCompletedCount.text = done.toString()

                                // Calcular próxima cita hoy
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                if (viewModel.selectedDate.value == todayStr) {
                                    val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                    val nextCita = citas
                                        .filter { it.status == "booked" && it.time >= nowTime }
                                        .minByOrNull { it.time }

                                    if (nextCita != null) {
                                        binding.dashboardDivider.visibility = View.VISIBLE
                                        binding.layoutNextAppointment.visibility = View.VISIBLE
                                        binding.tvNextAppTime.text = nextCita.time
                                        binding.tvNextAppName.text = nextCita.patientName
                                    } else {
                                        binding.dashboardDivider.visibility = View.GONE
                                        binding.layoutNextAppointment.visibility = View.GONE
                                    }
                                } else {
                                    binding.dashboardDivider.visibility = View.GONE
                                    binding.layoutNextAppointment.visibility = View.GONE
                                }
                            } else {
                                binding.cardDashboard.visibility = View.GONE
                            }
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
                            when {
                                msg == "DELETED" -> {
                                    context?.let { Toast.makeText(it, getString(R.string.cita_cancelada), Toast.LENGTH_SHORT).show() }
                                }
                                msg.startsWith("COMPLETED") -> {
                                    val citaId = msg.split("|").getOrNull(1)
                                    val completedCita = viewModel.citas.value.find { it.id == citaId }
                                    if (completedCita != null) {
                                        showAddClinicalNoteDialog(completedCita)
                                    }
                                }
                                msg == "NOTE_SAVED" -> {
                                    context?.let { Toast.makeText(it, getString(R.string.nota_guardada_exito), Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }.onFailure { e ->
                            context?.let { Toast.makeText(it, getString(R.string.error_con_mensaje, e.message), Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
        }
    }

    private fun showCitaOptionsDialog(cita: Cita) {
        val options = mutableListOf<String>()
        options.add(getString(R.string.ver_perfil_paciente))
        
        // Solo permitir marcar como realizada si no lo está ya
        if (cita.status != "done") {
            options.add(getString(R.string.marcar_como_realizada))
        }

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.opciones_cita)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    getString(R.string.ver_perfil_paciente) -> openPatientProfile(cita.patientId)
                    getString(R.string.marcar_como_realizada) -> confirmFinishCita(cita)
                }
            }
            .show()
    }

    private fun confirmDeleteCita(cita: Cita) {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.cancelar_cita)
            .setMessage(R.string.confirmar_cancelacion)
            .setPositiveButton(R.string.si) { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteCita(cita)
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmFinishCita(cita: Cita) {
        if (!isAdded) return
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.confirmar_finalizar_titulo)
            .setMessage(getString(R.string.confirmar_finalizar_mensaje, cita.patientName))
            .setPositiveButton(R.string.si) { dialog, _ ->
                dialog.dismiss()
                viewModel.completeCita(cita)
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showAddClinicalNoteDialog(cita: Cita) {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.hint_nota_clinica)
            setLines(5)
            maxLines = 10
            gravity = Gravity.TOP or Gravity.START
        }
        
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val margin = (24 * resources.displayMetrics.density).toInt()
            setMargins(margin, margin / 2, margin, margin / 2)
        }
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(getString(R.string.titulo_anadir_nota))
            .setMessage(getString(R.string.label_paciente_dialog, cita.patientName))
            .setView(container)
            .setCancelable(false) // Forzar que decida si guarda o no
            .setPositiveButton(getString(R.string.btn_guardar_nota)) { dialog, _ ->
                val note = input.text.toString().trim()
                if (note.isNotEmpty()) {
                    dialog.dismiss()
                    viewModel.saveClinicalNote(cita, note)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_nota_vacia), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_ahora_no)) { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), getString(R.string.sesion_finalizada_sin_nota), Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun openPatientProfile(patientId: String) {
        val profileFragment = ProfileFragment.newInstance(ProfileFragment.UserRole.PATIENT, patientId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentHost, profileFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

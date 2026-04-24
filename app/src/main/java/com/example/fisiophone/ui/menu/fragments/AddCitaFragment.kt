package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentAddCitaBinding
import com.example.fisiophone.ui.menu.User
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddCitaFragment : Fragment() {

    private var _binding: FragmentAddCitaBinding? = null
    private val binding get() = _binding!!
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var physios = mutableListOf<User>()
    private var selectedPhysio: User? = null
    private var selectedDate: Date? = null
    private var selectedTime: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCitaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        fetchPhysios()
        setupListeners()
    }

    private fun fetchPhysios() {
        binding.pbLoading.visibility = View.VISIBLE
        db.collection("users").whereEqualTo("role", "fisioterapeuta").get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                binding.pbLoading.visibility = View.GONE
                physios.clear()
                val names = mutableListOf<String>()
                for (doc in result) {
                    val user = User(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        dni = doc.getString("dni") ?: "",
                        role = "fisioterapeuta"
                    )
                    physios.add(user)
                    names.add("${user.nombre} ${user.apellidos}")
                }
                
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                binding.autoCompletePhysio.setAdapter(adapter)
            }
    }

    private fun setupListeners() {
        binding.autoCompletePhysio.setOnItemClickListener { _, _, position, _ ->
            selectedPhysio = physios[position]
            resetStep2()
            binding.tvSelectDateLabel.visibility = View.VISIBLE
            binding.btnOpenCalendar.visibility = View.VISIBLE
        }

        binding.btnOpenCalendar.setOnClickListener {
            showDatePicker()
        }

        binding.btnConfirmBooking.setOnClickListener {
            confirmBooking()
        }
    }

    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha")
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            if (isWorkingDay(date)) {
                selectedDate = date
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.tvSelectedDateDisplay.text = getString(R.string.dia_seleccionado, sdf.format(date))
                binding.tvSelectedDateDisplay.visibility = View.VISIBLE
                
                fetchAvailableSlots(date)
            } else {
                Toast.makeText(requireContext(), "El profesional no trabaja ese día", Toast.LENGTH_LONG).show()
            }
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun isWorkingDay(date: Date): Boolean {
        // For simplicity in this version, we assume all fisios work M-F.
        // In a real app, we would check the 'schedule' field from the physio's document.
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }

    private fun fetchAvailableSlots(date: Date) {
        val physioId = selectedPhysio?.id ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(date)
        
        binding.pbLoading.visibility = View.VISIBLE
        binding.cgTimeSlots.removeAllViews()
        
        // 1. Fetch physio schedule
        db.collection("users").document(physioId).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val schedule = doc.get("schedule") as? Map<String, Any>
                if (schedule == null) {
                    binding.pbLoading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Este profesional no tiene horario configurado", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val startStr = schedule["startHour"] as? String ?: "09:00"
                val endStr = schedule["endHour"] as? String ?: "21:00"
                val duration = (schedule["duration"] as? Long)?.toInt() ?: 60
                
                // 2. Fetch existing appointments for this day
                db.collection("citas")
                    .whereEqualTo("physioId", physioId)
                    .whereEqualTo("date", dateStr)
                    .get()
                    .addOnSuccessListener { appointments ->
                        if (_binding == null) return@addOnSuccessListener
                        binding.pbLoading.visibility = View.GONE
                        val bookedTimes = appointments.mapNotNull { it.getString("time") }
                        
                        generateSlots(startStr, endStr, duration, bookedTimes)
                        
                        binding.tvSelectTimeLabel.visibility = View.VISIBLE
                        binding.cgTimeSlots.visibility = View.VISIBLE
                    }
            }
    }

    private fun generateSlots(start: String, end: String, duration: Int, booked: List<String>) {
        val startMins = timeToMins(start)
        val endMins = timeToMins(end)
        
        var current = startMins
        while (current + duration <= endMins) {
            val timeStr = minsToTime(current)
            if (!booked.contains(timeStr)) {
                addTimeChip(timeStr)
            }
            current += duration
        }
    }

    private fun addTimeChip(time: String) {
        val chip = Chip(requireContext()).apply {
            text = time
            isCheckable = true
            setOnClickListener {
                selectedTime = time
                binding.btnConfirmBooking.visibility = View.VISIBLE
            }
        }
        binding.cgTimeSlots.addView(chip)
    }

    private fun timeToMins(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun minsToTime(mins: Int): String {
        return String.format("%02d:%02d", mins / 60, mins % 60)
    }

    private fun confirmBooking() {
        val patientId = auth.currentUser?.uid ?: return
        val physioId = selectedPhysio?.id ?: return
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate!!)
        val timeStr = selectedTime ?: return
        
        binding.pbLoading.visibility = View.VISIBLE
        binding.btnConfirmBooking.isEnabled = false

        // Fetch patient name first
        db.collection("users").document(patientId).get()
            .addOnSuccessListener { patientDoc ->
                val patientName = if (patientDoc.exists()) {
                    "${patientDoc.getString("nombre")} ${patientDoc.getString("apellidos")}"
                } else {
                    "Paciente"
                }

                val cita = mapOf(
                    "patientId" to patientId,
                    "physioId" to physioId,
                    "physioName" to "${selectedPhysio!!.nombre} ${selectedPhysio!!.apellidos}",
                    "patientName" to patientName,
                    "date" to dateStr,
                    "time" to timeStr,
                    "status" to "booked"
                )
                
                db.collection("citas").add(cita)
                    .addOnSuccessListener {
                        if (_binding == null) return@addOnSuccessListener
                        binding.pbLoading.visibility = View.GONE
                        Toast.makeText(requireContext(), getString(R.string.cita_exito), Toast.LENGTH_LONG).show()
                        parentFragmentManager.beginTransaction()
                            .replace(com.example.fisiophone.R.id.fragmentHost, CitasFragment())
                            .commit()
                    }
                    .addOnFailureListener { e ->
                        binding.pbLoading.visibility = View.GONE
                        binding.btnConfirmBooking.isEnabled = true
                        Toast.makeText(requireContext(), getString(R.string.error_reserva), Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.pbLoading.visibility = View.GONE
                binding.btnConfirmBooking.isEnabled = true
                Toast.makeText(requireContext(), "Error al obtener datos del paciente: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetStep2() {
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

package com.example.fisiophone.ui.menu.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTreatments()
        setupTimePickers()
        setupSaveButton()
        loadCurrentSchedule()
    }

    private fun setupTreatments() {
        val treatmentsArray = resources.getStringArray(R.array.treatments_array)
        for (treatment in treatmentsArray) {
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = treatment
                isCheckable = true
                tag = treatment
            }
            binding.cgTreatments.addView(chip)
        }
    }

    private fun setupTimePickers() {
        binding.etStartTime.setOnClickListener {
            showTimePicker { hour, minute ->
                binding.etStartTime.setText(String.format("%02d:%02d", hour, minute))
            }
        }
        binding.etEndTime.setOnClickListener {
            showTimePicker { hour, minute ->
                binding.etEndTime.setText(String.format("%02d:%02d", hour, minute))
            }
        }
    }

    private fun showTimePicker(onTimeSelected: (Int, Int) -> Unit) {
        val picker = TimePickerDialog(requireContext(), { _, h, m ->
            onTimeSelected(h, m)
        }, 9, 0, true)
        picker.show()
    }

    private fun setupSaveButton() {
        binding.btnSaveSchedule.setOnClickListener {
            saveSchedule()
        }
    }

    private fun loadCurrentSchedule() {
        val uid = auth.currentUser?.uid ?: return
        binding.pbSaving.visibility = View.VISIBLE
        
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                binding.pbSaving.visibility = View.GONE
                if (doc.exists()) {
                    val schedule = doc.get("schedule") as? Map<*, *> ?: return@addOnSuccessListener
                    
                    // Treatments
                    val selectedTreatments = (schedule["treatments"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    for (i in 0 until binding.cgTreatments.childCount) {
                        val chip = binding.cgTreatments.getChildAt(i) as com.google.android.material.chip.Chip
                        chip.isChecked = selectedTreatments.contains(chip.tag as String)
                    }
                    
                    // Days
                    val days = (schedule["workDays"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    binding.chipMon.isChecked = days.contains("Mon")
                    binding.chipTue.isChecked = days.contains("Tue")
                    binding.chipWed.isChecked = days.contains("Wed")
                    binding.chipThu.isChecked = days.contains("Thu")
                    binding.chipFri.isChecked = days.contains("Fri")
                    binding.chipSat.isChecked = days.contains("Sat")
                    binding.chipSun.isChecked = days.contains("Sun")
                    
                    // Hours
                    binding.etStartTime.setText(schedule["startHour"] as? String ?: "09:00")
                    binding.etEndTime.setText(schedule["endHour"] as? String ?: "21:00")
                    
                    // Duration
                    val duration = (schedule["duration"] as? Number)?.toInt() ?: 60
                    when (duration) {
                        30 -> binding.chip30min.isChecked = true
                        45 -> binding.chip45min.isChecked = true
                        60 -> binding.chip60min.isChecked = true
                    }
                }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.pbSaving.visibility = View.GONE
            }
    }

    private fun saveSchedule() {
        val uid = auth.currentUser?.uid ?: return
        
        val selectedDays = mutableListOf<String>()
        if (binding.chipMon.isChecked) selectedDays.add("Mon")
        if (binding.chipTue.isChecked) selectedDays.add("Tue")
        if (binding.chipWed.isChecked) selectedDays.add("Wed")
        if (binding.chipThu.isChecked) selectedDays.add("Thu")
        if (binding.chipFri.isChecked) selectedDays.add("Fri")
        if (binding.chipSat.isChecked) selectedDays.add("Sat")
        if (binding.chipSun.isChecked) selectedDays.add("Sun")
        
        val duration = when {
            binding.chip30min.isChecked -> 30
            binding.chip45min.isChecked -> 45
            else -> 60
        }
        
        val selectedTreatments = mutableListOf<String>()
        for (i in 0 until binding.cgTreatments.childCount) {
            val chip = binding.cgTreatments.getChildAt(i) as com.google.android.material.chip.Chip
            if (chip.isChecked) {
                selectedTreatments.add(chip.tag as String)
            }
        }
        
        if (selectedTreatments.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.selecciona_tratamiento_error), Toast.LENGTH_SHORT).show()
            return
        }
        
        val scheduleMap = mapOf(
            "workDays" to selectedDays,
            "startHour" to binding.etStartTime.text.toString(),
            "endHour" to binding.etEndTime.text.toString(),
            "duration" to duration,
            "treatments" to selectedTreatments
        )
        
        binding.pbSaving.visibility = View.VISIBLE
        binding.btnSaveSchedule.isEnabled = false
        
        db.collection("users").document(uid).update("schedule", scheduleMap)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                binding.pbSaving.visibility = View.GONE
                binding.btnSaveSchedule.isEnabled = true
                Toast.makeText(requireContext(), getString(R.string.horario_guardado), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.pbSaving.visibility = View.GONE
                binding.btnSaveSchedule.isEnabled = true
                Toast.makeText(requireContext(), getString(R.string.error_generico), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

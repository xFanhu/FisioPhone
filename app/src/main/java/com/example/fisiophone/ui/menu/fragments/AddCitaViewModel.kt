package com.example.fisiophone.ui.menu.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fisiophone.ui.menu.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddCitaViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _physios = MutableStateFlow<List<User>>(emptyList())
    val physios: StateFlow<List<User>> = _physios.asStateFlow()

    private val _availableSlots = MutableStateFlow<List<String>>(emptyList())
    val availableSlots: StateFlow<List<String>> = _availableSlots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookingResult = MutableSharedFlow<Result<Unit>>()
    val bookingResult: SharedFlow<Result<Unit>> = _bookingResult.asSharedFlow()

    var selectedPhysio: User? = null
        private set
    var selectedDate: Date? = null
        private set
    var selectedTime: String? = null
        private set
    var selectedTreatment: String? = null
        private set

    init {
        fetchPhysios()
    }

    private fun fetchPhysios() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = db.collection("users").whereEqualTo("role", "fisioterapeuta").get().await()
                val list = result.documents.map { doc ->
                    User(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        dni = doc.getString("dni") ?: "",
                        telefono = doc.getString("telefono") ?: "",
                        role = "fisioterapeuta",
                        treatments = ((doc.get("schedule") as? Map<*, *>)?.get("treatments") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                }
                _physios.value = list
            } catch (e: Exception) {
                // Ignore for now or handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPhysio(physio: User) {
        selectedPhysio = physio
        selectedTreatment = null
        selectedDate = null
        selectedTime = null
        _availableSlots.value = emptyList()
    }
    
    fun selectTreatment(treatment: String) {
        selectedTreatment = treatment
        selectedDate = null
        selectedTime = null
        _availableSlots.value = emptyList()
    }

    fun selectDate(date: Date) {
        if (!isWorkingDay(date)) {
            viewModelScope.launch {
                _bookingResult.emit(Result.failure(Exception("NO_WORKING_DAY")))
            }
            return
        }
        selectedDate = date
        selectedTime = null
        fetchAvailableSlots(date)
    }
    
    fun selectTime(time: String) {
        selectedTime = time
    }

    private fun isWorkingDay(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY
    }

    private fun fetchAvailableSlots(date: Date) {
        val physioId = selectedPhysio?.id ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(date)
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Fetch physio schedule
                val doc = db.collection("users").document(physioId).get().await()
                val schedule = doc.get("schedule") as? Map<*, *>
                if (schedule == null) {
                    _bookingResult.emit(Result.failure(Exception("NO_SCHEDULE")))
                    return@launch
                }

                val startStr = schedule["startHour"] as? String ?: "09:00"
                val endStr = schedule["endHour"] as? String ?: "21:00"
                val duration = (schedule["duration"] as? Number)?.toInt() ?: 60
                
                // 2. Fetch existing appointments
                val appointments = db.collection("citas")
                    .whereEqualTo("physioId", physioId)
                    .whereEqualTo("date", dateStr)
                    .get()
                    .await()
                    
                val bookedTimes = appointments.mapNotNull { it.getString("time") }
                
                // 3. Generate slots
                val slots = generateSlots(startStr, endStr, duration, bookedTimes)
                _availableSlots.value = slots

            } catch (e: Exception) {
                _bookingResult.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun generateSlots(start: String, end: String, duration: Int, booked: List<String>): List<String> {
        val startMins = timeToMins(start)
        val endMins = timeToMins(end)
        
        val slots = mutableListOf<String>()
        var current = startMins
        while (current + duration <= endMins) {
            val timeStr = minsToTime(current)
            if (!booked.contains(timeStr)) {
                slots.add(timeStr)
            }
            current += duration
        }
        return slots
    }

    private fun timeToMins(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun minsToTime(mins: Int): String {
        return String.format("%02d:%02d", mins / 60, mins % 60)
    }

    fun confirmBooking() {
        val patientId = auth.currentUser?.uid ?: return
        val physio = selectedPhysio ?: return
        val date = selectedDate ?: return
        val timeStr = selectedTime ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                
                // Fetch patient name
                val patientDoc = db.collection("users").document(patientId).get().await()
                val patientName = if (patientDoc.exists()) {
                    "${patientDoc.getString("nombre")} ${patientDoc.getString("apellidos")}"
                } else {
                    "Paciente"
                }

                val cita = mapOf(
                    "patientId" to patientId,
                    "physioId" to physio.id,
                    "physioName" to "${physio.nombre} ${physio.apellidos}",
                    "patientName" to patientName,
                    "date" to dateStr,
                    "time" to timeStr,
                    "status" to "booked",
                    "tratamiento" to (selectedTreatment ?: "Sesión de Fisioterapia")
                )
                
                db.collection("citas").add(cita).await()
                _bookingResult.emit(Result.success(Unit))
            } catch (e: Exception) {
                _bookingResult.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }
}

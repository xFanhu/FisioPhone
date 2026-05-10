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

class AddCitaFisioViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    private val _patients = MutableStateFlow<List<User>>(emptyList())
    val patients: StateFlow<List<User>> = _patients.asStateFlow()

    private val _physios = MutableStateFlow<List<User>>(emptyList())
    val physios: StateFlow<List<User>> = _physios.asStateFlow()

    private val _availableSlots = MutableStateFlow<List<String>?>(null)
    val availableSlots: StateFlow<List<String>?> = _availableSlots.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookingResult = MutableSharedFlow<Result<Unit>>()
    val bookingResult: SharedFlow<Result<Unit>> = _bookingResult.asSharedFlow()

    private val _workDays = MutableStateFlow<List<String>>(emptyList())
    val workDays: StateFlow<List<String>> = _workDays.asStateFlow()



    var selectedPatient: User? = null
        private set
    var selectedPhysio: User? = null
        private set
    var selectedDate: Date? = null
        private set
    var selectedTime: String? = null
        private set
    var selectedTreatment: String? = null
        private set


    var isEditMode = false
        private set
    var editCitaId: String? = null
        private set


    var currentUserRole: String = "fisioterapeuta"
        private set

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {

                val uid = auth.currentUser?.uid ?: return@launch
                val userDoc = db.collection("users").document(uid).get().await()
                currentUserRole = userDoc.getString("role") ?: "fisioterapeuta"


                val patientsResult = db.collection("users")
                    .whereEqualTo("role", "paciente")
                    .get().await()
                
                val patientsList = patientsResult.documents.map { doc ->
                    User(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        dni = doc.getString("dni") ?: "",
                        telefono = doc.getString("telefono") ?: "",
                        role = "paciente"
                    )
                }.sortedBy { it.nombre }
                _patients.value = patientsList


                val physiosResult = db.collection("users")
                    .whereIn("role", listOf("fisioterapeuta", "administrador"))
                    .get().await()
                
                val physiosList = physiosResult.documents.map { doc ->
                    User(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        dni = doc.getString("dni") ?: "",
                        telefono = doc.getString("telefono") ?: "",
                        role = doc.getString("role") ?: "fisioterapeuta",
                        treatments = ((doc.get("schedule") as? Map<*, *>)?.get("treatments") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                }
                _physios.value = physiosList


                if (currentUserRole == "fisioterapeuta") {
                    val physio = physiosList.find { it.id == uid }
                    if (physio != null) selectPhysio(physio)
                }


            } catch (e: Exception) {

            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setupEditMode(citaId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = db.collection("citas").document(citaId).get().await()
                if (doc.exists()) {
                    isEditMode = true
                    editCitaId = citaId
                    
                    val patientId = doc.getString("patientId") ?: ""
                    val physioId = doc.getString("physioId") ?: ""
                    val dateStr = doc.getString("date") ?: ""
                    val timeStr = doc.getString("time") ?: ""
                    val treatment = doc.getString("tratamiento") ?: ""
                    

                    while (_patients.value.isEmpty() || _physios.value.isEmpty()) {
                        kotlinx.coroutines.delay(100)
                    }

                    selectedPatient = _patients.value.find { it.id == patientId }
                    selectedPhysio = _physios.value.find { it.id == physioId }
                    selectedTreatment = treatment
                    selectedTime = null

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    try {
                        selectedDate = sdf.parse(dateStr)
                        selectedDate?.let { fetchAvailableSlots(it, skipCurrentTimeCheck = true) } // In edit mode, we might keep the same time even if "passed" visually if it's today
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPatient(patient: User) {
        selectedPatient = patient
    }

    fun selectPhysio(physio: User) {
        selectedPhysio = physio
        selectedTreatment = null
        selectedDate = null
        selectedTime = null
        _availableSlots.value = null
        fetchPhysioScheduleDays(physio.id)
    }

    private fun fetchPhysioScheduleDays(physioId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(physioId).get().await()
                val schedule = doc.get("schedule") as? Map<*, *>
                val days = (schedule?.get("workDays") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                _workDays.value = days
            } catch (e: Exception) {
                _workDays.value = emptyList()
            }
        }
    }


    fun selectTreatment(treatment: String) {
        selectedTreatment = treatment
        selectedTime = null
        _availableSlots.value = null
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
        _availableSlots.value = null
        fetchAvailableSlots(date, skipCurrentTimeCheck = false)
    }

    fun selectTime(time: String) {
        selectedTime = time
    }

    private fun isWorkingDay(date: Date): Boolean {
        val calendar = Calendar.getInstance().apply { time = date }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val dayStr = when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> ""
        }
        
        return _workDays.value.isEmpty() || _workDays.value.contains(dayStr)
    }


    fun fetchAvailableSlots(date: Date, skipCurrentTimeCheck: Boolean) {
        val physioId = selectedPhysio?.id ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(date)
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val doc = db.collection("users").document(physioId).get().await()
                val schedule = doc.get("schedule") as? Map<*, *>
                if (schedule == null) {
                    _bookingResult.emit(Result.failure(Exception("NO_SCHEDULE")))
                    return@launch
                }

                val startStr = schedule["startHour"] as? String ?: "09:00"
                val endStr = schedule["endHour"] as? String ?: "21:00"
                val duration = (schedule["duration"] as? Number)?.toInt() ?: 60
                
                val appointments = db.collection("citas")
                    .whereEqualTo("physioId", physioId)
                    .whereEqualTo("date", dateStr)
                    .get()
                    .await()
                    
                val bookedTimes = appointments.mapNotNull {

                    if (isEditMode && it.id == editCitaId) null else it.getString("time")
                }
                
                val isToday = !skipCurrentTimeCheck && dateStr == sdf.format(Date())
                val slots = generateSlots(startStr, endStr, duration, bookedTimes, isToday)
                _availableSlots.value = slots

            } catch (e: Exception) {
                _bookingResult.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun generateSlots(start: String, end: String, duration: Int, booked: List<String>, isToday: Boolean): List<String> {
        val startMins = timeToMins(start)
        val endMins = timeToMins(end)
        
        val nowMins = if (isToday) {
            val cal = Calendar.getInstance()
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } else { -1 }
        
        val slots = mutableListOf<String>()
        var current = startMins
        while (current + duration <= endMins) {
            val timeStr = minsToTime(current)
            if (!booked.contains(timeStr) && current > nowMins) {
                slots.add(timeStr)
            }
            current += duration
        }
        return slots
    }

    private fun timeToMins(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) return 0
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun minsToTime(mins: Int): String {
        return String.format("%02d:%02d", mins / 60, mins % 60)
    }

    fun confirmBooking() {
        val patient = selectedPatient ?: return
        val physio = selectedPhysio ?: return
        val date = selectedDate ?: return
        val timeStr = selectedTime ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val patientName = "${patient.nombre} ${patient.apellidos}"
                val physioName = "${physio.nombre} ${physio.apellidos}"

                val citaData = mutableMapOf<String, Any>(
                    "patientId" to patient.id,
                    "physioId" to physio.id,
                    "physioName" to physioName,
                    "patientName" to patientName,
                    "date" to dateStr,
                    "time" to timeStr,
                    "status" to "booked",
                    "tratamiento" to (selectedTreatment ?: "Sesión de Fisioterapia")
                )
                
                if (isEditMode && editCitaId != null) {
                    db.collection("citas").document(editCitaId!!).update(citaData).await()
                } else {
                    db.collection("citas").add(citaData).await()
                }
                _bookingResult.emit(Result.success(Unit))
            } catch (e: Exception) {
                _bookingResult.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }
}

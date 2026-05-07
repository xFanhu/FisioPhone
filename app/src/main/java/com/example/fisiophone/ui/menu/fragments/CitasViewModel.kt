package com.example.fisiophone.ui.menu.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.Date
import java.util.Locale

/**
 * ViewModel que gestiona la lógica de obtención y filtrado de citas.
 */
class CitasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estados reactivos (StateFlow)
    private val _citas = MutableStateFlow<List<Cita>>(emptyList())
    val citas: StateFlow<List<Cita>> = _citas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPatient = MutableStateFlow(true)
    val isPatient: StateFlow<Boolean> = _isPatient.asStateFlow()

    private val _messages = MutableSharedFlow<Result<String>>()
    val messages: SharedFlow<Result<String>> = _messages.asSharedFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    init {
        // Por defecto cargamos el día de hoy
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _selectedDate.value = sdf.format(Date())
        fetchData()
    }

    fun updateSelectedDate(date: Date) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _selectedDate.value = sdf.format(date)
        fetchData()
    }

    /**
     * Obtiene las citas de Firestore filtrando por rol y fecha si aplica.
     */
    private fun fetchData() {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Obtener rol del usuario actual
                val userDoc = db.collection("users").document(uid).get().await()
                val role = userDoc.getString("role")?.lowercase() ?: "paciente"
                val patientFlag = role == "paciente"
                _isPatient.value = patientFlag

                // 2. Construir query base
                val field = if (patientFlag) "patientId" else "physioId"
                var query: com.google.firebase.firestore.Query = db.collection("citas")
                    .whereEqualTo(field, uid)

                if (patientFlag) {
                    // El paciente solo quiere ver las citas activas (reservadas)
                    query = query.whereEqualTo("status", "booked")
                } else {
                    // El fisio ve las del día (reservadas o ya hechas) si está filtrando por fecha
                    if (_selectedDate.value != null) {
                        query = query.whereEqualTo("date", _selectedDate.value!!)
                    }
                }

                val result = query.get().await()

                // Mapear documentos a objetos Cita
                val list = result.documents.map { doc ->
                    Cita(
                        id = doc.id,
                        patientId = doc.getString("patientId") ?: "",
                        physioId = doc.getString("physioId") ?: "",
                        patientName = doc.getString("patientName") ?: "Paciente",
                        physioName = doc.getString("physioName") ?: "Fisioterapeuta",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        status = doc.getString("status") ?: "booked",
                        tratamiento = doc.getString("tratamiento") ?: "Sesión de Fisioterapia"
                    )
                }

                // 3. Filtrado adicional y ordenación en memoria
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val now = sdf.format(Date())

                val filteredList = if (patientFlag) {
                    // El PACIENTE solo ve sus citas futuras para no estorbar
                    list.filter { cita -> "${cita.date} ${cita.time}" >= now }
                } else {
                    // El FISIO ve todo el planning del día seleccionado
                    list
                }

                // Ordenar por fecha y luego por hora
                val sortedCitas = filteredList.sortedWith(compareBy({ it.date }, { it.time }))
                _citas.value = sortedCitas

            } catch (e: Exception) {
                _messages.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Marca una cita como realizada.
     */
    fun completeCita(cita: Cita) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("citas").document(cita.id).update("status", "done").await()
                _messages.emit(Result.success("COMPLETED|${cita.id}")) // Enviamos ID para saber cuál se ha completado
                fetchData() // Recargar lista
            } catch (e: Exception) {
                _messages.emit(Result.failure(e))
                _isLoading.value = false
            }
        }
    }

    /**
     * Guarda una anotación en la historia clínica del paciente.
     */
    fun saveClinicalNote(cita: Cita, note: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val physioDoc = db.collection("users").document(cita.physioId).get().await()
                val physioName = "${physioDoc.getString("nombre") ?: ""} ${physioDoc.getString("apellidos") ?: ""}".trim()
                
                val noteMap = mapOf(
                    "physioId" to cita.physioId,
                    "physioName" to physioName.ifEmpty { "Fisioterapeuta" },
                    "date" to SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                    "timestamp" to System.currentTimeMillis(),
                    "note" to note
                )
                
                db.collection("users").document(cita.patientId)
                    .collection("historias_clinicas")
                    .add(noteMap)
                    .await()
                
                _messages.emit(Result.success("NOTE_SAVED"))
            } catch (e: Exception) {
                _messages.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina una cita.
     */
    fun deleteCita(cita: Cita) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("citas").document(cita.id).delete().await()
                _messages.emit(Result.success("DELETED"))
                fetchData() // Recargar para actualizar lista
            } catch (e: Exception) {
                _messages.emit(Result.failure(e))
                _isLoading.value = false
            }
        }
    }
}

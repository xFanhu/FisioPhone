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

class CitasViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _citas = MutableStateFlow<List<Cita>>(emptyList())
    val citas: StateFlow<List<Cita>> = _citas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPatient = MutableStateFlow(true)
    val isPatient: StateFlow<Boolean> = _isPatient.asStateFlow()

    private val _messages = MutableSharedFlow<Result<String>>()
    val messages: SharedFlow<Result<String>> = _messages.asSharedFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Obtener rol
                val userDoc = db.collection("users").document(uid).get().await()
                val role = userDoc.getString("role")?.lowercase() ?: "paciente"
                val patientFlag = role == "paciente"
                _isPatient.value = patientFlag

                // 2. Obtener citas
                val field = if (patientFlag) "patientId" else "physioId"
                val result = db.collection("citas")
                    .whereEqualTo(field, uid)
                    .get()
                    .await()

                val list = result.documents.map { doc ->
                    Cita(
                        id = doc.id,
                        patientId = doc.getString("patientId") ?: "",
                        physioId = doc.getString("physioId") ?: "",
                        patientName = doc.getString("patientName") ?: "Paciente",
                        physioName = doc.getString("physioName") ?: "Fisioterapeuta",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        status = doc.getString("status") ?: "",
                        tratamiento = doc.getString("tratamiento") ?: "Sesión de Fisioterapia"
                    )
                }

                // 3. Ordenar en memoria
                val sortedCitas = list.sortedWith(compareBy({ it.date }, { it.time }))
                _citas.value = sortedCitas

            } catch (e: Exception) {
                _messages.emit(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCita(cita: Cita) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection("citas").document(cita.id).delete().await()
                _messages.emit(Result.success("DELETED"))
                // Recargar citas tras el borrado exitoso
                fetchData()
            } catch (e: Exception) {
                _messages.emit(Result.failure(e))
                _isLoading.value = false // Solo paramos el loading si falla, si no lo hace fetchData()
            }
        }
    }
}

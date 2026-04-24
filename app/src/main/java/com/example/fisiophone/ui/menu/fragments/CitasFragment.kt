package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentCitasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CitasFragment : Fragment() {

    private var _binding: FragmentCitasBinding? = null
    private val binding get() = _binding!!
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
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
        fetchUserRoleAndCitas()
    }

    private fun setupRecyclerView() {
        binding.rvCitas.layoutManager = LinearLayoutManager(requireContext())
        // Initial dummy state
        citaAdapter = CitaAdapter(emptyList(), true) { cita ->
            deleteCita(cita)
        }
        binding.rvCitas.adapter = citaAdapter
    }

    private fun fetchUserRoleAndCitas() {
        val uid = auth.currentUser?.uid ?: return
        binding.pbCitas.visibility = View.VISIBLE
        
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                
                val role = doc.getString("role")?.lowercase() ?: "paciente"
                val isPatient = role == "paciente"
                
                // Update adapter with correct label
                citaAdapter = CitaAdapter(emptyList(), isPatient) { cita ->
                    deleteCita(cita)
                }
                binding.rvCitas.adapter = citaAdapter
                
                fetchCitas(uid, isPatient)
            }
    }

    private fun fetchCitas(uid: String, isPatient: Boolean) {
        val field = if (isPatient) "patientId" else "physioId"
        
        db.collection("citas")
            .whereEqualTo(field, uid)
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                
                binding.pbCitas.visibility = View.GONE
                val citas = mutableListOf<Cita>()
                for (doc in result) {
                    val cita = Cita(
                        id = doc.id,
                        patientId = doc.getString("patientId") ?: "",
                        physioId = doc.getString("physioId") ?: "",
                        patientName = doc.getString("patientName") ?: "Paciente",
                        physioName = doc.getString("physioName") ?: "Fisioterapeuta",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                    citas.add(cita)
                }
                
                // Sort in memory to avoid Firestore index requirement
                val sortedCitas = citas.sortedWith(compareBy({ it.date }, { it.time }))
                
                if (sortedCitas.isEmpty()) {
                    binding.tvNoCitas.visibility = View.VISIBLE
                    citaAdapter.updateList(emptyList())
                } else {
                    binding.tvNoCitas.visibility = View.GONE
                    citaAdapter.updateList(sortedCitas)
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.pbCitas.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteCita(cita: Cita) {
        db.collection("citas").document(cita.id).delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.cita_cancelada), Toast.LENGTH_SHORT).show()
                fetchUserRoleAndCitas() // Refresh
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

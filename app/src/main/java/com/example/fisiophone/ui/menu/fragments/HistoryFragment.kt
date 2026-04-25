package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: HistoriaAdapter
    private var patientId: String? = null
    private var isOwnProfile: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        patientId = arguments?.getString(ARG_USER_ID)
        isOwnProfile = arguments?.getBoolean(ARG_IS_OWN) ?: false

        setupRecyclerView()
        setupButtons()
        
        if (patientId != null) {
            fetchHistory()
        }
    }

    private fun setupRecyclerView() {
        adapter = HistoriaAdapter(emptyList())
        binding.rvHistoria.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Si es el propio perfil del paciente, ocultamos el botón
        if (isOwnProfile) {
            binding.fabAddHistory.visibility = View.GONE
        } else {
            binding.fabAddHistory.visibility = View.VISIBLE
            binding.fabAddHistory.setOnClickListener {
                showAddHistoryDialog()
            }
        }
    }

    private fun showAddHistoryDialog() {
        val context = requireContext()
        val input = TextInputEditText(context).apply {
            hint = context.getString(R.string.escribe_detalles)
            setLines(5)
            maxLines = 10
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
        }
        
        // Wrap with some padding
        val container = android.widget.FrameLayout(context)
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(
                (24 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt()
            )
        }
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.nueva_anotacion)
            .setView(container)
            .setPositiveButton(R.string.guardar_historia) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    saveHistoryNote(text)
                } else {
                    Toast.makeText(context, "La nota no puede estar vacía", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun saveHistoryNote(note: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val targetUid = patientId ?: return

        db.collection("users").document(currentUid).get().addOnSuccessListener { doc ->
            val physioName = "${doc.getString("nombre") ?: ""} ${doc.getString("apellidos") ?: ""}".trim()
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            
            val historiaMap = mapOf(
                "physioId" to currentUid,
                "physioName" to physioName.ifEmpty { "Fisioterapeuta" },
                "date" to dateStr,
                "timestamp" to System.currentTimeMillis(),
                "note" to note
            )

            db.collection("users").document(targetUid)
                .collection("historias_clinicas")
                .add(historiaMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Anotación guardada", Toast.LENGTH_SHORT).show()
                    fetchHistory() // Recargar lista
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchHistory() {
        val targetUid = patientId ?: return
        
        db.collection("users").document(targetUid)
            .collection("historias_clinicas")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener
                
                val list = mutableListOf<HistoriaClinica>()
                for (doc in documents) {
                    list.add(HistoriaClinica(
                        id = doc.id,
                        physioName = doc.getString("physioName") ?: "Fisioterapeuta",
                        date = doc.getString("date") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        note = doc.getString("note") ?: ""
                    ))
                }
                
                adapter.updateList(list)
                
                if (list.isEmpty()) {
                    binding.rvHistoria.visibility = View.GONE
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                } else {
                    binding.rvHistoria.visibility = View.VISIBLE
                    binding.tvEmptyHistory.visibility = View.GONE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_ID = "arg_user_id"
        private const val ARG_IS_OWN = "arg_is_own"

        fun newInstance(userId: String, isOwnProfile: Boolean): HistoryFragment {
            return HistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putBoolean(ARG_IS_OWN, isOwnProfile)
                }
            }
        }
    }
}

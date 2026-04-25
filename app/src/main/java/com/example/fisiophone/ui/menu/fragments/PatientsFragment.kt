package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fisiophone.databinding.FragmentPatientsBinding
import com.example.fisiophone.ui.menu.User
import com.example.fisiophone.ui.menu.UserAdapter
import com.google.firebase.firestore.FirebaseFirestore

class PatientsFragment : Fragment() {

    private var _binding: FragmentPatientsBinding? = null
    private val binding get() = _binding!!
    
    private val db = FirebaseFirestore.getInstance()
    private var allPatients = mutableListOf<User>()
    private lateinit var adapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        fetchPatients()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(emptyList(), showActions = false, onItemClick = { user ->
            val profileFragment = ProfileFragment.newInstance(
                ProfileFragment.UserRole.fromValue(user.role),
                user.id
            )
            parentFragmentManager.beginTransaction()
                .replace(com.example.fisiophone.R.id.fragmentHost, profileFragment)
                .addToBackStack(ProfileFragment.TAG)
                .commit()
        })
        binding.rvPatients.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPatients.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterPatients(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPatients(newText)
                return true
            }
        })
    }

    private fun fetchPatients() {
        binding.pbLoading.visibility = View.VISIBLE
        db.collection("users")
            .whereEqualTo("role", "paciente")
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener
                
                binding.pbLoading.visibility = View.GONE
                allPatients.clear()
                for (doc in documents) {
                    val user = User(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        apellidos = doc.getString("apellidos") ?: "",
                        email = doc.getString("email") ?: "",
                        dni = doc.getString("dni") ?: "",
                        telefono = doc.getString("telefono") ?: "",
                        role = doc.getString("role") ?: "paciente"
                    )
                    allPatients.add(user)
                }
                allPatients.sortBy { it.nombre }
                adapter.updateList(allPatients)
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.pbLoading.visibility = View.GONE
            }
    }

    private fun filterPatients(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.updateList(allPatients)
            return
        }
        val lowerQuery = query.lowercase()
        val filtered = allPatients.filter {
            it.nombre.lowercase().contains(lowerQuery) ||
            it.apellidos.lowercase().contains(lowerQuery) ||
            it.email.lowercase().contains(lowerQuery) ||
            it.dni.lowercase().contains(lowerQuery)
        }
        adapter.updateList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.fisiophone.ui.menu.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
import com.example.fisiophone.ui.menu.ManageUsersActivity
import com.example.fisiophone.ui.menu.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeamFragment : Fragment() {

    private var _binding: com.example.fisiophone.databinding.FragmentTeamBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var teamAdapter: TeamAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var currentUserRole: String = "paciente"
    private var teamList = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = com.example.fisiophone.databinding.FragmentTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTeamMembers.layoutManager = LinearLayoutManager(requireContext())
        teamAdapter = TeamAdapter(emptyList()) { user ->
            val profileFragment = ProfileFragment.newInstance(
                ProfileFragment.UserRole.fromValue(user.role),
                user.id
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentHost, profileFragment)
                .addToBackStack(ProfileFragment.TAG)
                .commit()
        }
        binding.rvTeamMembers.adapter = teamAdapter

        setupFab()
        checkUserRole()
        fetchTeamMembers()
    }

    private fun setupFab() {
        binding.fabAddPhysio.setOnClickListener {
            val intent = Intent(requireContext(), ManageUsersActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkUserRole() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                if (document != null && document.exists()) {
                    currentUserRole = document.getString("role") ?: "paciente"
                    if (currentUserRole == "administrador") {
                        binding.fabAddPhysio.visibility = View.VISIBLE
                    } else {
                        binding.fabAddPhysio.visibility = View.GONE
                    }
                }
            }
    }

    private fun fetchTeamMembers() {
        db.collection("users")
            .whereEqualTo("role", "fisioterapeuta")
            .get()
            .addOnSuccessListener { result ->
                if (_binding == null) return@addOnSuccessListener
                teamList.clear()
                for (document in result) {
                    val user = User(
                        id = document.id,
                        nombre = document.getString("nombre") ?: "",
                        apellidos = document.getString("apellidos") ?: "",
                        email = document.getString("email") ?: "",
                        dni = document.getString("dni") ?: "",
                        telefono = document.getString("telefono") ?: "",
                        role = document.getString("role") ?: "fisioterapeuta"
                    )
                    teamList.add(user)
                }
                teamList.sortBy { it.nombre }
                teamAdapter.updateList(teamList)
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), getString(R.string.error_equipo), Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

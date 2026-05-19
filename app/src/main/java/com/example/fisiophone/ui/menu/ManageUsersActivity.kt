package com.example.fisiophone.ui.menu

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.databinding.ActivityManageUsersBinding
import com.example.fisiophone.R
import com.example.fisiophone.ui.menu.User
import com.example.fisiophone.ui.menu.UserAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var userAdapter: UserAdapter
    private val db = FirebaseFirestore.getInstance()
    
    private var allUsers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList(), onRoleChangeClick = { userToChange ->
            changeUserRole(userToChange)
        })
        binding.recyclerViewUsers.adapter = userAdapter

        setupSearchView()
        fetchUsers()
    }

    private fun fetchUsers() {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                allUsers.clear()
                for (document in result) {
                    val role = document.getString("role") ?: "paciente"
                    val user = User(
                        id = document.id,
                        nombre = document.getString("nombre") ?: "",
                        apellidos = document.getString("apellidos") ?: "",
                        email = document.getString("email") ?: "",
                        dni = document.getString("dni") ?: "",
                        telefono = document.getString("telefono") ?: "",
                        role = role,
                        photoUrl = document.getString("photoUrl")
                    )
                    allUsers.add(user)
                }
                allUsers.sortBy { it.nombre }
                userAdapter.updateList(allUsers)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_generico), Toast.LENGTH_SHORT).show()
            }
    }

    //Buscador
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterList(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    private fun filterList(query: String?) {
        if (query.isNullOrBlank()) {
            userAdapter.updateList(allUsers)
            return
        }

        val lowerQuery = query.lowercase()
        val filteredList = allUsers.filter { user ->
            user.nombre.lowercase().contains(lowerQuery) ||
            user.apellidos.lowercase().contains(lowerQuery) ||
            user.email.lowercase().contains(lowerQuery) ||
            user.dni.lowercase().contains(lowerQuery)
        }
        userAdapter.updateList(filteredList)
    }

    private fun changeUserRole(user: User) {
        val newRole = if (user.role == "fisioterapeuta") "paciente" else "fisioterapeuta"
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirmar_cambio_rol_titulo))
            .setMessage(getString(R.string.confirmar_cambio_rol_mensaje, newRole))
            .setPositiveButton(getString(R.string.si)) { dialog, _ ->
                dialog.dismiss()
                db.collection("users").document(user.id)
                    .update("role", newRole)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.rol_actualizado, newRole), Toast.LENGTH_SHORT).show()

                        val index = allUsers.indexOfFirst { it.id == user.id }
                        if (index != -1) {
                            allUsers[index].role = newRole
                            filterList(binding.searchView.query.toString())
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, getString(R.string.error_actualizando_rol), Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

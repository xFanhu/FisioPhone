package com.example.fisiophone.ui.menu

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var userAdapter: UserAdapter
    private val db = FirebaseFirestore.getInstance()
    
    private var allUsers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        topAppBar = findViewById(R.id.topAppBar)
        searchView = findViewById(R.id.searchView)
        recyclerView = findViewById(R.id.recyclerViewUsers)

        topAppBar.setNavigationOnClickListener {
            finish()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList(), onRoleChangeClick = { userToChange ->
            changeUserRole(userToChange)
        })
        recyclerView.adapter = userAdapter

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
                        role = role
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

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        
        db.collection("users").document(user.id)
            .update("role", newRole)
            .addOnSuccessListener {
                Toast.makeText(this, "Rol actualizado a $newRole", Toast.LENGTH_SHORT).show()
                // Update local list to reflect changes immediately
                val index = allUsers.indexOfFirst { it.id == user.id }
                if (index != -1) {
                    allUsers[index].role = newRole
                    filterList(searchView.query.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error actualizando rol", Toast.LENGTH_SHORT).show()
            }
    }
}

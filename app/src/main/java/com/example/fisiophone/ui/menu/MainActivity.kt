package com.example.fisiophone.ui.menu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fisiophone.R
import com.example.fisiophone.databinding.ActivityMainBinding
import com.example.fisiophone.ui.menu.fragments.ConfigFragment
import com.example.fisiophone.ui.menu.fragments.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userRole: String = "paciente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNavigation()
        fetchUserRole()
        
        // Show Home/Citas by default
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_citas
        }
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
            showProfileFragment()
        }
        
        binding.topAppBar.inflateMenu(R.menu.top_app_bar_menu)
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    auth.signOut()
                    val intent = android.content.Intent(this@MainActivity, com.example.fisiophone.ui.auth.LogInActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_citas -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentHost.id, com.example.fisiophone.ui.menu.fragments.CitasFragment())
                        .commit()
                    true
                }
                R.id.nav_equipo -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentHost.id, com.example.fisiophone.ui.menu.fragments.TeamFragment())
                        .commit()
                    true
                }
                R.id.nav_pacientes -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentHost.id, com.example.fisiophone.ui.menu.fragments.PatientsFragment())
                        .commit()
                    true
                }
                R.id.nav_add -> {
                    val fragment = if (userRole == "fisioterapeuta" || userRole == "administrador") {
                        com.example.fisiophone.ui.menu.fragments.ScheduleFragment()
                    } else {
                        com.example.fisiophone.ui.menu.fragments.AddCitaFragment()
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentHost.id, fragment)
                        .commit()
                    true
                }
                R.id.nav_ajustes -> {
                    showConfigFragment()
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchUserRole() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userRole = document.getString("role") ?: "paciente"
                    updateMenuVisibility()
                }
            }
    }

    private fun updateMenuVisibility() {
        val menu = binding.bottomNavigation.menu
        val teamItem = menu.findItem(R.id.nav_equipo)
        val addItem = menu.findItem(R.id.nav_add)
        val patientsItem = menu.findItem(R.id.nav_pacientes)
        
        if (userRole == "fisioterapeuta" || userRole == "administrador") {
            teamItem?.isVisible = true
            patientsItem?.isVisible = true
            addItem?.title = "Horario"
            addItem?.setIcon(R.drawable.ic_citas)
        } else {
            teamItem?.isVisible = false
            patientsItem?.isVisible = false
            addItem?.title = "Nueva Cita"
            addItem?.setIcon(R.drawable.ic_add)
        }
    }

    private fun showConfigFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentHost.id, ConfigFragment())
            .addToBackStack(ConfigFragment.TAG)
            .commit()
    }

    private fun showProfileFragment() {
        val roleEnum = if (userRole == "paciente") {
            ProfileFragment.UserRole.PATIENT
        } else {
            ProfileFragment.UserRole.PHYSIOTHERAPIST
        }
        
        supportFragmentManager.beginTransaction()
            .replace(
                binding.fragmentHost.id,
                ProfileFragment.newInstance(roleEnum)
            )
            .addToBackStack(ProfileFragment.TAG)
            .commit()
    }
}

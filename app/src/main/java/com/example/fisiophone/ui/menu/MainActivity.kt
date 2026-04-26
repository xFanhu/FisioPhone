package com.example.fisiophone.ui.menu

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fisiophone.R
import com.example.fisiophone.databinding.ActivityMainBinding
import com.example.fisiophone.ui.menu.fragments.ConfigFragment
import com.example.fisiophone.ui.menu.fragments.ProfileFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import com.example.fisiophone.notifications.NotificationHelper
import com.example.fisiophone.data.settings.SettingsManager
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userRole: String = "paciente"
    private var appointmentsListener: ListenerRegistration? = null
    private var isInitialLoad = true

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
                    if (userRole == "fisioterapeuta" || userRole == "administrador") {
                        setupNotificationsListener()
                    }
                }
            }
    }

    private fun setupNotificationsListener() {
        val currentUser = auth.currentUser ?: return
        appointmentsListener?.remove()
        isInitialLoad = true

        appointmentsListener = db.collection("citas")
            .whereEqualTo("physioId", currentUser.uid)
            .whereEqualTo("status", "booked")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    if (isInitialLoad) {
                        isInitialLoad = false
                        return@addSnapshotListener
                    }

                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            // Ignorar si la cita la ha creado el propio fisio localmente
                            if (change.document.metadata.hasPendingWrites()) continue
                            
                            lifecycleScope.launch {
                                    val settings = SettingsManager.getSettings(this@MainActivity).first()
                                    if (settings.notificationsEnabled) {
                                        val patientName = change.document.getString("patientName") ?: getString(R.string.un_paciente)
                                        val date = change.document.getString("date") ?: ""
                                        val time = change.document.getString("time") ?: ""
                                        
                                        val title = getString(R.string.notif_nueva_cita_titulo)
                                        val message = getString(R.string.notif_nueva_cita_mensaje, patientName, date, time)
                                        
                                        NotificationHelper.showNotification(this@MainActivity, title, message)
                                    }
                            }
                        }
                    }
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
            addItem?.title = getString(R.string.menu_horario)
            addItem?.setIcon(R.drawable.ic_citas)
        } else {
            teamItem?.isVisible = true // Los pacientes pueden ver el equipo
            patientsItem?.isVisible = false
            addItem?.title = getString(R.string.menu_nueva_cita)
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

    override fun onDestroy() {
        super.onDestroy()
        appointmentsListener?.remove()
    }
}

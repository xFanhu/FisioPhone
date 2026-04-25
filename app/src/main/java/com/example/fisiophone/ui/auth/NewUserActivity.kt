package com.example.fisiophone.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fisiophone.databinding.ActivityNewUserBinding
import com.example.fisiophone.ui.menu.MainActivity
import com.example.fisiophone.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NewUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewUserBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNewUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (ime.bottom > systemBars.bottom) ime.bottom else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        binding.newUserButton.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val nombre = binding.etNombre.text.toString().trim()
            val apellidos = binding.etApellidos.text.toString().trim()
            val dni = binding.etDNI.text.toString().trim()
            val telefono = binding.etTelefono.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val rPassword = binding.etRPassword.text.toString().trim()

            if (email.isEmpty() || nombre.isEmpty() || apellidos.isEmpty() ||
                dni.isEmpty() || telefono.isEmpty() || password.isEmpty() || rPassword.isEmpty()
            ) {
                Toast.makeText(this, getString(com.example.fisiophone.R.string.rellenar_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!telefono.matches(Regex("^\\+?[0-9]+$"))) {
                Toast.makeText(this, getString(R.string.error_telefono_vacio), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != rPassword) {
                Toast.makeText(this, getString(com.example.fisiophone.R.string.pass_no_coinciden), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, getString(com.example.fisiophone.R.string.pass_corta), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userMap = hashMapOf(
                                "email" to email,
                                "nombre" to nombre,
                                "apellidos" to apellidos,
                                "dni" to dni,
                                "telefono" to telefono,
                                "role" to "paciente"
                            )

                            db.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener {
                                    setLoading(false)
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    setLoading(false)
                                    Toast.makeText(this, getString(R.string.error_guardando_datos, e.message), Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        setLoading(false)
                        Toast.makeText(this, getString(R.string.error_formato, getString(R.string.error_registro), task.exception?.message), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.pbLoading.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.newUserButton.isEnabled = !isLoading
        binding.newUserButton.text = if (isLoading) getString(com.example.fisiophone.R.string.trabajando_registro) else getString(com.example.fisiophone.R.string.crearUsuario)
    }
}
package com.example.fisiophone.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.example.fisiophone.databinding.ActivityLogInBinding
import com.example.fisiophone.R
import com.example.fisiophone.ui.menu.MainActivity

class LogInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogInBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.loginButton.setOnClickListener {
            val email = binding.etUsuario.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.rellenar_campos), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    setLoading(false)
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && user.isEmailVerified) {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else if (user != null) {
                            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                                .setTitle(getString(R.string.titulo_verificar_cuenta))
                                .setMessage(getString(R.string.mensaje_verificar_cuenta))
                                .setPositiveButton(getString(R.string.enviar_correo)) { dialog, _ ->
                                    user.sendEmailVerification()
                                    Toast.makeText(this, getString(R.string.email_verificacion_enviado), Toast.LENGTH_LONG).show()
                                    auth.signOut()
                                    dialog.dismiss()
                                }
                                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                    auth.signOut()
                                    dialog.dismiss()
                                }
                                .setCancelable(false)
                                .show()
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.error_formato, getString(R.string.error_login), task.exception?.message), Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etUsuario.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_email_olvido), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.olvido_password))
                .setMessage(getString(R.string.mensaje_olvido_password, email))
                .setPositiveButton(getString(R.string.enviar_correo)) { dialog, _ ->
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, getString(R.string.email_reset_enviado), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, task.exception?.message ?: "Error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.pbLoading.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.loginButton.isEnabled = !isLoading
        binding.loginButton.text = if (isLoading) getString(R.string.trabajando_login) else getString(R.string.iniciar)
    }
}

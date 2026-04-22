package com.example.fisiophone.ui.menu

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fisiophone.databinding.ActivityMenuBinding
import com.example.fisiophone.ui.menu.fragments.ConfigFragment

class MenuActivity : AppCompatActivity() {

    // Binding del layout
    private lateinit var binding: ActivityMenuBinding

    // Estas vistas las guardo para poder ocultarlas/mostrarlas fácilmente
    private lateinit var menuContentView: View
    private lateinit var menuLogoView: View
    private lateinit var fragmentHostView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Asigno las vistas que voy a usar
        menuLogoView = binding.menuLogoImage
        menuContentView = binding.menuContent
        fragmentHostView = binding.fragmentHost

        // Cuando se pulsa la card de ajustes, abro el fragment de configuración
        binding.settingsCard.setOnClickListener {
            showConfigFragment()
        }

        // Botón atrás
        onBackPressedDispatcher.addCallback(this) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                // Si hay fragmentos abiertos, vuelvo atrás en lugar de cerrar la app
                supportFragmentManager.popBackStack()
            } else {
                // Si no hay nada, cierro la activity
                finish()
            }
        }

        // Cada vez que cambia el backstack, actualizo lo que se ve
        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuVisibility()
        }

        // Ajuste inicial de visibilidad
        updateMenuVisibility()
    }

    private fun showConfigFragment() {
        // Reemplazo el contenedor por el fragment de configuración
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentHost.id, ConfigFragment())
            .addToBackStack(ConfigFragment.TAG) // para poder volver atrás
            .commit()
    }

    private fun updateMenuVisibility() {
        // Compruebo si hay algún fragment abierto
        val isShowingFragment = supportFragmentManager.backStackEntryCount > 0

        // Si hay fragment -> oculto menú
        // Si no hay fragment -> muestro menú
        val menuVisibility = if (isShowingFragment) View.GONE else View.VISIBLE
        val fragmentVisibility = if (isShowingFragment) View.VISIBLE else View.GONE

        menuLogoView.visibility = menuVisibility
        menuContentView.visibility = menuVisibility
        fragmentHostView.visibility = fragmentVisibility
    }
}
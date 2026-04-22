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

    private lateinit var binding: ActivityMenuBinding
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

        menuLogoView = binding.menuLogoImage
        menuContentView = binding.menuContent
        fragmentHostView = binding.fragmentHost

        binding.settingsCard.setOnClickListener {
            showConfigFragment()
        }

        onBackPressedDispatcher.addCallback(this) {
            // Si hay un fragment abierto, volvemos al menú en lugar de cerrar la activity.
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateMenuVisibility()
        }

        updateMenuVisibility()
    }

    private fun showConfigFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentHost.id, ConfigFragment())
            .addToBackStack(ConfigFragment.TAG)
            .commit()
    }

    private fun updateMenuVisibility() {
        val isShowingFragment = supportFragmentManager.backStackEntryCount > 0
        val menuVisibility = if (isShowingFragment) View.GONE else View.VISIBLE
        val fragmentVisibility = if (isShowingFragment) View.VISIBLE else View.GONE

        menuLogoView.visibility = menuVisibility
        menuContentView.visibility = menuVisibility
        fragmentHostView.visibility = fragmentVisibility
    }
}

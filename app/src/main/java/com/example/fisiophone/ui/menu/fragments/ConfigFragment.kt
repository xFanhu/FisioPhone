package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fisiophone.data.settings.SettingsManager
import com.example.fisiophone.databinding.FragmentConfigBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConfigFragment : Fragment() {

    // Binding del layout (lo hacemos nullable porque la vista se puede destruir)
    private var _binding: FragmentConfigBinding? = null

    // Acceso al binding
    private val binding get() = _binding!!

    // Esto lo uso para que el switch no salte solo al cargar los datos
    private var isInitializing = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflamos el layout con ViewBinding
        _binding = FragmentConfigBinding.inflate(inflater, container, false)

        // Devolvemos la vista del fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializo la interfaz
        initUI()

        // Cargo los ajustes guardados
        loadSettings()
    }

    private fun initUI() {
        // Listener del switch del modo oscuro
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->

            // Si estamos inicializando, no hago nada (para que no se guarde solo)
            if (isInitializing) return@setOnCheckedChangeListener

            // Guardo el valor en segundo plano
            viewLifecycleOwner.lifecycleScope.launch {
                SettingsManager.saveDarkMode(requireContext(), isChecked)
            }

            // Aplico el modo oscuro o claro
            SettingsManager.applyNightMode(isChecked)
        }
    }

    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {

            // Cojo los ajustes guardados (Flow -> primer valor)
            val settingsData = SettingsManager.getSettings(requireContext()).first()

            // Activo esto para que el listener no salte al asignar el valor
            isInitializing = true

            // Pongo el valor en el switch según lo que haya guardado
            binding.switchDarkMode.isChecked = settingsData.darkMode

            // Ya he terminado de inicializar
            isInitializing = false

            // Aplico el modo correspondiente al arrancar
            SettingsManager.applyNightMode(settingsData.darkMode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Muy importante para evitar leaks
        _binding = null
    }

    companion object {
        const val TAG = "ConfigFragment"
    }
}
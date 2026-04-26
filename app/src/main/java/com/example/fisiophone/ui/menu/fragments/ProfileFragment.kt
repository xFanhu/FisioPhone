package com.example.fisiophone.ui.menu.fragments

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedPhotoUri: Uri? = null
    private lateinit var sessionAdapter: PatientSessionAdapter

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult

        requireContext().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        selectedPhotoUri = uri
        renderPhoto()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedPhotoUri = savedInstanceState?.getString(KEY_SELECTED_PHOTO_URI)?.let(Uri::parse)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPhotoPicker()
        setupRecyclerView()
        setupHistoryButton()
        fetchUserData()
    }

    private fun setupHistoryButton() {
        binding.cardClinicalHistory.setOnClickListener {
            val targetUid = arguments?.getString(ARG_USER_ID) ?: FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val isOwnProfile = arguments?.getString(ARG_USER_ID) == null
            val fragment = HistoryFragment.newInstance(targetUid, isOwnProfile)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentHost, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        sessionAdapter = PatientSessionAdapter(emptyList())
        binding.rvSessions.adapter = sessionAdapter
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchUserData() {
        val targetUid = arguments?.getString(ARG_USER_ID) ?: FirebaseAuth.getInstance().currentUser?.uid
        if (targetUid != null) {
            FirebaseFirestore.getInstance().collection("users").document(targetUid)
                .get()
                .addOnSuccessListener { document ->
                    if (_binding == null) return@addOnSuccessListener
                    if (document != null && document.exists()) {
                        val roleStr = document.getString("role") ?: "paciente"
                        val roleEnum = UserRole.fromValue(roleStr)
                        
                        val profile = UserProfile(
                            role = roleEnum,
                            name = document.getString("nombre") ?: "",
                            surnames = document.getString("apellidos") ?: "",
                            email = document.getString("email") ?: "",
                            dni = document.getString("dni") ?: getString(R.string.na),
                            phone = document.getString("telefono") ?: "",
                            sessions = emptyList(),
                            treatments = ((document.get("schedule") as? Map<*, *>)?.get("treatments") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            scheduleInfo = document.get("schedule") as? Map<String, Any>
                        )
                        
                        if (roleEnum == UserRole.PATIENT) {
                            fetchUserSessions(targetUid, profile)
                        } else {
                            bindProfile(profile)
                        }
                    }
                }
        }
    }

    private fun fetchUserSessions(uid: String, profile: UserProfile) {
        FirebaseFirestore.getInstance().collection("citas")
            .whereEqualTo("patientId", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener
                val sessions = mutableListOf<PatientSession>()
                
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                val currentDateTimeStr = sdf.format(java.util.Date())
                
                for (doc in documents) {
                    val dateStr = doc.getString("date") ?: ""
                    val timeStr = doc.getString("time") ?: "00:00"
                    val sessionDateTimeStr = "$dateStr $timeStr"
                    
                    if (sessionDateTimeStr < currentDateTimeStr) {
                        sessions.add(PatientSession(
                            date = dateStr,
                            physiotherapistName = doc.getString("physioName") ?: "",
                            treatment = doc.getString("tratamiento") ?: getString(R.string.sesion_fisio_default)
                        ))
                    }
                }
                // Sort by date (optional, but good)
                val sortedSessions = sessions.sortedByDescending { it.date }
                bindProfile(profile.copy(sessions = sortedSessions))
            }
            .addOnFailureListener {
                bindProfile(profile)
            }
    }

    private fun setupPhotoPicker() {
        binding.photoPickerCard.setOnClickListener {
            pickPhoto.launch(arrayOf("image/*"))
        }
    }

    private fun bindProfile(profile: UserProfile) {
        val isOwnProfile = arguments?.getString(ARG_USER_ID) == null
        val isPatient = profile.role == UserRole.PATIENT
        val isPhysio = profile.role == UserRole.PHYSIOTHERAPIST
        
        // PRIVACIDAD: Ocultamos DNI/Teléfono si es el perfil de un fisioterapeuta visto por un paciente.
        // Asumimos: si 'isPatient' es true, el que lo mira es un fisio/admin. Si es 'isOwnProfile', es el propio usuario.
        val showSensitiveData = isOwnProfile || isPatient

        binding.tvNameValue.text = profile.name
        binding.tvApellidosValue.text = profile.surnames
        binding.tvEmailValue.text = profile.email

        binding.tvDniLabel.visibility = if (showSensitiveData) View.VISIBLE else View.GONE
        binding.tvDniValue.visibility = if (showSensitiveData) View.VISIBLE else View.GONE
        if (showSensitiveData) {
            binding.tvDniValue.text = profile.dni
        }

        if (profile.phone.isNotEmpty() && showSensitiveData) {
            binding.tvPhoneLabel.visibility = View.VISIBLE
            binding.tvPhoneValue.visibility = View.VISIBLE
            binding.tvPhoneValue.text = profile.phone
        } else {
            binding.tvPhoneLabel.visibility = View.GONE
            binding.tvPhoneValue.visibility = View.GONE
        }

        binding.cardClinicalHistory.visibility = if (isPatient) View.VISIBLE else View.GONE
        
        binding.tvSessionsTitle.visibility = if (isPatient) View.VISIBLE else View.GONE
        binding.tvSessionsEmpty.visibility = if (isPatient && profile.sessions.isEmpty()) View.VISIBLE else View.GONE
        
        val containerCard = binding.rvSessions.parent as? View
        containerCard?.visibility = if (isPatient && profile.sessions.isNotEmpty()) View.VISIBLE else View.GONE

        if (isPatient) {
            renderSessions(profile.sessions)
        }
        
        if (isPhysio) {
            binding.tvSpecialtiesTitle.visibility = View.VISIBLE
            binding.cgSpecialties.visibility = View.VISIBLE
            binding.cgSpecialties.removeAllViews()
            if (profile.treatments.isEmpty()) {
                val chip = com.google.android.material.chip.Chip(requireContext())
                chip.text = getString(R.string.sin_especialidades)
                chip.isCheckable = false
                binding.cgSpecialties.addView(chip)
            } else {
                profile.treatments.forEach { treatment ->
                    val chip = com.google.android.material.chip.Chip(requireContext())
                    chip.text = treatment
                    chip.isCheckable = false
                    binding.cgSpecialties.addView(chip)
                }
            }

            binding.tvScheduleTitle.visibility = View.VISIBLE
            binding.tvScheduleDays.visibility = View.VISIBLE
            binding.tvScheduleHours.visibility = View.VISIBLE
            
            val workingDays = (profile.scheduleInfo?.get("workDays") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val startHour = profile.scheduleInfo?.get("startHour") as? String ?: "09:00"
            val endHour = profile.scheduleInfo?.get("endHour") as? String ?: "18:00"

            if (workingDays.isNotEmpty()) {
                val daysMap = mapOf(
                    "Mon" to getString(R.string.lunes),
                    "Tue" to getString(R.string.martes),
                    "Wed" to getString(R.string.miercoles),
                    "Thu" to getString(R.string.jueves),
                    "Fri" to getString(R.string.viernes),
                    "Sat" to getString(R.string.sabado),
                    "Sun" to getString(R.string.domingo)
                )
                val localizedDays = workingDays.map { daysMap[it] ?: it }.joinToString(", ")
                binding.tvScheduleDays.text = localizedDays
            } else {
                binding.tvScheduleDays.text = getString(R.string.horario_no_configurado)
            }
            binding.tvScheduleHours.text = "$startHour - $endHour"
        } else {
            binding.tvSpecialtiesTitle.visibility = View.GONE
            binding.cgSpecialties.visibility = View.GONE
            binding.tvScheduleTitle.visibility = View.GONE
            binding.tvScheduleDays.visibility = View.GONE
            binding.tvScheduleHours.visibility = View.GONE
        }
        
        binding.photoPickerCard.isEnabled = isOwnProfile
        
        binding.btnCall.visibility = if (!isOwnProfile && profile.phone.isNotEmpty() && showSensitiveData) View.VISIBLE else View.GONE
        binding.btnCall.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.llamar_paciente_titulo)
                .setMessage(getString(R.string.llamar_paciente_mensaje, profile.name))
                .setPositiveButton(R.string.si) { _, _ ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.phone}"))
                    startActivity(intent)
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
        
        renderPhoto()
    }

    private fun renderPhoto() {
        val hasPhoto = selectedPhotoUri != null
        val isOwnProfile = arguments?.getString(ARG_USER_ID) == null
        
        binding.ivProfilePhoto.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        binding.tvProfilePhotoPlaceholder.visibility = if (!hasPhoto && isOwnProfile) View.VISIBLE else View.GONE

        if (hasPhoto) {
            binding.ivProfilePhoto.setImageURI(selectedPhotoUri)
        }
    }

    private fun renderSessions(sessions: List<PatientSession>) {
        binding.tvSessionsEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        sessionAdapter.updateList(sessions)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED_PHOTO_URI, selectedPhotoUri?.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class UserProfile(
        val role: UserRole,
        val name: String,
        val surnames: String,
        val email: String,
        val dni: String,
        val phone: String,
        val sessions: List<PatientSession>,
        val treatments: List<String> = emptyList(),
        val scheduleInfo: Map<String, Any>? = null
    )

    data class PatientSession(
        val date: String,
        val physiotherapistName: String,
        val treatment: String
    )

    enum class UserRole(val value: String) {
        PATIENT("paciente"),
        PHYSIOTHERAPIST("fisioterapeuta"),
        ADMIN("administrador");

        companion object {
            fun fromValue(value: String): UserRole {
                return entries.firstOrNull { it.value == value } ?: PATIENT
            }
        }
    }

    companion object {
        const val TAG = "ProfileFragment"

        private const val ARG_USER_ROLE = "arg_user_role"
        private const val ARG_USER_ID = "arg_user_id"
        private const val KEY_SELECTED_PHOTO_URI = "key_selected_photo_uri"

        fun newInstance(userRole: UserRole, userId: String? = null): ProfileFragment {
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ROLE, userRole.value)
                    putString(ARG_USER_ID, userId)
                }
            }
        }
    }
}

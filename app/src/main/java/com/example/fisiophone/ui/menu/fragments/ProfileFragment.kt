package com.example.fisiophone.ui.menu.fragments

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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

    // Selector de fotos de la galería
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
        setupEditButton()
        fetchUserData()
    }

    private fun setupEditButton() {
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }
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

    // Obtiene los datos del usuario de Firestore. Si es el propio perfil coge lo del elvis, no se pasa ID por argumnts.

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
                            bio = document.getString("bio") ?: "",
                            photoUrl = document.getString("photoUrl"),
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

    //Si el perfil es paciente, carga su historial de citas realizadas.
    private fun fetchUserSessions(uid: String, profile: UserProfile) {
        FirebaseFirestore.getInstance().collection("citas")
            .whereEqualTo("patientId", uid)
            .whereEqualTo("status", "done")
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener
                val sessions = mutableListOf<PatientSession>()
                
                for (doc in documents) {
                    val dateStr = doc.getString("date") ?: ""
                    sessions.add(PatientSession(
                        date = dateStr,
                        physiotherapistName = doc.getString("physioName") ?: "",
                        treatment = doc.getString("tratamiento") ?: getString(R.string.sesion_fisio_default)
                    ))
                }
                val sortedSessions = sessions.sortedByDescending { it.date }
                bindProfile(profile.copy(sessions = sortedSessions))
            }
            .addOnFailureListener { bindProfile(profile) }
    }

    private fun setupPhotoPicker() {
        binding.photoPickerCard.setOnClickListener {
            pickPhoto.launch(arrayOf("image/*"))
        }
    }

    //Pone los datos del perfil aplicando privacidad, para que el paciente no vea datos personales de fisios.

    private fun bindProfile(profile: UserProfile) {
        val isOwnProfile = arguments?.getString(ARG_USER_ID) == null
        val isPatient = profile.role == UserRole.PATIENT
        val isPhysio = profile.role == UserRole.PHYSIOTHERAPIST || profile.role == UserRole.ADMIN

        // El botón de editar solo aparece si es mi perfil yy soy fisio (para la bio)
        binding.btnEditProfile.visibility = if (isOwnProfile && isPhysio) View.VISIBLE else View.GONE
        // El historial clínico se ve siempre que el perfil que estamos mirando sea de un paciente
        binding.cardClinicalHistory.visibility = if (isPatient) View.VISIBLE else View.GONE
        
        // PRIVACIDAD: Los pacientes NO ven DNI ni teléfono de los fisios.
        val showSensitiveData = isOwnProfile || isPatient

        binding.tvNameValue.text = profile.name
        binding.tvApellidosValue.text = profile.surnames
        binding.tvEmailValue.text = profile.email

        //Visibilidad de DNI
        binding.tvDniLabel.visibility = if (showSensitiveData) View.VISIBLE else View.GONE
        binding.tvDniValue.visibility = if (showSensitiveData) View.VISIBLE else View.GONE
        if (showSensitiveData) {
            binding.tvDniValue.text = profile.dni
        }

        //Visibilidad de Teléfono
        if (profile.phone.isNotEmpty() && showSensitiveData) {
            binding.tvPhoneLabel.visibility = View.VISIBLE
            binding.tvPhoneValue.visibility = View.VISIBLE
            binding.tvPhoneValue.text = profile.phone
        } else {
            binding.tvPhoneLabel.visibility = View.GONE
            binding.tvPhoneValue.visibility = View.GONE
        }
        
        // Biografía (solo fisios9
        if (isPhysio) {
            binding.layoutBioLabel.visibility = View.VISIBLE
            binding.tvBioValue.visibility = View.VISIBLE
            if (profile.bio.isNotEmpty()) {
                binding.tvBioValue.text = profile.bio
                binding.tvBioValue.setTypeface(null, Typeface.NORMAL)
            } else {
                binding.tvBioValue.text = getString(R.string.biografia_vacia)
                binding.tvBioValue.setTypeface(null, Typeface.ITALIC)
            }
        } else {
            binding.layoutBioLabel.visibility = View.GONE
            binding.tvBioValue.visibility = View.GONE
        }

        // Lista sesiones realizads (solo pacientes)
        binding.tvSessionsTitle.visibility = if (isPatient) View.VISIBLE else View.GONE
        binding.tvSessionsEmpty.visibility = if (isPatient && profile.sessions.isEmpty()) View.VISIBLE else View.GONE
        
        val containerCard = binding.rvSessions.parent as? View
        containerCard?.visibility = if (isPatient && profile.sessions.isNotEmpty()) View.VISIBLE else View.GONE

        if (isPatient) {
            renderSessions(profile.sessions)
        }
        
        //Especialidades y Horario (solo fisos)
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
        
        // Boton llamada
        binding.btnCall.visibility = if (!isOwnProfile && profile.phone.isNotEmpty() && showSensitiveData) View.VISIBLE else View.GONE
        binding.btnCall.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.llamar_paciente_titulo)
                .setMessage(getString(R.string.llamar_paciente_mensaje, profile.name))
                .setPositiveButton(R.string.si) { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.phone}"))
                    startActivity(intent)
                }
                .setNegativeButton(R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        
        renderPhoto(profile.photoUrl)
    }

    private fun showEditProfileDialog() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val currentBio = binding.tvBioValue.text.toString().let { 
            if (it == getString(R.string.biografia_vacia)) "" else it 
        }

        val input = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = getString(R.string.hint_biografia)
            setText(currentBio)
            setLines(5)
            maxLines = 10
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
        }

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val margin = (24 * resources.displayMetrics.density).toInt()
            setMargins(margin, margin / 2, margin, margin / 2)
        }
        input.layoutParams = params
        container.addView(input)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.editar_perfil)
            .setView(container)
            .setPositiveButton(R.string.guardar_cambios) { dialog, _ ->
                val newBio = input.text.toString().trim()
                updateBio(uid, newBio)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateBio(uid: String, bio: String) {
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("bio", bio)
            .addOnSuccessListener {
                fetchUserData() // Recarga para mostrar cambios
                android.widget.Toast.makeText(requireContext(), R.string.horario_guardado, android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(requireContext(), R.string.error_generico, android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun renderPhoto(photoUrl: String? = null) {
        val isOwnProfile = arguments?.getString(ARG_USER_ID) == null

        when {
            selectedPhotoUri != null -> {
                binding.ivProfilePhoto.setImageURI(selectedPhotoUri)
                binding.ivProfilePhoto.visibility = View.VISIBLE
                binding.tvProfilePhotoPlaceholder.visibility = View.GONE
            }
            !photoUrl.isNullOrEmpty() -> {
                com.bumptech.glide.Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .circleCrop()
                    .into(binding.ivProfilePhoto)
                binding.ivProfilePhoto.visibility = View.VISIBLE
                binding.tvProfilePhotoPlaceholder.visibility = View.GONE
            }
            else -> {
                binding.ivProfilePhoto.setImageResource(R.drawable.ic_profile)
                binding.ivProfilePhoto.visibility = View.VISIBLE
                binding.tvProfilePhotoPlaceholder.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
            }
        }
    }

    private fun renderSessions(sessions: List<PatientSession>) {
        binding.tvSessionsEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        sessionAdapter.updateList(sessions)
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
        val bio: String = "",
        val photoUrl: String? = null,
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

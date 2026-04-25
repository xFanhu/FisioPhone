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
                            sessions = emptyList()
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
        binding.tvNameValue.text = profile.name
        binding.tvApellidosValue.text = profile.surnames
        binding.tvEmailValue.text = profile.email
        binding.tvDniValue.text = profile.dni

        if (profile.phone.isNotEmpty()) {
            binding.tvPhoneLabel.visibility = View.VISIBLE
            binding.tvPhoneValue.visibility = View.VISIBLE
            binding.tvPhoneValue.text = profile.phone
        } else {
            binding.tvPhoneLabel.visibility = View.GONE
            binding.tvPhoneValue.visibility = View.GONE
        }

        val isPatient = profile.role == UserRole.PATIENT
        binding.cardClinicalHistory.visibility = if (isPatient) View.VISIBLE else View.GONE
        
        binding.tvSessionsTitle.visibility = if (isPatient) View.VISIBLE else View.GONE
        binding.tvSessionsEmpty.visibility = if (isPatient && profile.sessions.isEmpty()) View.VISIBLE else View.GONE
        
        val containerCard = binding.rvSessions.parent as? View
        containerCard?.visibility = if (isPatient && profile.sessions.isNotEmpty()) View.VISIBLE else View.GONE

        binding.tvDniLabel.visibility = if (isPatient) View.VISIBLE else View.GONE
        binding.tvDniValue.visibility = if (isPatient) View.VISIBLE else View.GONE

        if (isPatient) {
            renderSessions(profile.sessions)
        }
        
        val isOwnProfile = arguments?.getString(ARG_USER_ID) == null
        binding.photoPickerCard.isEnabled = isOwnProfile
        
        binding.btnCall.visibility = if (!isOwnProfile && profile.phone.isNotEmpty()) View.VISIBLE else View.GONE
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
        val sessions: List<PatientSession>
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

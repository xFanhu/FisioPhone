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
        fetchUserData()
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
            .whereEqualTo("pacienteId", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener
                val sessions = mutableListOf<PatientSession>()
                for (doc in documents) {
                    sessions.add(PatientSession(
                        date = doc.getString("fecha") ?: "",
                        physiotherapistName = doc.getString("fisioterapeutaNombre") ?: "",
                        treatment = doc.getString("tratamiento") ?: getString(R.string.sesion_fisio_default)
                    ))
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

        val isPatient = profile.role == UserRole.PATIENT
        binding.tvSessionsTitle.visibility = if (isPatient) View.VISIBLE else View.GONE
        binding.tvSessionsEmpty.visibility = if (isPatient && profile.sessions.isEmpty()) View.VISIBLE else View.GONE
        
        val containerCard = binding.sessionsContainer.parent as? View
        containerCard?.visibility = if (isPatient && profile.sessions.isNotEmpty()) View.VISIBLE else View.GONE

        binding.tvDniLabel.visibility = if (isPatient) View.VISIBLE else View.GONE
        binding.tvDniValue.visibility = if (isPatient) View.VISIBLE else View.GONE

        if (isPatient) {
            renderSessions(profile.sessions)
        }
        
        val isOwnProfile = arguments?.getString(ARG_USER_ID) == null
        binding.photoPickerCard.isEnabled = isOwnProfile
        
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
        binding.sessionsContainer.removeAllViews()
        binding.tvSessionsEmpty.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE

        sessions.forEach { session ->
            binding.sessionsContainer.addView(createSessionCard(session))
        }
    }

    private fun createSessionCard(session: PatientSession): View {
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = 0x1A000000.toInt()
            setCardBackgroundColor(requireContext().getColor(R.color.card_background))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val dateView = TextView(requireContext()).apply {
            text = session.date
            setTextColor(requireContext().getColor(R.color.azul))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
        }

        val treatmentView = TextView(requireContext()).apply {
            text = session.treatment
            setTextColor(requireContext().getColor(R.color.card_text))
            textSize = 15f
        }

        val professionalView = TextView(requireContext()).apply {
            text = getString(R.string.perfil_sesion_fisio, session.physiotherapistName)
            setTextColor(requireContext().getColor(R.color.description_gray))
            textSize = 12f
        }

        textContainer.addView(dateView)
        textContainer.addView(treatmentView)
        textContainer.addView(professionalView)
        content.addView(textContainer)
        
        card.addView(content)
        return card
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
        val sessions: List<PatientSession>
    )

    data class PatientSession(
        val date: String,
        val physiotherapistName: String,
        val treatment: String
    )

    enum class UserRole(val value: String) {
        PATIENT("paciente"),
        PHYSIOTHERAPIST("fisioterapeuta");

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

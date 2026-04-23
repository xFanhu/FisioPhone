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
        bindProfile(getMockProfile())
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

        val isPatient = profile.role == UserRole.PATIENT
        binding.tvSessionsTitle.visibility = if (isPatient) View.VISIBLE else View.GONE
        binding.sessionsContainer.visibility = if (isPatient) View.VISIBLE else View.GONE

        if (isPatient) {
            renderSessions(profile.sessions)
        } else {
            binding.tvSessionsEmpty.visibility = View.GONE
        }

        renderPhoto()
    }

    private fun renderPhoto() {
        val hasPhoto = selectedPhotoUri != null
        binding.ivProfilePhoto.visibility = if (hasPhoto) View.VISIBLE else View.GONE
        binding.tvProfilePhotoPlaceholder.visibility = if (hasPhoto) View.GONE else View.VISIBLE

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
        val card = CardView(requireContext()).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(4).toFloat()
            setCardBackgroundColor(requireContext().getColor(R.color.card_background))
            useCompatPadding = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val dateView = TextView(requireContext()).apply {
            text = session.date
            setTextColor(requireContext().getColor(R.color.card_text))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        }

        val professionalView = TextView(requireContext()).apply {
            text = getString(R.string.perfil_sesion_fisio, session.physiotherapistName)
            setTextColor(requireContext().getColor(R.color.description_gray))
            textSize = 14f
        }

        val treatmentView = TextView(requireContext()).apply {
            text = getString(R.string.perfil_sesion_tratamiento, session.treatment)
            setTextColor(requireContext().getColor(R.color.description_gray))
            textSize = 14f
        }

        content.addView(dateView)
        content.addView(professionalView)
        content.addView(treatmentView)
        card.addView(content)
        return card
    }

    private fun getMockProfile(): UserProfile {
        val role = arguments?.getString(ARG_USER_ROLE)
            ?.let(UserRole::fromValue)
            ?: UserRole.PATIENT

        return when (role) {
            UserRole.PATIENT -> UserProfile(
                role = role,
                name = "Lucia",
                surnames = "Fernandez Lopez",
                email = "lucia.fernandez@email.com",
                sessions = listOf(
                    PatientSession(
                        date = "12/04/2026",
                        physiotherapistName = "Marta Ruiz",
                        treatment = "Rehabilitacion de hombro"
                    ),
                    PatientSession(
                        date = "18/04/2026",
                        physiotherapistName = "Daniel Soto",
                        treatment = "Descarga muscular lumbar"
                    ),
                    PatientSession(
                        date = "22/04/2026",
                        physiotherapistName = "Marta Ruiz",
                        treatment = "Movilidad escapular"
                    )
                )
            )

            UserRole.PHYSIOTHERAPIST -> UserProfile(
                role = role,
                name = "Carlos",
                surnames = "Martin Perez",
                email = "c.martin@fisiophone.com",
                sessions = emptyList()
            )
        }
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
        private const val KEY_SELECTED_PHOTO_URI = "key_selected_photo_uri"

        fun newInstance(userRole: UserRole): ProfileFragment {
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ROLE, userRole.value)
                }
            }
        }
    }
}

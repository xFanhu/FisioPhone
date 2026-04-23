package com.example.fisiophone.ui.menu.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.fisiophone.R
import com.example.fisiophone.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val notifications = mutableListOf<AppointmentNotification>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notifications.clear()
        notifications.addAll(getSampleNotifications())
        renderNotifications()
    }

    private fun renderNotifications() {
        binding.notificationsContainer.removeAllViews()
        binding.tvEmptyNotifications.visibility =
            if (notifications.isEmpty()) View.VISIBLE else View.GONE

        notifications.forEach { notification ->
            binding.notificationsContainer.addView(createNotificationCard(notification))
        }
    }

    private fun createNotificationCard(notification: AppointmentNotification): View {
        val card = CardView(requireContext()).apply {
            radius = dp(12).toFloat()
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
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(10), dp(12))
        }

        val message = getString(
            R.string.notificacion_cita,
            notification.physiotherapistName,
            notification.time,
            notification.day
        )

        val textView = TextView(requireContext()).apply {
            text = message
            setTextColor(requireContext().getColor(R.color.card_text))
            textSize = 15f
            setLineSpacing(0f, 1.15f)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val deleteButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.ic_delete)
            background = null
            contentDescription = getString(R.string.eliminar_notificacion)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener {
                deleteNotification(notification.id)
            }
        }

        content.addView(textView)
        content.addView(deleteButton)
        card.addView(content)
        return card
    }

    private fun deleteNotification(notificationId: Long) {
        // Sustituir por el borrado en base de datos cuando este disponible.
        notifications.removeAll { it.id == notificationId }
        renderNotifications()
    }

    private fun getSampleNotifications(): List<AppointmentNotification> {
        // Sustituir por la llamada a la base de datos cuando este disponible.
        return listOf(
            AppointmentNotification(
                id = 1L,
                physiotherapistName = "Laura Garcia",
                time = "10:30",
                day = "24/04/2026"
            ),
            AppointmentNotification(
                id = 2L,
                physiotherapistName = "Carlos Martin",
                time = "17:00",
                day = "28/04/2026"
            )
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class AppointmentNotification(
        val id: Long,
        val physiotherapistName: String,
        val time: String,
        val day: String
    )

    companion object {
        const val TAG = "NotificationsFragment"
    }
}

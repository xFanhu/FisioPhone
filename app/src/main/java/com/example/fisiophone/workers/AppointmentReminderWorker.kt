package com.example.fisiophone.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fisiophone.data.settings.SettingsManager
import com.example.fisiophone.notifications.NotificationHelper
import kotlinx.coroutines.flow.first

class AppointmentReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // Checkea que las notificaciones estén habilitads
            val settings = SettingsManager.getSettings(context).first()
            if (!settings.notificationsEnabled) {
                return Result.success()
            }

            val physioName = inputData.getString("physioName") ?: "tu fisioterapeuta"
            val time = inputData.getString("time") ?: ""

            val title = "Recordatorio de Cita"
            val message = "Mañana tienes una cita con $physioName a las $time."

            NotificationHelper.showNotification(context, title, message)

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}

package com.example.fisiophone.utils

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import java.util.*

/**
 * Validador para MaterialDatePicker que permite habilitar solo los días 
 * que el profesional tiene configurados en su horario.
 */
class WorkingDaysValidator(private val workDays: List<String>) : CalendarConstraints.DateValidator {

    override fun isValid(date: Long): Boolean {
        // Usar UTC para evitar desfases de zona horaria en el DatePicker
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val dayStr = when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> ""
        }
        
        // Si no hay días configurados, permitimos todos por defecto (o ninguno, según prefieras)
        return workDays.isEmpty() || workDays.contains(dayStr)
    }

    constructor(parcel: Parcel) : this(parcel.createStringArrayList() ?: emptyList())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(workDays)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WorkingDaysValidator> {
        override fun createFromParcel(parcel: Parcel): WorkingDaysValidator = WorkingDaysValidator(parcel)
        override fun newArray(size: Int): Array<WorkingDaysValidator?> = arrayOfNulls(size)
    }
}

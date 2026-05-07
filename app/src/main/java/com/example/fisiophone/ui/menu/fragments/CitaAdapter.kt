package com.example.fisiophone.ui.menu.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R

data class Cita(
    val id: String,
    val patientId: String,
    val physioId: String,
    val patientName: String,
    val physioName: String,
    val date: String,
    val time: String,
    val status: String,
    val tratamiento: String
)

class CitaAdapter(
    private var citas: List<Cita>,
    private var isPatient: Boolean,
    private val onItemClick: (Cita) -> Unit = {},
    private val onDeleteClick: (Cita) -> Unit,
    private val onFinishClick: (Cita) -> Unit = {} // Nuevo callback
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

    fun updateRole(newIsPatient: Boolean) {
        if (isPatient != newIsPatient) {
            isPatient = newIsPatient
            notifyDataSetChanged()
        }
    }

    fun updateList(newList: List<Cita>) {
        citas = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        holder.bind(citas[position])
    }

    override fun getItemCount(): Int = citas.size

    inner class CitaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCitaTime: TextView = itemView.findViewById(R.id.tvCitaTime)
        private val tvCitaDate: TextView = itemView.findViewById(R.id.tvCitaDate)
        private val tvCitaNameLabel: TextView = itemView.findViewById(R.id.tvCitaNameLabel)
        private val tvCitaNameValue: TextView = itemView.findViewById(R.id.tvCitaNameValue)
        private val ivDeleteCita: ImageView = itemView.findViewById(R.id.ivDeleteCita)
        private val btnFinishCita: View = itemView.findViewById(R.id.btnFinishCita) // Nuevo botón

        private val tvCitaStatus: TextView = itemView.findViewById(R.id.tvCitaStatus)
        private val tvCitaTreatment: TextView = itemView.findViewById(R.id.tvCitaTreatment)

        fun bind(cita: Cita) {
            val context = itemView.context
            tvCitaTime.text = cita.time
            
            // Formatear la fecha de aaaa-mm-dd a dd-mm-aaaa
            val dateParts = cita.date.split("-")
            tvCitaDate.text = if (dateParts.size == 3) {
                "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"
            } else {
                cita.date
            }
            
            tvCitaTreatment.text = cita.tratamiento
            
            // Lógica de estado explícita (ya no es automática por tiempo)
            if (cita.status == "done") {
                tvCitaStatus.text = context.getString(R.string.estado_realizada)
                tvCitaStatus.setTextColor(android.graphics.Color.GRAY)
                btnFinishCita.visibility = View.GONE
            } else {
                tvCitaStatus.text = context.getString(R.string.estado_confirmada)
                tvCitaStatus.setTextColor(context.getColor(R.color.azul))
                
                // Mostrar botón "Finalizar" solo si es FISIO y es el momento (hoy o pasado)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                val now = sdf.format(java.util.Date())
                val isTimeToShowFinish = !isPatient && "${cita.date} ${cita.time}" <= now
                
                btnFinishCita.visibility = if (isTimeToShowFinish) View.VISIBLE else View.GONE
            }

            if (isPatient) {
                tvCitaNameLabel.text = context.getString(R.string.label_fisioterapeuta)
                tvCitaNameValue.text = cita.physioName
            } else {
                tvCitaNameLabel.text = context.getString(R.string.label_paciente)
                tvCitaNameValue.text = cita.patientName
            }

            ivDeleteCita.setOnClickListener { 

                onDeleteClick(cita) 
            }
            btnFinishCita.setOnClickListener { 

                onFinishClick(cita) 
            }
            itemView.setOnClickListener { onItemClick(cita) }
        }
    }
}

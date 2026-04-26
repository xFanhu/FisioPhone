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
    private val isPatient: Boolean,
    private val onDeleteClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {

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

        private val tvCitaTreatment: TextView = itemView.findViewById(R.id.tvCitaTreatment)

        fun bind(cita: Cita) {
            tvCitaTime.text = cita.time
            tvCitaDate.text = cita.date
            tvCitaTreatment.text = cita.tratamiento
            
            if (isPatient) {
                tvCitaNameLabel.text = itemView.context.getString(R.string.label_fisioterapeuta)
                tvCitaNameValue.text = cita.physioName
            } else {
                tvCitaNameLabel.text = itemView.context.getString(R.string.label_paciente)
                tvCitaNameValue.text = cita.patientName
            }

            ivDeleteCita.setOnClickListener {
                onDeleteClick(cita)
            }
        }
    }
}

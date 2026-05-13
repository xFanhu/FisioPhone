package com.example.fisiophone.ui.menu.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
import com.example.fisiophone.databinding.ItemCitaBinding

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
    private val onDeleteClick: (Cita) -> Unit
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
        val binding = ItemCitaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        holder.bind(citas[position])
    }

    override fun getItemCount(): Int = citas.size

    inner class CitaViewHolder(private val binding: ItemCitaBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: Cita) {
            val context = itemView.context
            binding.tvCitaTime.text = cita.time
            
            // Formatear la fecha de aaaa-mm-dd a dd-mm-aaaa
            val dateParts = cita.date.split("-")
            binding.tvCitaDate.text = if (dateParts.size == 3) {
                "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"
            } else {
                cita.date
            }
            
            binding.tvCitaTreatment.text = cita.tratamiento
            
            // Lógica de estado explícita (ya no es automática por tiempo)
            if (cita.status == "done") {
                binding.tvCitaStatus.text = context.getString(R.string.estado_realizada)
                binding.tvCitaStatus.setTextColor(android.graphics.Color.GRAY)
            } else {
                binding.tvCitaStatus.text = context.getString(R.string.estado_confirmada)
                binding.tvCitaStatus.setTextColor(context.getColor(R.color.azul))
            }

            if (isPatient) {
                binding.tvCitaNameLabel.text = context.getString(R.string.label_fisioterapeuta)
                binding.tvCitaNameValue.text = cita.physioName
            } else {
                binding.tvCitaNameLabel.text = context.getString(R.string.label_paciente)
                binding.tvCitaNameValue.text = cita.patientName
            }

            binding.ivDeleteCita.setOnClickListener { 
                onDeleteClick(cita) 
            }
            itemView.setOnClickListener { onItemClick(cita) }
        }
    }
}

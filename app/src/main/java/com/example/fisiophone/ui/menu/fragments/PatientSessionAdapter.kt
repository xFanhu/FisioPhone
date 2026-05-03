package com.example.fisiophone.ui.menu.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
import com.example.fisiophone.databinding.ItemPatientSessionBinding

class PatientSessionAdapter(
    private var sessions: List<ProfileFragment.PatientSession>
) : RecyclerView.Adapter<PatientSessionAdapter.SessionViewHolder>() {

    class SessionViewHolder(val binding: ItemPatientSessionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemPatientSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        
        // Formatear la fecha de aaaa-mm-dd a dd-mm-aaaa
        val dateParts = session.date.split("-")
        val formattedDate = if (dateParts.size == 3) {
            "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"
        } else {
            session.date
        }
        
        holder.binding.tvSessionDate.text = formattedDate
        holder.binding.tvSessionTreatment.text = session.treatment
        holder.binding.tvSessionPhysio.text = holder.itemView.context.getString(R.string.perfil_sesion_fisio, session.physiotherapistName)
    }

    override fun getItemCount(): Int = sessions.size

    fun updateList(newSessions: List<ProfileFragment.PatientSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}

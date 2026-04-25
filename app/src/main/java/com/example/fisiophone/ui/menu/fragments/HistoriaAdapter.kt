package com.example.fisiophone.ui.menu.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R

data class HistoriaClinica(
    val id: String,
    val physioName: String,
    val date: String,
    val timestamp: Long,
    val note: String
)

class HistoriaAdapter(
    private var historias: List<HistoriaClinica>
) : RecyclerView.Adapter<HistoriaAdapter.HistoriaViewHolder>() {

    fun updateList(newList: List<HistoriaClinica>) {
        historias = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoriaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_historia, parent, false)
        return HistoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoriaViewHolder, position: Int) {
        holder.bind(historias[position])
    }

    override fun getItemCount(): Int = historias.size

    inner class HistoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPhysioName: TextView = itemView.findViewById(R.id.tvPhysioName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)

        fun bind(historia: HistoriaClinica) {
            tvPhysioName.text = historia.physioName
            tvDate.text = historia.date
            tvNote.text = historia.note
        }
    }
}

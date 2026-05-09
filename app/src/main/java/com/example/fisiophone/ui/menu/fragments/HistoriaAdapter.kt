package com.example.fisiophone.ui.menu.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
import com.example.fisiophone.databinding.ItemHistoriaBinding

data class HistoriaClinica(
    val id: String,
    val physioName: String,
    val date: String,
    val timestamp: Long,
    val note: String
)

class HistoriaAdapter(
    private var historias: List<HistoriaClinica>,
    private val onItemClick: (HistoriaClinica) -> Unit
) : RecyclerView.Adapter<HistoriaAdapter.HistoriaViewHolder>() {

    fun updateList(newList: List<HistoriaClinica>) {
        historias = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoriaViewHolder {
        val binding = ItemHistoriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoriaViewHolder, position: Int) {
        holder.bind(historias[position])
    }

    override fun getItemCount(): Int = historias.size

    inner class HistoriaViewHolder(private val binding: ItemHistoriaBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(historia: HistoriaClinica) {
            binding.tvPhysioName.text = historia.physioName
            binding.tvDate.text = historia.date
            binding.tvNote.text = historia.note
            
            binding.root.setOnClickListener {
                onItemClick(historia)
            }
        }
    }
}

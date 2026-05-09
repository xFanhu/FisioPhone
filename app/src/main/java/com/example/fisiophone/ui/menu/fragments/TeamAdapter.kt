package com.example.fisiophone.ui.menu.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
import com.example.fisiophone.databinding.ItemTeamMemberBinding
import com.example.fisiophone.ui.menu.User

class TeamAdapter(
    private var teamList: List<User>,
    private val onItemClick: (User) -> Unit = {}
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    fun updateList(newList: List<User>) {
        teamList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemTeamMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val user = teamList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = teamList.size

    inner class TeamViewHolder(private val binding: ItemTeamMemberBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvTeamMemberName.text = "${user.nombre} ${user.apellidos}"
            binding.tvTeamMemberEmail.text = user.email
            
            if (!user.photoUrl.isNullOrEmpty()) {
                binding.ivTeamMemberPhoto.setPadding(0, 0, 0, 0)
                binding.ivTeamMemberPhoto.imageTintList = null
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(user.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(binding.ivTeamMemberPhoto)
            } else {
                binding.ivTeamMemberPhoto.setPadding(32, 32, 32, 32)
                binding.ivTeamMemberPhoto.setImageResource(R.drawable.ic_profile)
                binding.ivTeamMemberPhoto.imageTintList = android.content.res.ColorStateList.valueOf(
                    itemView.context.getColor(R.color.azul)
                )
            }

            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}

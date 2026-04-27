package com.example.fisiophone.ui.menu.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_team_member, parent, false)
        return TeamViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val user = teamList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = teamList.size

    inner class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTeamMemberName: TextView = itemView.findViewById(R.id.tvTeamMemberName)
        private val tvTeamMemberEmail: TextView = itemView.findViewById(R.id.tvTeamMemberEmail)
        private val ivTeamMemberPhoto: android.widget.ImageView = itemView.findViewById(R.id.ivTeamMemberPhoto)

        fun bind(user: User) {
            tvTeamMemberName.text = "${user.nombre} ${user.apellidos}"
            tvTeamMemberEmail.text = user.email
            
            if (!user.photoUrl.isNullOrEmpty()) {
                ivTeamMemberPhoto.setPadding(0, 0, 0, 0)
                ivTeamMemberPhoto.imageTintList = null
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(user.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(ivTeamMemberPhoto)
            } else {
                ivTeamMemberPhoto.setPadding(32, 32, 32, 32)
                ivTeamMemberPhoto.setImageResource(R.drawable.ic_profile)
                ivTeamMemberPhoto.imageTintList = android.content.res.ColorStateList.valueOf(
                    itemView.context.getColor(R.color.azul)
                )
            }

            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}

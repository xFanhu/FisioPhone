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

        fun bind(user: User) {
            tvTeamMemberName.text = "${user.nombre} ${user.apellidos}"
            tvTeamMemberEmail.text = user.email
            
            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}

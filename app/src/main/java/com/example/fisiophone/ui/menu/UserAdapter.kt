package com.example.fisiophone.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R

data class User(
    val id: String,
    val nombre: String,
    val apellidos: String,
    val email: String,
    val dni: String,
    var role: String
)

class UserAdapter(
    private var userList: List<User>,
    private val showActions: Boolean = true,
    private val onRoleChangeClick: (User) -> Unit = {},
    private val onItemClick: (User) -> Unit = {}
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    fun updateList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserRole: TextView = itemView.findViewById(R.id.tvUserRole)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvUserDni: TextView = itemView.findViewById(R.id.tvUserDni)
        private val btnChangeRole: Button = itemView.findViewById(R.id.btnChangeRole)

        fun bind(user: User) {
            tvUserName.text = "${user.nombre} ${user.apellidos}"
            tvUserRole.text = user.role.replaceFirstChar { it.uppercase() }
            tvUserEmail.text = user.email
            tvUserDni.text = "DNI: ${user.dni}"

            if (user.role == "fisioterapeuta") {
                btnChangeRole.text = "Hacer Paciente"
            } else {
                btnChangeRole.text = "Hacer Fisioterapeuta"
            }

            // Ocultar botón si es administrador (no queremos que se degraden a sí mismos accidentalmente)
            if (!showActions || user.role == "administrador") {
                btnChangeRole.visibility = View.GONE
            } else {
                btnChangeRole.visibility = View.VISIBLE
                btnChangeRole.setOnClickListener {
                    onRoleChangeClick(user)
                }
            }

            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}

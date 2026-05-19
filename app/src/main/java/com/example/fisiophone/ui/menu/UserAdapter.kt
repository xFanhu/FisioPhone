package com.example.fisiophone.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fisiophone.R
import com.example.fisiophone.databinding.ItemUserBinding
import com.google.android.material.button.MaterialButton

data class User(
    val id: String,
    val nombre: String,
    val apellidos: String,
    val email: String,
    val dni: String,
    val telefono: String,
    var role: String,
    val treatments: List<String> = emptyList(),
    val photoUrl: String? = null
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
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    inner class UserViewHolder(private val binding: ItemUserBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: User) {
            binding.tvUserName.text = "${user.nombre} ${user.apellidos}"
            binding.tvUserRole.text = user.role.replaceFirstChar { it.uppercase() }
            binding.tvUserEmail.text = user.email
            binding.tvUserDni.text = "${itemView.context.getString(R.string.DNI)}: ${user.dni}"
            
            if (!user.photoUrl.isNullOrEmpty()) {
                binding.ivUserPhoto.setPadding(0, 0, 0, 0)
                binding.ivUserPhoto.imageTintList = null
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(user.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(binding.ivUserPhoto)
            } else {
                val paddingPx = itemView.context.resources.getDimensionPixelSize(R.dimen.avatar_list_padding)
                binding.ivUserPhoto.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                binding.ivUserPhoto.setImageResource(R.drawable.ic_profile)
                binding.ivUserPhoto.imageTintList = android.content.res.ColorStateList.valueOf(
                    itemView.context.getColor(R.color.azul)
                )
            }

            if (user.telefono.isNotEmpty()) {
                binding.tvUserPhone.visibility = View.VISIBLE
                binding.tvUserPhone.text = "${itemView.context.getString(R.string.telefono)}: ${user.telefono}"
            } else {
                binding.tvUserPhone.visibility = View.GONE
            }

            if (user.role == "fisioterapeuta") {
                binding.btnChangeRole.setText(R.string.hacer_paciente)
            } else {
                binding.btnChangeRole.setText(R.string.hacer_fisioterapeuta)
            }

            // Oculta botón si es administrador o en PatientsFragment
            if (!showActions || user.role == "administrador") {
                binding.btnChangeRole.visibility = View.GONE
            } else {
                binding.btnChangeRole.visibility = View.VISIBLE
                binding.btnChangeRole.setOnClickListener {
                    onRoleChangeClick(user)
                }
            }

            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}

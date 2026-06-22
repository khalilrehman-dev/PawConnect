package com.example.authapp.ui.discover

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.authapp.R
import com.example.authapp.model.Pet

class DiscoverAdapter(
    private val onPetClick: (Pet) -> Unit
) : RecyclerView.Adapter<DiscoverAdapter.DiscoverViewHolder>() {

    private val pets = mutableListOf<Pet>()

    fun submitList(newPets: List<Pet>) {
        pets.clear()
        pets.addAll(newPets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_pet, parent, false)
        return DiscoverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiscoverViewHolder, position: Int) {
        holder.bind(pets[position])
    }

    override fun getItemCount() = pets.size

    inner class DiscoverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPet: ImageView  = itemView.findViewById(R.id.ivPetImage)
        private val tvName: TextView  = itemView.findViewById(R.id.tvPetName)
        private val tvBreed: TextView = itemView.findViewById(R.id.tvPetBreed)

        fun bind(pet: Pet) {
            tvName.text  = pet.name
            tvBreed.text = "${pet.species} • ${pet.breed}"

            if (pet.imageUrl.isNotEmpty()) {
                ivPet.load(pet.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_pet_placeholder)
                    error(R.drawable.ic_pet_placeholder)
                }
            } else {
                ivPet.setImageResource(R.drawable.ic_pet_placeholder)
            }

            itemView.setOnClickListener { onPetClick(pet) }
        }
    }
}

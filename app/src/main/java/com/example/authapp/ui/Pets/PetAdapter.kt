package com.example.authapp.ui.pets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.authapp.R
import com.example.authapp.model.Pet

class PetAdapter(
    private val onPetClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    private val pets = mutableListOf<Pet>()

    fun submitList(newPets: List<Pet>) {
        pets.clear()
        pets.addAll(newPets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(pets[position])
    }

    override fun getItemCount() = pets.size

    inner class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPet: ImageView   = itemView.findViewById(R.id.ivPetImage)
        private val tvName: TextView   = itemView.findViewById(R.id.tvPetName)
        private val tvBreed: TextView  = itemView.findViewById(R.id.tvPetBreed)
        private val tvAge: TextView    = itemView.findViewById(R.id.tvPetAge)

        fun bind(pet: Pet) {
            tvName.text  = pet.name
            tvBreed.text = "${pet.species} • ${pet.breed}"
            tvAge.text   = "${pet.age} yr${if (pet.age != 1) "s" else ""} • ${pet.gender}"

            ivPet.load(pet.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_pet_placeholder)
                error(R.drawable.ic_pet_placeholder)
                transformations(RoundedCornersTransformation(12f))
            }

            itemView.setOnClickListener { onPetClick(pet) }
        }
    }
}

package com.example.authapp.ui.Vets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.authapp.R
import com.example.authapp.model.Vet

class VetAdapter(
    private val onVetClick: (Vet) -> Unit
) : RecyclerView.Adapter<VetAdapter.VetViewHolder>() {

    private val vets = mutableListOf<Vet>()

    fun submitList(newVets: List<Vet>) {
        vets.clear()
        vets.addAll(newVets)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vet, parent, false)
        return VetViewHolder(view)
    }

    override fun onBindViewHolder(holder: VetViewHolder, position: Int) =
        holder.bind(vets[position])

    override fun getItemCount() = vets.size

    inner class VetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView        = itemView.findViewById(R.id.ivVetPhoto)
        private val tvName: TextView          = itemView.findViewById(R.id.tvVetName)
        private val tvClinic: TextView        = itemView.findViewById(R.id.tvClinicName)
        private val tvCity: TextView          = itemView.findViewById(R.id.tvCity)
        private val tvSpecialization: TextView = itemView.findViewById(R.id.tvSpecialization)
        private val tvYears: TextView = itemView.findViewById(R.id.tvYears)

        fun bind(vet: Vet) {
            tvName.text           = "Dr. ${vet.displayName}"
            tvClinic.text         = vet.clinicName
            tvCity.text           = vet.city
            tvSpecialization.text = vet.specialization
            tvYears.text = "${vet.yearsOfExperience} yrs"

            ivPhoto.load(vet.profileImageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_pet_placeholder)
                error(R.drawable.ic_pet_placeholder)
                transformations(RoundedCornersTransformation(12f))            }

            itemView.setOnClickListener { onVetClick(vet) }
        }
    }
}
package com.example.authapp.ui.Vets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.authapp.R
import com.example.authapp.model.Vet


class VetAdapter(
    private val onVetClick: (Vet) -> Unit
) : RecyclerView.Adapter<VetAdapter.VetViewHolder>() {

    private val vets =
        mutableListOf<Vet>()


    fun submitList(
        newVets: List<Vet>
    ) {

        vets.clear()

        vets.addAll(
            newVets
        )

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VetViewHolder {

        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(
                    R.layout.item_vet,
                    parent,
                    false
                )


        return VetViewHolder(
            view
        )
    }


    override fun onBindViewHolder(
        holder: VetViewHolder,
        position: Int
    ) {

        holder.bind(
            vets[position]
        )
    }


    override fun getItemCount():
            Int {

        return vets.size
    }


    inner class VetViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(
        itemView
    ) {

        private val ivPhoto:
                ImageView =
            itemView.findViewById(
                R.id.ivVetPhoto
            )


        private val tvName:
                TextView =
            itemView.findViewById(
                R.id.tvVetName
            )


        private val tvClinic:
                TextView =
            itemView.findViewById(
                R.id.tvClinicName
            )


        private val tvCity:
                TextView =
            itemView.findViewById(
                R.id.tvCity
            )


        private val tvSpecialization:
                TextView =
            itemView.findViewById(
                R.id.tvSpecialization
            )


        private val tvYears:
                TextView =
            itemView.findViewById(
                R.id.tvYears
            )


        private val tvAvailability:
                TextView =
            itemView.findViewById(
                R.id.tvAvailability
            )


        fun bind(
            vet: Vet
        ) {

            val cleanName =
                vet.displayName
                    .trim()
                    .removePrefix("Dr.")
                    .trim()


            tvName.text =
                if (cleanName.isBlank()) {
                    "Veterinarian"
                } else {
                    "Dr. $cleanName"
                }


            tvClinic.text =
                vet.clinicName
                    .ifBlank {
                        "Clinic not provided"
                    }


            tvCity.text =
                vet.city
                    .ifBlank {
                        "Location not provided"
                    }


            tvSpecialization.text =
                vet.specialization
                    .ifBlank {
                        "General Practice"
                    }


            tvYears.text =
                when (vet.yearsOfExperience) {

                    1 ->
                        "1 yr experience"

                    else ->
                        "${vet.yearsOfExperience} yrs experience"
                }


            if (vet.isAvailable) {

                tvAvailability.text =
                    "Available"

                tvAvailability.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.paw_success
                    )
                )

            } else {

                tvAvailability.text =
                    "Unavailable"

                tvAvailability.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.paw_text_tertiary
                    )
                )
            }


            if (
                vet.profileImageUrl.isBlank()
            ) {

                ivPhoto.setImageResource(
                    R.drawable.ic_profile
                )

            } else {

                ivPhoto.load(
                    vet.profileImageUrl
                ) {

                    crossfade(
                        true
                    )

                    placeholder(
                        R.drawable.ic_profile
                    )

                    error(
                        R.drawable.ic_profile
                    )
                }
            }


            itemView.setOnClickListener {

                onVetClick(
                    vet
                )
            }
        }
    }
}
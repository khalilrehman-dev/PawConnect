package com.example.authapp.model

data class Appointment(
    val id: String = "",
    val petOwnerId: String = "",
    val vetId: String = "",
    val vetName: String = "",
    val clinicName: String = "",
    val petId: String = "",
    val petName: String = "",
    val date: String = "",
    val time: String = "",
    val note: String = "",
    val status: String = "pending",   // pending / accepted / rejected
    val createdAt: Long = 0L
)
package com.example.authapp.model

data class Pet(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val species: String = "",       // Dog, Cat, Bird, Rabbit, Other
    val breed: String = "",
    val age: Int = 0,
    val gender: String = "",        // Male, Female
    val description: String = "",
    val imageUrl: String = "",
    val createdAt: Long = 0L
)

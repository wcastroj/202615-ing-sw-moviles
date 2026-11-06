package com.example.vinyls_application.models

/** Tipo de artista según el endpoint del que proviene (`/bands` o `/musicians`). */
enum class PerformerType { BAND, MUSICIAN }

data class Performer(
    val id: Int,
    val name: String,
    val image: String,
    val description: String,
    val type: PerformerType,
    val birthDate: String? = null,
    val creationDate: String? = null,
)

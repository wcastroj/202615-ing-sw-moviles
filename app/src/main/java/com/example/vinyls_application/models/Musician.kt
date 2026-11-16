package com.example.vinyls_application.models

data class Musician(
    override val id: Int,
    override val name: String,
    override val image: String,
    override val description: String,
    override val albums: List<Album>,
    override val performerPrizes: List<PerformerPrize>,
    val birthDate: String
) : Performer
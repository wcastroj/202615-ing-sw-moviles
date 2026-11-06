package com.example.vinyls_application.models

data class Band(
    override val id: Int,
    override val name: String,
    override val image: String,
    override val description: String,
    override val albums: List<Album>,
    override val performerPrizes: List<PerformerPrize>,
    val creationDate: String
) : PerformerBase
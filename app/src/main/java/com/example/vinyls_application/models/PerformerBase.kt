package com.example.vinyls_application.models

data class PerformerPrize(
    val id: Int,
    val premiationDate: String,
)

interface PerformerBase {
    val id: Int
    val name: String
    val image: String
    val description: String
    val albums: List<Album>
    val performerPrizes: List<PerformerPrize>
}
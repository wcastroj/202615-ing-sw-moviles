package com.example.vinyls_application.models

data class PerformerPrize(
    val id: Int,
    val premiationDate: String,
)

/**
 * Artista del catálogo. Se usa tanto en listas (catálogo de artistas, performers de un álbum)
 * como en el detalle. Sus únicas implementaciones son [Band] y [Musician].
 */
sealed interface Performer {
    val id: Int
    val name: String
    val image: String
    val description: String
    val albums: List<Album>
    val performerPrizes: List<PerformerPrize>

    /** Endpoint al que pertenece el artista (`/bands` o `/musicians`); se usa para navegar al detalle. */
    val type: PerformerType
        get() = when (this) {
            is Band -> PerformerType.BAND
            is Musician -> PerformerType.MUSICIAN
        }

    /** Fecha representativa del artista: `creationDate` para una banda, `birthDate` para un músico. */
    val date: String
        get() = when (this) {
            is Band -> creationDate
            is Musician -> birthDate
        }
}

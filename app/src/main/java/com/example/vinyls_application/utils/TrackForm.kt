package com.example.vinyls_application.utils

// Utilidades del formulario para asociar una canción a un álbum: reglas de validación.
// Son funciones puras, sin dependencias de Android.

/** Un enum no crea objetos al validar: la validación corre con cada tecla. */
enum class TrackFieldError {
    NONE, REQUIRED, NAME_TOO_LONG, INVALID_CHARACTERS, ONLY_SYMBOLS, INVALID_DURATION, ZERO_DURATION
}

/**
 * Reglas del formulario de asociar canción. Recibe [CharSequence] para validar el texto del
 * campo sin convertirlo a String en cada tecla.
 */
object TrackFormValidator {
    const val MAX_NAME_LENGTH = 50

    /** Letras de cualquier idioma (con tildes, ñ, ü), números, espacios y ' - . , ( ) & ! ? ¡ ¿ : */
    private val ALLOWED_NAME = Regex("""^[\p{L}\p{M}\p{N} '\-.,()&!?¡¿:]+$""")

    /** Evita nombres hechos solo de símbolos, como "---" o "?!". */
    private val LETTER_OR_DIGIT = Regex("""[\p{L}\p{N}]""")

    fun validateName(value: CharSequence): TrackFieldError {
        if (value.isBlank()) return TrackFieldError.REQUIRED
        val name = value.trim()
        return when {
            name.length > MAX_NAME_LENGTH -> TrackFieldError.NAME_TOO_LONG
            !ALLOWED_NAME.matches(name) -> TrackFieldError.INVALID_CHARACTERS
            !LETTER_OR_DIGIT.containsMatchIn(name) -> TrackFieldError.ONLY_SYMBOLS
            else -> TrackFieldError.NONE
        }
    }

    /** mm:ss con el primer 0 de los minutos opcional: 04:35, 4:35 y 59:59 son válidos. */
    private val DURATION = Regex("""^[0-5]?[0-9]:[0-5][0-9]$""")

    /** 0:00 y 00:00: cumplen el formato, pero una canción no puede durar cero segundos. */
    private val ZERO = Regex("""^0?0:00$""")

    fun validateDuration(value: CharSequence): TrackFieldError {
        if (value.isBlank()) return TrackFieldError.REQUIRED
        val duration = value.trim()
        return when {
            !DURATION.matches(duration) -> TrackFieldError.INVALID_DURATION
            ZERO.matches(duration) -> TrackFieldError.ZERO_DURATION
            else -> TrackFieldError.NONE
        }
    }
}

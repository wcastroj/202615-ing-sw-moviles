package com.example.vinyls_application.utils

import com.example.vinyls_application.models.Album
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Utilidades del formulario de creación de álbum (HU05): reglas de validación y
// conversión de la fecha de lanzamiento. Son funciones puras, sin dependencias de Android.

/** Un enum no crea objetos al validar: la validación corre con cada tecla. */
enum class AlbumFieldError { NONE, REQUIRED, INVALID_COVER, NAME_TOO_LONG, DESCRIPTION_TOO_LONG }

data class AlbumFormErrors(
    val cover: AlbumFieldError = AlbumFieldError.NONE,
    val name: AlbumFieldError = AlbumFieldError.NONE,
    val recordLabel: AlbumFieldError = AlbumFieldError.NONE,
    val genre: AlbumFieldError = AlbumFieldError.NONE,
    val releaseDate: AlbumFieldError = AlbumFieldError.NONE,
    val description: AlbumFieldError = AlbumFieldError.NONE
) {
    val hasErrors: Boolean
        get() = this != NO_ERRORS

    private companion object {
        val NO_ERRORS = AlbumFormErrors()
    }
}

/**
 * Reglas del formulario de creación de álbum. Recibe [CharSequence] para validar el texto del
 * campo sin convertirlo a String en cada tecla.
 */
object AlbumFormValidator {
    const val MAX_NAME_LENGTH = 50
    const val MAX_DESCRIPTION_LENGTH = 250

    private val COVER_URL = Regex("""^https?://\S+\.(png|jpe?g|webp)$""", RegexOption.IGNORE_CASE)

    fun validateName(value: CharSequence): AlbumFieldError =
        validateText(value, MAX_NAME_LENGTH, AlbumFieldError.NAME_TOO_LONG)

    fun validateDescription(value: CharSequence): AlbumFieldError =
        validateText(value, MAX_DESCRIPTION_LENGTH, AlbumFieldError.DESCRIPTION_TOO_LONG)

    fun validateCover(value: CharSequence): AlbumFieldError = when {
        value.isBlank() -> AlbumFieldError.REQUIRED
        COVER_URL.matches(value.trim()) -> AlbumFieldError.NONE
        else -> AlbumFieldError.INVALID_COVER
    }

    /** Para los campos que se escogen: fecha, género y sello discográfico. */
    fun validateRequired(value: CharSequence): AlbumFieldError =
        if (value.isBlank()) AlbumFieldError.REQUIRED else AlbumFieldError.NONE

    fun validate(album: Album) = AlbumFormErrors(
        cover = validateCover(album.cover),
        name = validateName(album.name),
        recordLabel = validateRequired(album.recordLabel),
        genre = validateRequired(album.genre),
        releaseDate = validateRequired(album.releaseDate),
        description = validateDescription(album.description)
    )

    private fun validateText(
        value: CharSequence,
        maxLength: Int,
        tooLongError: AlbumFieldError
    ): AlbumFieldError = when {
        value.isBlank() -> AlbumFieldError.REQUIRED
        // Solo se recorta el texto si, tal como está, ya supera el límite
        value.length > maxLength && value.trim().length > maxLength -> tooLongError
        else -> AlbumFieldError.NONE
    }
}

/**
 * Convierte la fecha de lanzamiento entre el selector, el campo (`dd/MM/yyyy`) y el backend (ISO).
 *
 * Todo se hace en UTC, igual que MaterialDatePicker, para que la zona horaria no corra la fecha un
 * día. Se usa SimpleDateFormat porque java.time no está disponible en minSdk 21; como no es seguro
 * entre hilos, se crea una instancia en cada uso.
 */
object AlbumDateFormatter {
    private const val DISPLAY_PATTERN = "dd/MM/yyyy"

    fun toDisplay(utcMillis: Long): String = displayFormat().format(utcMillis)

    fun toMillis(display: String): Long? = try {
        displayFormat().parse(display)?.time
    } catch (e: ParseException) {
        null
    }

    /** `"10/05/2024"` → `"2024-05-10T00:00:00.000Z"`. Si no hay fecha válida devuelve `""`. */
    fun toIso(display: String): String {
        val parts = display.split('/')
        if (parts.size != 3) return ""
        return "${parts[2]}-${parts[1]}-${parts[0]}T00:00:00.000Z"
    }

    private fun displayFormat() = SimpleDateFormat(DISPLAY_PATTERN, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
}

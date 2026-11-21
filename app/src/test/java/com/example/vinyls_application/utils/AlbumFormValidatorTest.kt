package com.example.vinyls_application.utils

import com.example.vinyls_application.models.Genre
import com.example.vinyls_application.models.RecordLabel
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas unitarias de HU05: reglas de cada campo del formulario de creación de álbum. */
class AlbumFormValidatorTest {

    // PU-023: Nombre válido (positiva).
    @Test
    fun pu023_nombre_valido() {
        assertEquals(AlbumFieldError.NONE, AlbumFormValidator.validateName("Ecos del Atardecer"))
    }

    // PU-024: Nombre con 50 caracteres, el límite (positiva).
    @Test
    fun pu024_nombre_con_50_caracteres() {
        assertEquals(AlbumFieldError.NONE, AlbumFormValidator.validateName("a".repeat(50)))
    }

    // PU-025: Nombre con 51 caracteres (negativa).
    @Test
    fun pu025_nombre_con_51_caracteres() {
        assertEquals(AlbumFieldError.NAME_TOO_LONG, AlbumFormValidator.validateName("a".repeat(51)))
    }

    // PU-026: Nombre vacío (negativa).
    @Test
    fun pu026_nombre_vacio() {
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateName(""))
    }

    // PU-027: Nombre con solo espacios (negativa).
    @Test
    fun pu027_nombre_con_solo_espacios() {
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateName("   "))
    }

    // PU-028: Cover con URL terminada en una extensión permitida (positiva).
    // .svg no se incluye: se retiró de la HU porque Glide no puede mostrarlo (ver PU-029).
    @Test
    fun pu028_cover_con_extension_permitida() {
        listOf("png", "jpg", "jpeg", "webp", "PNG", "JpEg").forEach { extension ->
            assertEquals(
                extension,
                AlbumFieldError.NONE,
                AlbumFormValidator.validateCover("https://example.com/portada.$extension")
            )
        }
    }

    // PU-029: Cover con extensión no permitida (negativa).
    @Test
    fun pu029_cover_con_extension_no_permitida() {
        listOf(
            "https://example.com/portada.gif",
            "https://example.com/portada.svg",
            "https://example.com/portada.png?size=200",
            "https://example.com/portada"
        ).forEach { url ->
            assertEquals(url, AlbumFieldError.INVALID_COVER, AlbumFormValidator.validateCover(url))
        }
    }

    // PU-030: Cover con texto que no es URL (negativa).
    @Test
    fun pu030_cover_con_texto_que_no_es_url() {
        listOf(
            "esto no es una url",
            "portada.png",
            "ftp://example.com/portada.png",
            "https://.png",
            "https://example.com/mi portada.png"
        ).forEach { value ->
            assertEquals(value, AlbumFieldError.INVALID_COVER, AlbumFormValidator.validateCover(value))
        }
    }

    // PU-031: Cover vacío (negativa).
    @Test
    fun pu031_cover_vacio() {
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateCover(""))
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateCover("   "))
    }

    // PU-032: Fecha de lanzamiento seleccionada (positiva).
    @Test
    fun pu032_fecha_de_lanzamiento_seleccionada() {
        val releaseDate = AlbumDateFormatter.toIso("10/05/2024")

        assertEquals(AlbumFieldError.NONE, AlbumFormValidator.validateRequired(releaseDate))
    }

    // PU-033: Fecha de lanzamiento vacía (negativa).
    @Test
    fun pu033_fecha_de_lanzamiento_vacia() {
        val releaseDate = AlbumDateFormatter.toIso("")

        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateRequired(releaseDate))
    }

    // PU-034: Descripción con 250 caracteres, el límite (positiva).
    @Test
    fun pu034_descripcion_con_250_caracteres() {
        assertEquals(AlbumFieldError.NONE, AlbumFormValidator.validateDescription("a".repeat(250)))
    }

    // PU-035: Descripción con 251 caracteres (negativa).
    @Test
    fun pu035_descripcion_con_251_caracteres() {
        assertEquals(
            AlbumFieldError.DESCRIPTION_TOO_LONG,
            AlbumFormValidator.validateDescription("a".repeat(251))
        )
    }

    // PU-036: Descripción vacía (negativa).
    @Test
    fun pu036_descripcion_vacia() {
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateDescription(""))
    }

    // PU-037: Descripción con solo espacios (negativa).
    @Test
    fun pu037_descripcion_con_solo_espacios() {
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateDescription(" \n "))
    }

    // PU-038: Género seleccionado (positiva).
    @Test
    fun pu038_genero_seleccionado() {
        assertEquals(AlbumFieldError.NONE, AlbumFormValidator.validateRequired(Genre.ROCK.apiValue))
    }

    // PU-039: Género vacío (negativa).
    @Test
    fun pu039_genero_vacio() {
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateRequired(""))
    }

    // PU-040: Sello discográfico seleccionado (positiva).
    @Test
    fun pu040_sello_discografico_seleccionado() {
        assertEquals(
            AlbumFieldError.NONE,
            AlbumFormValidator.validateRequired(RecordLabel.FANIA_RECORDS.apiValue)
        )
    }

    // PU-041: Sello discográfico vacío (negativa).
    @Test
    fun pu041_sello_discografico_vacio() {
        assertEquals(AlbumFieldError.REQUIRED, AlbumFormValidator.validateRequired(""))
    }

}

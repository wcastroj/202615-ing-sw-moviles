package com.example.vinyls_application.network

import com.example.vinyls_application.models.PerformerType
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformerParserTest {

    // Respuesta de GET /bands: trae creationDate (y listas anidadas que el parser ignora)
    private val bandsJson = """
        [
          {
            "id": 1,
            "name": "Queen",
            "image": "https://example.com/queen.jpg",
            "description": "Banda británica de rock",
            "creationDate": "1970-01-01T00:00:00.000Z",
            "albums": [],
            "musicians": [],
            "performerPrizes": []
          },
          {
            "id": 2,
            "name": "Los Van Van",
            "image": "https://example.com/vanvan.jpg",
            "description": "Orquesta cubana",
            "creationDate": "1969-12-04T05:00:00.000Z",
            "albums": [],
            "musicians": [],
            "performerPrizes": []
          }
        ]
    """.trimIndent()

    // Respuesta de GET /musicians: trae birthDate en lugar de creationDate
    private val musiciansJson = """
        [
          {
            "id": 100,
            "name": "Rubén Blades Bellido de Luna",
            "image": "https://example.com/ruben.jpg",
            "description": "Cantante, compositor y actor panameño",
            "birthDate": "1948-07-16T00:00:00.000Z",
            "albums": [],
            "performerPrizes": []
          },
          {
            "id": 101,
            "name": "Celia Cruz",
            "image": "https://example.com/celia.jpg",
            "description": "Reina de la salsa",
            "birthDate": "1925-10-21T00:00:00.000Z",
            "albums": [],
            "performerPrizes": []
          }
        ]
    """.trimIndent()

    // PU-011: Una banda se convierte con nombre, imagen y fecha de creación (positiva).
    @Test
    fun pu011_banda_se_convierte_con_nombre_imagen_y_fecha_de_creacion() {
        val bands = NetworkServiceAdapter.parseBands(bandsJson)

        assertEquals(2, bands.size)
        assertEquals(1, bands[0].id)
        assertEquals("Queen", bands[0].name)
        assertEquals("https://example.com/queen.jpg", bands[0].image)
        assertEquals("Banda británica de rock", bands[0].description)
        assertEquals("1970-01-01T00:00:00.000Z", bands[0].creationDate)

        assertEquals(2, bands[1].id)
        assertEquals("Los Van Van", bands[1].name)
        assertEquals("https://example.com/vanvan.jpg", bands[1].image)
        assertEquals("1969-12-04T05:00:00.000Z", bands[1].creationDate)
    }

    // PU-012: Un músico se convierte con nombre, imagen y fecha de nacimiento (positiva).
    @Test
    fun pu012_musico_se_convierte_con_nombre_imagen_y_fecha_de_nacimiento() {
        val musicians = NetworkServiceAdapter.parseMusicians(musiciansJson)

        assertEquals(2, musicians.size)
        assertEquals(100, musicians[0].id)
        assertEquals("Rubén Blades Bellido de Luna", musicians[0].name)
        assertEquals("https://example.com/ruben.jpg", musicians[0].image)
        assertEquals("Cantante, compositor y actor panameño", musicians[0].description)
        assertEquals("1948-07-16T00:00:00.000Z", musicians[0].birthDate)

        assertEquals(101, musicians[1].id)
        assertEquals("Celia Cruz", musicians[1].name)
        assertEquals("https://example.com/celia.jpg", musicians[1].image)
        assertEquals("1925-10-21T00:00:00.000Z", musicians[1].birthDate)
    }

    // PU-014: Cada artista queda identificado como Band o Musician (positiva).
    @Test
    fun pu014_cada_artista_queda_identificado_como_band_o_musician() {
        val bands = NetworkServiceAdapter.parseBands(bandsJson)
        val musicians = NetworkServiceAdapter.parseMusicians(musiciansJson)

        assertTrue(bands.all { it.type == PerformerType.BAND })
        assertTrue(musicians.all { it.type == PerformerType.MUSICIAN })
    }

    // PU-014: En el detalle de un álbum los artistas vienen mezclados y también se identifican.
    @Test
    fun pu014_artistas_de_un_album_se_identifican_por_su_fecha() {
        val albumDetailJson = """
            {
              "id": 100, "name": "Buscando América", "cover": "", "releaseDate": "",
              "description": "", "genre": "Salsa", "recordLabel": "Elektra",
              "tracks": [], "comments": [],
              "performers": [
                { "id": 100, "name": "Rubén Blades", "image": "", "description": "",
                  "birthDate": "1948-07-16T00:00:00.000Z" },
                { "id": 1, "name": "Queen", "image": "", "description": "",
                  "creationDate": "1970-01-01T00:00:00.000Z" }
              ]
            }
        """.trimIndent()

        val performers = NetworkServiceAdapter.parseAlbumDetail(albumDetailJson).performers

        assertEquals(PerformerType.MUSICIAN, performers[0].type)
        assertEquals(PerformerType.BAND, performers[1].type)
    }

    // PU-015: Artista con campos vacíos se convierte sin fallar (negativa / robustez de datos).
    @Test
    fun pu015_artista_con_campos_vacios() {
        val emptyFieldsJson = """
            [{ "id": 3, "name": "", "image": "", "description": "", "creationDate": "" }]
        """.trimIndent()

        val bands = NetworkServiceAdapter.parseBands(emptyFieldsJson)

        assertEquals(1, bands.size)
        assertEquals(3, bands[0].id)
        assertEquals("", bands[0].name)
        assertEquals("", bands[0].image)
        assertEquals("", bands[0].description)
        assertEquals("", bands[0].creationDate)
        assertEquals(PerformerType.BAND, bands[0].type)
    }

    // PU-015: Banda sin fecha de creación lanza excepción (negativa / robustez de datos).
    @Test(expected = JSONException::class)
    fun pu015_banda_sin_fecha_de_creacion_lanza_excepcion() {
        val missingCreationDate = """
            [{ "id": 3, "name": "Sin fecha", "image": "", "description": "" }]
        """.trimIndent()

        NetworkServiceAdapter.parseBands(missingCreationDate)
    }

    // PU-015: Músico sin fecha de nacimiento lanza excepción (negativa / robustez de datos).
    @Test(expected = JSONException::class)
    fun pu015_musico_sin_fecha_de_nacimiento_lanza_excepcion() {
        val missingBirthDate = """
            [{ "id": 102, "name": "Sin fecha", "image": "", "description": "" }]
        """.trimIndent()

        NetworkServiceAdapter.parseMusicians(missingBirthDate)
    }

    // PU-015: Artista con solo el id lanza excepción (negativa / robustez de datos).
    @Test(expected = JSONException::class)
    fun pu015_artista_con_campos_faltantes_lanza_excepcion() {
        NetworkServiceAdapter.parseMusicians("""[{ "id": 103 }]""")
    }

    // PU-016: Lista vacía de bandas y de músicos devuelve listas vacías (negativa).
    @Test
    fun pu016_lista_vacia_devuelve_listas_vacias() {
        val bands = NetworkServiceAdapter.parseBands("[]")
        val musicians = NetworkServiceAdapter.parseMusicians("[]")

        assertNotNull(bands)
        assertTrue(bands.isEmpty())
        assertNotNull(musicians)
        assertTrue(musicians.isEmpty())
    }

    // PU-017: Respuesta inválida de la API lanza excepción (negativa).
    @Test(expected = JSONException::class)
    fun pu017_json_invalido_de_bandas_lanza_excepcion() {
        NetworkServiceAdapter.parseBands("invalid_json_string")
    }

    // PU-017: Respuesta inválida de la API lanza excepción (negativa).
    @Test(expected = JSONException::class)
    fun pu017_json_invalido_de_musicos_lanza_excepcion() {
        NetworkServiceAdapter.parseMusicians("invalid_json_string")
    }
}

package com.example.vinyls_application.network

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumParserTest {
    private val albumJson = """
       [{
          "id": 100,
          "name": "Buscando América",
          "cover": "https://example.com/cover.jpg",
          "releaseDate": "1984-08-01T00:00:00.000Z",
          "description": "Álbum de prueba",
          "genre": "Salsa",
          "recordLabel": "Elektra",
          "tracks": [
            { "id": 1, "name": "Decisiones", "duration": "5:05" },
            { "id": 2, "name": "Desapariciones", "duration": "6:29" },
            { "id": 3, "name": "GDBD", "duration": "4:53" }
          ],
          "performers": [],
          "comments": []
        }]
    """.trimIndent()

    private val multipleAlbumsJson = """
        [
          {
            "id": 100,
            "name": "Buscando América",
            "cover": "https://example.com/cover.jpg",
            "releaseDate": "1984-08-01T00:00:00.000Z",
            "description": "Álbum de prueba",
            "genre": "Salsa",
            "recordLabel": "Elektra"
          },
          {
            "id": 101,
            "name": "Poeta del Pueblo",
            "cover": "https://example.com/cover2.jpg",
            "releaseDate": "1990-01-01T00:00:00.000Z",
            "description": "Otro álbum",
            "genre": "Salsa",
            "recordLabel": "Fania"
          }
        ]
    """.trimIndent()

    @Test
    fun test_parse_album_json() {
        val response = albumJson

        val album = NetworkServiceAdapter.parseAlbums(response)

        assertEquals(100, album[0].albumId)
        assertEquals("https://example.com/cover.jpg", album[0].cover)
        assertEquals("Buscando América", album[0].name)
        assertEquals("Salsa", album[0].genre)
        assertEquals("Elektra", album[0].recordLabel)
        assertEquals("1984-08-01T00:00:00.000Z", album[0].releaseDate)
        assertEquals("Álbum de prueba", album[0].description)
    }

    // PU-001: El JSON de la API se convierte en una lista de álbumes (positiva).
    @Test
    fun pu001_json_api_se_convierte_en_lista_de_albumes() {
        val albums = NetworkServiceAdapter.parseAlbums(multipleAlbumsJson)

        assertNotNull(albums)
        assertEquals(2, albums.size)
        assertEquals(100, albums[0].albumId)
        assertEquals("Buscando América", albums[0].name)
        assertEquals(101, albums[1].albumId)
        assertEquals("Poeta del Pueblo", albums[1].name)
    }

    // PU-002: Cada álbum trae imagen, nombre y género (positiva).
    @Test
    fun pu002_cada_album_trae_imagen_nombre_y_genero() {
        val albums = NetworkServiceAdapter.parseAlbums(multipleAlbumsJson)

        assertEquals("https://example.com/cover.jpg", albums[0].cover)
        assertEquals("Buscando América", albums[0].name)
        assertEquals("Salsa", albums[0].genre)

        assertEquals("https://example.com/cover2.jpg", albums[1].cover)
        assertEquals("Poeta del Pueblo", albums[1].name)
        assertEquals("Salsa", albums[1].genre)
    }

    // PU-003: Álbum con campos vacíos o nulos (negativa / robustez de datos).
    @Test
    fun pu003_album_con_campos_vacios_o_nulos() {
        val emptyFieldsJson = """
            [{
              "id": 102,
              "name": "",
              "cover": "",
              "releaseDate": "",
              "description": "",
              "genre": "",
              "recordLabel": ""
            }]
        """.trimIndent()

        val albums = NetworkServiceAdapter.parseAlbums(emptyFieldsJson)

        assertEquals(1, albums.size)
        assertEquals(102, albums[0].albumId)
        assertEquals("", albums[0].name)
        assertEquals("", albums[0].cover)
        assertEquals("", albums[0].genre)
        assertEquals("", albums[0].releaseDate)
        assertEquals("", albums[0].description)
        assertEquals("", albums[0].recordLabel)
    }

    @Test(expected = JSONException::class)
    fun pu003_album_con_campo_faltante_lanza_excepcion() {
        val missingFieldsJson = """
            [{
              "id": 103
            }]
        """.trimIndent()

        NetworkServiceAdapter.parseAlbums(missingFieldsJson)
    }

    // PU-004: Lista vacía genera el estado sin álbumes (negativa).
    @Test
    fun pu004_lista_vacia_genera_estado_sin_albumes() {
        val emptyJson = "[]"

        val albums = NetworkServiceAdapter.parseAlbums(emptyJson)

        assertNotNull(albums)
        assertTrue(albums.isEmpty())
    }

    // PU-005: Error de la API o JSON inválido lanza excepción (negativa).
    @Test(expected = JSONException::class)
    fun pu005_error_api_o_json_invalido_lanza_excepcion() {
        val invalidJson = "invalid_json_string"

        NetworkServiceAdapter.parseAlbums(invalidJson)
    }
}

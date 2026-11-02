package com.example.vinyls_application.network

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDetailParserTest {

    private val albumDetailJson = """
        {
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
          "performers": [
            {
              "id": 1,
              "name": "Rubén Blades",
              "image": "https://example.com/ruben.jpg",
              "description": "Cantautor panameño",
              "birthDate": "1948-07-16T00:00:00.000Z"
            },
            {
              "id": 2,
              "name": "Grupo Niche",
              "image": "https://example.com/niche.jpg",
              "description": "Orquesta de salsa",
              "creationDate": "1978-01-01T00:00:00.000Z"
            }
          ],
          "comments": [
            { "id": 1, "description": "Excelente álbum", "rating": "5" }
          ]
        }
    """.trimIndent()

    // PU-006: El detalle se convierte con nombre, género y número de canciones
    @Test
    fun pu006_detalle_convierte_nombre_genero_y_numero_de_canciones() {
        // Arrange
        val response = albumDetailJson

        // Act
        val albumDetail = NetworkServiceAdapter.parseAlbumDetail(response)

        // Assert
        assertEquals("Buscando América", albumDetail.name)
        assertEquals("Salsa", albumDetail.genre)
        assertEquals(3, albumDetail.tracks.size)
    }

    // PU-007: Las canciones se convierten con id, nombre y duración (positiva).
    @Test
    fun pu007_las_canciones_se_convierten_con_id_nombre_y_duracion() {
        val albumDetail = NetworkServiceAdapter.parseAlbumDetail(albumDetailJson)

        val tracks = albumDetail.tracks
        assertEquals(3, tracks.size)

        assertEquals(1, tracks[0].trackId)
        assertEquals("Decisiones", tracks[0].name)
        assertEquals("5:05", tracks[0].duration)

        assertEquals(2, tracks[1].trackId)
        assertEquals("Desapariciones", tracks[1].name)
        assertEquals("6:29", tracks[1].duration)

        assertEquals(3, tracks[2].trackId)
        assertEquals("GDBD", tracks[2].name)
        assertEquals("4:53", tracks[2].duration)
    }

    // PU-008: Los artistas se convierten con id, nombre y fecha de nacimiento o creación (positiva).
    @Test
    fun pu008_los_artistas_se_convierten_con_id_nombre_y_fecha_de_nacimiento_o_creacion() {
        val albumDetail = NetworkServiceAdapter.parseAlbumDetail(albumDetailJson)

        val performers = albumDetail.performers
        assertEquals(2, performers.size)

        // Artista con fecha de nacimiento (birthDate)
        assertEquals(1, performers[0].id)
        assertEquals("Rubén Blades", performers[0].name)
        assertEquals("1948-07-16T00:00:00.000Z", performers[0].birthDate)
        assertNull(performers[0].creationDate)

        // Artista/Agrupación con fecha de creación (creationDate)
        assertEquals(2, performers[1].id)
        assertEquals("Grupo Niche", performers[1].name)
        assertEquals("1978-01-01T00:00:00.000Z", performers[1].creationDate)
        assertNull(performers[1].birthDate)
    }

    // PU-009: Álbum sin canciones ni comentarios (negativa / robustez de datos).
    @Test
    fun pu009_album_sin_canciones_ni_comentarios() {
        val jsonWithoutTracksAndComments = """
            {
              "id": 101,
              "name": "Álbum Vacío",
              "cover": "https://example.com/empty.jpg",
              "releaseDate": "2020-01-01T00:00:00.000Z",
              "description": "Sin canciones",
              "genre": "Rock",
              "recordLabel": "Sony",
              "tracks": [],
              "performers": [],
              "comments": []
            }
        """.trimIndent()

        val albumDetail = NetworkServiceAdapter.parseAlbumDetail(jsonWithoutTracksAndComments)

        assertNotNull(albumDetail)
        assertEquals(101, albumDetail.albumId)
        assertEquals("Álbum Vacío", albumDetail.name)
        assertTrue(albumDetail.tracks.isEmpty())
        assertTrue(albumDetail.comments.isEmpty())
        assertTrue(albumDetail.performers.isEmpty())
    }

    // PU-010: Error al obtener el detalle o JSON malformado lanza excepción (negativa).
    @Test(expected = JSONException::class)
    fun pu010_error_al_obtener_detalle_o_json_malformado_lanza_excepcion() {
        val invalidJson = "invalid_json_detail_string"

        NetworkServiceAdapter.parseAlbumDetail(invalidJson)
    }
}

package com.example.vinyls_application.network

import org.junit.Assert.assertEquals
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
          "performers": [],
          "comments": []
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
}

package com.example.vinyls_application.network

import junit.framework.TestCase.assertEquals
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
}
package com.example.vinyls_application.network

import com.example.vinyls_application.models.Band
import com.example.vinyls_application.models.Musician
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformerDetailParserTest {

    private val musicianJson = """
        {
          "id": 100,
          "name": "Rubén Blades Bellido de Luna",
          "image": "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Ruben_Blades_by_Gage_Skidmore.jpg/800px-Ruben_Blades_by_Gage_Skidmore.jpg",
          "description": "Es un cantante, compositor, músico, actor, abogado, político y activista panameño. Ha desarrollado gran parte de su carrera artística en la ciudad de Nueva York.",
          "birthDate": "1948-07-16T00:00:00.000Z",
          "albums": [
            {
              "id": 100,
              "name": "Buscando América",
              "cover": "https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg",
              "releaseDate": "1984-08-01T00:00:00.000Z",
              "description": "Buscando América es el primer álbum de la banda de Rubén Blades...",
              "genre": "Salsa",
              "recordLabel": "Elektra"
            }
          ],
          "performerPrizes": [
            {
              "id": 100,
              "premiationDate": "1978-12-10T00:00:00.000Z"
            }
          ]
        }
    """.trimIndent()

    private val bandJson = """
        {
          "id": 101,
          "name": "Queen",
          "image": "https://pm1.narvii.com/6724/a8b29909071e9d08517b40c748b6689649372852v2_hq.jpg",
          "description": "Queen es una banda británica de rock formada en 1970 en Londres...",
          "creationDate": "1970-01-01T00:00:00.000Z",
          "musicians": [],
          "albums": [
            {
              "id": 102,
              "name": "A Night at the Opera",
              "cover": "https://upload.wikimedia.org/wikipedia/en/4/4d/Queen_A_Night_At_The_Opera.png",
              "releaseDate": "1975-11-21T00:00:00.000Z",
              "description": "Es el cuarto álbum de estudio de la banda británica de rock Queen...",
              "genre": "Rock",
              "recordLabel": "EMI"
            }
          ],
          "performerPrizes": [
            {
              "id": 101,
              "premiationDate": "1980-12-10T00:00:00.000Z"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun pu018_returnsMusician_withDetailsAlbumsAndPrizes() {
        val result = NetworkServiceAdapter.parsePerformer(musicianJson)

        assertTrue(result is Musician)
        val musician = result as Musician

        assertEquals(100, musician.id)
        assertEquals("Rubén Blades Bellido de Luna", musician.name)
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Ruben_Blades_by_Gage_Skidmore.jpg/800px-Ruben_Blades_by_Gage_Skidmore.jpg",
            musician.image
        )
        assertEquals(
            "Es un cantante, compositor, músico, actor, abogado, político y activista panameño. Ha desarrollado gran parte de su carrera artística en la ciudad de Nueva York.",
            musician.description
        )
        assertEquals("1948-07-16T00:00:00.000Z", musician.birthDate)

        assertEquals(1, musician.albums.size)
        val album = musician.albums[0]
        assertEquals(100, album.albumId)
        assertEquals("Buscando América", album.name)
        assertEquals("https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg", album.cover)
        assertEquals("1984-08-01T00:00:00.000Z", album.releaseDate)
        assertEquals("Buscando América es el primer álbum de la banda de Rubén Blades...", album.description)
        assertEquals("Salsa", album.genre)
        assertEquals("Elektra", album.recordLabel)

        assertEquals(1, musician.performerPrizes.size)
        val prize = musician.performerPrizes[0]
        assertEquals(100, prize.id)
        assertEquals("1978-12-10T00:00:00.000Z", prize.premiationDate)
    }

    @Test
    fun pu019_returnsBand_withDetailsAlbumsAndPrizes() {
        val result = NetworkServiceAdapter.parsePerformer(bandJson)

        assertTrue(result is Band)
        val band = result as Band

        assertEquals(101, band.id)
        assertEquals("Queen", band.name)
        assertEquals(
            "https://pm1.narvii.com/6724/a8b29909071e9d08517b40c748b6689649372852v2_hq.jpg",
            band.image
        )
        assertEquals(
            "Queen es una banda británica de rock formada en 1970 en Londres...",
            band.description
        )
        assertEquals("1970-01-01T00:00:00.000Z", band.creationDate)

        assertEquals(1, band.albums.size)
        val album = band.albums[0]
        assertEquals(102, album.albumId)
        assertEquals("A Night at the Opera", album.name)
        assertEquals("https://upload.wikimedia.org/wikipedia/en/4/4d/Queen_A_Night_At_The_Opera.png", album.cover)
        assertEquals("1975-11-21T00:00:00.000Z", album.releaseDate)
        assertEquals("Es el cuarto álbum de estudio de la banda británica de rock Queen...", album.description)
        assertEquals("Rock", album.genre)
        assertEquals("EMI", album.recordLabel)

        assertEquals(1, band.performerPrizes.size)
        val prize = band.performerPrizes[0]
        assertEquals(101, prize.id)
        assertEquals("1980-12-10T00:00:00.000Z", prize.premiationDate)
    }

    @Test
    fun pu020_returnsPerformer_withParsedAlbums() {
        val performerWithAlbumsJson = """
            {
              "id": 100,
              "name": "Rubén Blades Bellido de Luna",
              "image": "https://example.com/ruben.jpg",
              "description": "Cantante panameño",
              "birthDate": "1948-07-16T00:00:00.000Z",
              "albums": [
                {
                  "id": 100,
                  "name": "Buscando América",
                  "cover": "https://example.com/cover1.jpg",
                  "releaseDate": "1984-08-01T00:00:00.000Z",
                  "description": "Primer álbum de Rubén Blades",
                  "genre": "Salsa",
                  "recordLabel": "Elektra"
                },
                {
                  "id": 101,
                  "name": "Siembra",
                  "cover": "https://example.com/cover2.jpg",
                  "releaseDate": "1978-09-07T00:00:00.000Z",
                  "description": "Segundo álbum de Rubén Blades",
                  "genre": "Salsa",
                  "recordLabel": "Fania"
                }
              ],
              "performerPrizes": []
            }
        """.trimIndent()

        val result = NetworkServiceAdapter.parsePerformer(performerWithAlbumsJson)

        assertTrue(result is Musician)
        val musician = result as Musician

        val albums = musician.albums
        assertEquals(2, albums.size)

        val album1 = albums[0]
        assertEquals(100, album1.albumId)
        assertEquals("Buscando América", album1.name)
        assertEquals("https://example.com/cover1.jpg", album1.cover)
        assertEquals("1984-08-01T00:00:00.000Z", album1.releaseDate)
        assertEquals("Primer álbum de Rubén Blades", album1.description)
        assertEquals("Salsa", album1.genre)
        assertEquals("Elektra", album1.recordLabel)

        val album2 = albums[1]
        assertEquals(101, album2.albumId)
        assertEquals("Siembra", album2.name)
        assertEquals("https://example.com/cover2.jpg", album2.cover)
        assertEquals("1978-09-07T00:00:00.000Z", album2.releaseDate)
        assertEquals("Segundo álbum de Rubén Blades", album2.description)
        assertEquals("Salsa", album2.genre)
        assertEquals("Fania", album2.recordLabel)
    }

    @Test
    fun pu21_returnsMusician_withEmptyLists() {
        val musicianEmptyListsJson = """
            {
              "id": 102,
              "name": "Músico Sin Álbumes",
              "image": "https://example.com/image.jpg",
              "description": "Sin álbumes ni premios",
              "birthDate": "1990-01-01T00:00:00.000Z",
              "albums": [],
              "performerPrizes": []
            }
        """.trimIndent()

        val result = NetworkServiceAdapter.parsePerformer(musicianEmptyListsJson)

        assertNotNull(result)
        assertTrue(result is Musician)
        val musician = result as Musician
        assertEquals(102, musician.id)
        assertTrue(musician.albums.isEmpty())
        assertTrue(musician.performerPrizes.isEmpty())
    }

    @Test(expected = JSONException::class)
    fun parsePerformer_invalidJson_throwsException() {
        NetworkServiceAdapter.parsePerformer("invalid_json")
    }
}

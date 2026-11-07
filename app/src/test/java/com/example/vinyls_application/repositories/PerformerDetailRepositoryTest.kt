package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.models.Band
import com.example.vinyls_application.models.Musician
import com.example.vinyls_application.models.PerformerBase
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class PerformerDetailRepositoryTest {

    private val musicianJson = """
        {
          "id": 100,
          "name": "Rubén Blades Bellido de Luna",
          "image": "https://example.com/ruben.jpg",
          "description": "Cantante, compositor y actor panameño.",
          "birthDate": "1948-07-16T00:00:00.000Z",
          "albums": [
            {
              "id": 100,
              "name": "Buscando América",
              "cover": "https://example.com/cover.jpg",
              "releaseDate": "1984-08-01T00:00:00.000Z",
              "description": "Álbum de prueba",
              "genre": "Salsa",
              "recordLabel": "Elektra"
            }
          ],
          "performerPrizes": [
            {
              "id": 1,
              "premiationDate": "1978-12-10T00:00:00.000Z"
            }
          ]
        }
    """.trimIndent()

    private val bandJson = """
        {
          "id": 101,
          "name": "Queen",
          "image": "https://example.com/queen.jpg",
          "description": "Banda británica de rock.",
          "creationDate": "1970-01-01T00:00:00.000Z",
          "musicians": [],
          "albums": [
            {
              "id": 102,
              "name": "A Night at the Opera",
              "cover": "https://example.com/night.jpg",
              "releaseDate": "1975-11-21T00:00:00.000Z",
              "description": "Cuarto álbum de la banda",
              "genre": "Rock",
              "recordLabel": "EMI"
            }
          ],
          "performerPrizes": [
            {
              "id": 2,
              "premiationDate": "1980-12-10T00:00:00.000Z"
            }
          ]
        }
    """.trimIndent()

    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: PerformerDetailRepository
    private var result: PerformerBase? = null
    private var error: VolleyError? = null

    @Before
    fun setUp() {
        val application = mock(Application::class.java)
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue)
        repository = PerformerDetailRepository(application)
        result = null
        error = null
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    @Test
    fun pi010_getMusician_returns200_withMusicianDetail() {
        repository.getPerformer(100, PerformerType.MUSICIAN, { result = it }, { error = it })
        val request = capturedRequest()

        deliverResponse(request, musicianJson)

        assertEquals(Request.Method.GET, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "musicians/100", request.url)
        assertNotNull(result)
        assertTrue(result is Musician)
        val musician = result as Musician
        assertEquals(100, musician.id)
        assertEquals("Rubén Blades Bellido de Luna", musician.name)
        assertEquals("1948-07-16T00:00:00.000Z", musician.birthDate)
        assertEquals(1, musician.albums.size)
        assertEquals("Buscando América", musician.albums[0].name)
        assertEquals(1, musician.performerPrizes.size)
        assertNull(error)
    }

    @Test
    fun pi011_getBand_returns200_withBandDetail() {
        repository.getPerformer(101, PerformerType.BAND, { result = it }, { error = it })
        val request = capturedRequest()

        deliverResponse(request, bandJson)

        assertEquals(Request.Method.GET, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "bands/101", request.url)
        assertNotNull(result)
        assertTrue(result is Band)
        val band = result as Band
        assertEquals(101, band.id)
        assertEquals("Queen", band.name)
        assertEquals("1970-01-01T00:00:00.000Z", band.creationDate)
        assertEquals(1, band.albums.size)
        assertEquals("A Night at the Opera", band.albums[0].name)
        assertEquals(1, band.performerPrizes.size)
        assertNull(error)
    }

    @Test
    fun pi012_getArtist_returns404_deliversError() {
        repository.getPerformer(999, PerformerType.MUSICIAN, { result = it }, { error = it })
        val request = capturedRequest()

        request.deliverError(ClientError(networkResponse(404)))

        assertTrue(error is ClientError)
        assertEquals(404, error?.networkResponse?.statusCode)
        assertNull(result)
    }

    @Test
    fun pi013_getArtist_returns500_deliversError() {
        repository.getPerformer(100, PerformerType.MUSICIAN, { result = it }, { error = it })
        val request = capturedRequest()

        request.deliverError(ServerError(networkResponse(500)))

        assertTrue(error is ServerError)
        assertEquals(500, error?.networkResponse?.statusCode)
        assertNull(result)
    }

    @Test
    fun pi013_noNetwork_deliversError() {
        repository.getPerformer(100, PerformerType.MUSICIAN, { result = it }, { error = it })
        val request = capturedRequest()

        request.deliverError(NoConnectionError())

        assertTrue(error is NoConnectionError)
        assertNull(result)
    }

    private fun capturedRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(captor.capture())
        return captor.value
    }

    private fun deliverResponse(request: StringRequest, response: String) {
        StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(request, response)
    }

    private fun networkResponse(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), false, 0, emptyList())
}

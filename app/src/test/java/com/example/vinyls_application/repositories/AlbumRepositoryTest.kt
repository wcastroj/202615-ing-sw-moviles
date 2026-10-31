package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.models.Album
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

class AlbumRepositoryTest {

    private val albumsJson = """
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

    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: AlbumRepository
    private var result: List<Album>? = null
    private var error: VolleyError? = null

    @Before
    fun setUp() {
        val application = mock(Application::class.java)
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue)
        repository = AlbumRepository(application)
        result = null
        error = null
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    @Test
    fun pi001_getAlbums_whenResponseIs200_deliversAlbumList() {
        repository.getAlbums({ result = it }, { error = it })
        val request = capturedRequest()

        deliverResponse(request, albumsJson)

        assertEquals(Request.Method.GET, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "albums", request.url)
        assertNotNull(result)
        assertEquals(2, result?.size)
        assertEquals(100, result?.get(0)?.albumId)
        assertEquals("Buscando América", result?.get(0)?.name)
        assertEquals("Salsa", result?.get(0)?.genre)
        assertEquals(101, result?.get(1)?.albumId)
        assertEquals("Poeta del Pueblo", result?.get(1)?.name)
        assertNull(error)
    }

    @Test
    fun pi002_getAlbums_whenResponseIs200WithEmptyList_deliversEmptyList() {
        repository.getAlbums({ result = it }, { error = it })
        val request = capturedRequest()

        deliverResponse(request, "[]")

        assertEquals(Request.Method.GET, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "albums", request.url)
        assertNotNull(result)
        assertTrue(result?.isEmpty() == true)
        assertNull(error)
    }

    @Test
    fun pi003_getAlbums_whenServerError500_deliversError() {
        repository.getAlbums({ result = it }, { error = it })
        val request = capturedRequest()

        request.deliverError(ServerError(network500Response()))

        assertTrue(error is ServerError)
        assertEquals(500, error?.networkResponse?.statusCode)
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

    private fun network500Response() =
        NetworkResponse(500, ByteArray(0), false, 0, emptyList())
}

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
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.network.NetworkServiceAdapter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class AlbumDetailRepositoryTest {

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

    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: AlbumDetailRepository
    private var result: AlbumDetail? = null
    private var error: VolleyError? = null

    @Before
    fun setUp() {
        val application = mock(Application::class.java)
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue)
        repository = AlbumDetailRepository(application)
        result = null
        error = null
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PI-004: GET /albums/{id} responde 200 y el repositorio entrega el detalle
    @Test
    fun pi004_get_album_responde_200_y_el_repositorio_entrega_el_detalle() {
        // Arrange
        repository.refreshData(100, { result = it }, { error = it })
        val request = capturedRequest()

        // Act
        deliverResponse(request, albumDetailJson)

        // Assert
        assertEquals(Request.Method.GET, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "albums/100", request.url)
        assertEquals(100, result?.albumId)
        assertEquals("Buscando América", result?.name)
        assertEquals("Salsa", result?.genre)
        assertEquals(3, result?.tracks?.size)
        assertNull(error)
    }

    // PI-005: GET /albums/{id} responde 404
    @Test
    fun pi005_get_album_responde_404_y_el_repositorio_entrega_error() {
        // Arrange
        repository.refreshData(100, { result = it }, { error = it })
        val request = capturedRequest()

        // Act
        request.deliverError(ClientError(networkResponse(404)))

        // Assert
        assertTrue(error is ClientError)
        assertEquals(404, error?.networkResponse?.statusCode)
        assertNull(result)
    }

    // PI-006: GET /albums/{id} responde 500 o no hay red (caso 500)
    @Test
    fun pi006_get_album_responde_500_y_el_repositorio_entrega_error() {
        // Arrange
        repository.refreshData(100, { result = it }, { error = it })
        val request = capturedRequest()

        // Act
        request.deliverError(ServerError(networkResponse(500)))

        // Assert
        assertTrue(error is ServerError)
        assertEquals(500, error?.networkResponse?.statusCode)
        assertNull(result)
    }

    // PI-006: GET /albums/{id} responde 500 o no hay red (caso sin red)
    @Test
    fun pi006_sin_red_el_repositorio_entrega_error() {
        // Arrange
        repository.refreshData(100, { result = it }, { error = it })
        val request = capturedRequest()

        // Act
        request.deliverError(NoConnectionError())

        // Assert
        assertTrue(error is NoConnectionError)
        assertNull(result)
    }

    private fun capturedRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(captor.capture())
        return captor.value
    }

    // deliverResponse es protected en StringRequest; Volley la llama al recibir la respuesta HTTP
    private fun deliverResponse(request: StringRequest, response: String) {
        StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(request, response)
    }

    private fun networkResponse(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), false, 0, emptyList())
}

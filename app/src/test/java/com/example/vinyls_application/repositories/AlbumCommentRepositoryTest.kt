package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.models.AlbumDetail
import com.example.vinyls_application.network.NetworkServiceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumCommentRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: AlbumDetailRepository
    private var error: VolleyError? = null
    private var isCompleted = false
    private var albumResult: AlbumDetail? = null

    @Before
    fun setUp() {
        val application = mock(Application::class.java)
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue, testDispatcher)
        repository = AlbumDetailRepository(application)
        error = null
        isCompleted = false
        albumResult = null
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PI-022: POST /albums/{id}/comments crea el comentario (Positiva)
    @Test
    fun pi022_postComment_createsCommentSuccessfully() = runTest(testDispatcher) {
        // Arrange
        launchPostComment(100, "Excelente álbum de salsa", 5)
        val request = capturedJsonObjectRequest()

        // Act
        deliverJsonResponse(request, JSONObject("""{"id": 10, "description": "Excelente álbum de salsa", "rating": "5"}"""))

        // Assert
        assertEquals(Request.Method.POST, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "albums/100/comments", request.url)
        assertTrue(isCompleted)
        assertNull(error)
    }

    // PI-023: El comentario aparece luego en GET /albums/{id} (Positiva)
    @Test
    fun pi023_commentAppearsInAlbumDetailGet() = runTest(testDispatcher) {
        val albumWithCommentJson = """
            {
              "id": 100,
              "name": "Buscando América",
              "cover": "https://example.com/cover.jpg",
              "releaseDate": "1984-08-01T00:00:00.000Z",
              "description": "Álbum de prueba",
              "genre": "Salsa",
              "recordLabel": "Elektra",
              "tracks": [],
              "performers": [],
              "comments": [
                { "id": 10, "description": "Excelente álbum de salsa", "rating": "5" }
              ]
            }
        """.trimIndent()

        // Arrange
        launchLoadAlbumDetail(100)
        val request = capturedStringRequest()

        // Act
        deliverStringResponse(request, albumWithCommentJson)

        // Assert
        assertEquals(Request.Method.GET, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "albums/100", request.url)
        assertNotNull(albumResult)
        assertEquals(1, albumResult?.comments?.size)
        assertEquals("Excelente álbum de salsa", albumResult?.comments?.get(0)?.description)
        assertEquals("5", albumResult?.comments?.get(0)?.rating)
        assertNull(error)
    }

    // PI-024: POST /albums/{id}/comments responde 404 porque el álbum no existe (Negativa)
    @Test
    fun pi024_postComment_albumNotFound_deliversError() = runTest(testDispatcher) {
        // Arrange
        launchPostComment(999, "Comentario fantasma", 4)
        val request = capturedJsonObjectRequest()

        // Act
        request.deliverError(ClientError(networkResponse(404)))

        // Assert
        assertTrue(error is ClientError)
        assertEquals(404, error?.networkResponse?.statusCode)
        assertFalse(isCompleted)
    }

    // PI-025: POST /albums/{id}/comments responde 500 o no hay red (Negativa)
    @Test
    fun pi025_postComment_serverError_deliversError() = runTest(testDispatcher) {
        // Arrange
        launchPostComment(100, "Falla del servidor", 3)
        val request = capturedJsonObjectRequest()

        // Act
        request.deliverError(ServerError(networkResponse(500)))

        // Assert
        assertTrue(error is ServerError)
        assertEquals(500, error?.networkResponse?.statusCode)
        assertFalse(isCompleted)
    }

    private fun CoroutineScope.launchPostComment(albumId: Int, comment: String, rating: Int) = launch {
        try {
            repository.postComment(albumId, comment, rating)
            isCompleted = true
        } catch (e: VolleyError) {
            error = e
        }
    }

    private fun CoroutineScope.launchLoadAlbumDetail(albumId: Int) = launch {
        try {
            albumResult = repository.refreshData(albumId)
        } catch (e: VolleyError) {
            error = e
        }
    }

    private fun capturedJsonObjectRequest(): JsonObjectRequest {
        val captor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(captor.capture())
        return captor.value
    }

    private fun capturedStringRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(captor.capture())
        return captor.value
    }

    private fun deliverStringRequest(request: StringRequest, response: String) {
        StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(request, response)
    }

    private fun deliverStringResponse(request: StringRequest, response: String) {
        StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(request, response)
    }

    private fun deliverJsonResponse(request: JsonObjectRequest, response: JSONObject) {
        var clazz: Class<*>? = request.javaClass
        while (clazz != null) {
            val method = clazz.declaredMethods.firstOrNull { it.name == "deliverResponse" }
            if (method != null) {
                method.isAccessible = true
                method.invoke(request, response)
                return
            }
            clazz = clazz.superclass
        }
    }

    private fun networkResponse(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), false, 0, emptyList())
}
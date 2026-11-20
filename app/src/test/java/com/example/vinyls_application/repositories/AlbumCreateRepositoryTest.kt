package com.example.vinyls_application.repositories

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.android.volley.ClientError
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.NoCache
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.network.NetworkServiceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Pruebas de integración de HU05: repositorio + adapter + Volley real contra MockWebServer.
 * Robolectric aporta el Context y el hilo principal donde Volley entrega las respuestas.
 */
@RunWith(RobolectricTestRunner::class)
class AlbumCreateRepositoryTest {

    private val album = Album(
        name = "Ecos del Atardecer",
        cover = "https://example.com/portada.jpg",
        releaseDate = "2024-05-10T00:00:00.000Z",
        description = "Un álbum de prueba",
        genre = "Rock",
        recordLabel = "EMI"
    )

    private val albums = CopyOnWriteArrayList(listOf(albumJson(100, "Buscando América", "Salsa", "Elektra")))

    private lateinit var application: Application
    private lateinit var server: MockWebServer
    private lateinit var queue: RequestQueue
    private lateinit var repository: AlbumRepository
    private lateinit var originalBaseUrl: String
    private var isServerRunning = false

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.start()
        isServerRunning = true
        originalBaseUrl = NetworkServiceAdapter.BASE_URL
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()

        // Misma pila HTTP que usa Volley en Android, sin caché para que cada GET llegue al servidor
        queue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply { start() }
        NetworkServiceAdapter.instance =
            NetworkServiceAdapter(application, queue, Dispatchers.Unconfined)
        repository = AlbumRepository(application)
    }

    @After
    fun tearDown() {
        queue.stop()
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        if (isServerRunning) server.shutdown()
    }

    // PI-014: POST /albums crea el álbum (positiva).
    @Test
    fun pi014_post_albums_crea_el_album() {
        givenBackend()

        val result = await { repository.createAlbum(album) }

        assertTrue(result.isSuccess)
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/albums", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals(album.name, body.getString("name"))
        assertEquals(album.cover, body.getString("cover"))
        assertEquals(album.releaseDate, body.getString("releaseDate"))
        assertEquals(album.description, body.getString("description"))
        assertEquals(album.genre, body.getString("genre"))
        assertEquals(album.recordLabel, body.getString("recordLabel"))
        // El id lo asigna el backend: el álbum nuevo no lo envía
        assertFalse(body.has("id"))
    }

    // PI-015: El álbum creado aparece luego en GET /albums (positiva).
    @Test
    fun pi015_album_creado_aparece_en_get_albums() {
        givenBackend()

        await { repository.createAlbum(album) }.getOrThrow()
        val result: List<Album> = await { repository.getAlbums() }.getOrThrow()

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == album.name && it.genre == album.genre })
    }

    // PI-016: POST /albums responde 400 por datos rechazados (negativa).
    @Test
    fun pi016_post_albums_responde_400() {
        givenBackend(postResponse = errorResponse(400))

        val error = await { repository.createAlbum(album) }.exceptionOrNull()

        assertEquals(ClientError::class.java, error?.javaClass)
        assertEquals(400, (error as VolleyError).networkResponse.statusCode)
        assertEquals(1, albums.size)
    }

    // PI-017: POST /albums responde 500 o no hay red (negativa).
    @Test
    fun pi017_post_albums_responde_500_o_sin_red() {
        // Caso 500: el servidor falla y el álbum no se crea
        givenBackend(postResponse = errorResponse(500))

        val serverError = await { repository.createAlbum(album) }.exceptionOrNull()

        assertEquals(ServerError::class.java, serverError?.javaClass)
        assertEquals(500, (serverError as VolleyError).networkResponse.statusCode)
        assertEquals(1, albums.size)

        // Caso sin red: el servidor deja de responder
        server.shutdown()
        isServerRunning = false

        val networkError = await { repository.createAlbum(album) }.exceptionOrNull()

        assertEquals(NoConnectionError::class.java, networkError?.javaClass)
    }

    /** Un POST exitoso agrega el álbum a la lista, como haría el backend. */
    private fun givenBackend(postResponse: MockResponse? = null) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path != "/albums" -> MockResponse().setResponseCode(404)
                request.method == "POST" -> postResponse ?: createAlbum(request)
                else -> jsonResponse(albums.joinToString(",", "[", "]"))
            }
        }
    }

    private fun createAlbum(request: RecordedRequest): MockResponse {
        // Se lee una copia para que la prueba también pueda consultar el cuerpo de la petición
        val body = JSONObject(request.body.clone().readUtf8())
        val created = albumJson(200, body.getString("name"), body.getString("genre"), body.getString("recordLabel"))
        albums.add(created)
        return jsonResponse(created)
    }

    /**
     * Ejecuta [block] y procesa el hilo principal hasta que termine: ahí entrega Volley la
     * respuesta. La espera tiene límite para que la prueba falle en lugar de quedarse colgada.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> await(block: suspend () -> T): Result<T> {
        val result = CoroutineScope(Dispatchers.Unconfined).async { runCatching { block() } }
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (!result.isCompleted) {
            check(System.currentTimeMillis() < deadline) { "La petición no terminó a tiempo" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return result.getCompleted()
    }

    /** El backend acompaña sus errores con un JSON que describe el problema. */
    private fun errorResponse(statusCode: Int) = jsonResponse("""{"statusCode":$statusCode}""")
        .setResponseCode(statusCode)

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private fun albumJson(id: Int, name: String, genre: String, recordLabel: String) =
        """{"id":$id,"name":"$name","cover":"https://example.com/$id.jpg","releaseDate":"1984-08-01T00:00:00.000Z","description":"Album","genre":"$genre","recordLabel":"$recordLabel"}"""

    private companion object {
        const val TIMEOUT_MS = 15_000L
        const val POLL_INTERVAL_MS = 10L
    }
}

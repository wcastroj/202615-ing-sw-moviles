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
import com.example.vinyls_application.models.Track
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
 * Pruebas de integración de HU06: repositorio + adapter + Volley real contra MockWebServer.
 * Robolectric aporta el Context y el hilo principal donde Volley entrega las respuestas.
 */
@RunWith(RobolectricTestRunner::class)
class TrackRepositoryTest {

    private val track = Track(name = "Don't Stop Me Now", duration = "3:45")

    /** Canciones que el backend falso tiene guardadas para el álbum. */
    private val tracks = CopyOnWriteArrayList(listOf(trackJson(1, "Decisiones", "5:05")))

    private lateinit var application: Application
    private lateinit var server: MockWebServer
    private lateinit var queue: RequestQueue
    private lateinit var repository: TrackRepository
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

        // Misma pila HTTP que usa Volley en Android, sin caché para que cada petición llegue al servidor
        queue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply { start() }
        NetworkServiceAdapter.instance =
            NetworkServiceAdapter(application, queue, Dispatchers.Unconfined)
        repository = TrackRepository(application)
    }

    @After
    fun tearDown() {
        queue.stop()
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        if (isServerRunning) server.shutdown()
    }

    // PI-018: POST /albums/{id}/tracks crea el track (positiva).
    @Test
    fun pi018_post_albums_id_tracks_crea_el_track() {
        givenBackend()

        val result = await { repository.associateTrack(ALBUM_ID, track) }

        assertTrue(result.isSuccess)
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("POST", request.method)
        assertEquals("/albums/$ALBUM_ID/tracks", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals(track.name, body.getString("name"))
        assertEquals(track.duration, body.getString("duration"))
        // El id lo asigna el backend: la canción nueva no lo envía
        assertFalse(body.has("id"))
    }

    // PI-019: El track creado aparece luego en GET /albums/{id} (positiva).
    @Test
    fun pi019_track_creado_aparece_en_get_albums_id() {
        givenBackend()

        await { repository.associateTrack(ALBUM_ID, track) }.getOrThrow()
        val album = await { AlbumDetailRepository(application).refreshData(ALBUM_ID) }.getOrThrow()

        assertEquals(2, album.tracks.size)
        assertTrue(album.tracks.any { it.name == track.name && it.duration == track.duration })
    }

    // PI-020: POST /albums/{id}/tracks responde 404 porque el álbum no existe (negativa).
    @Test
    fun pi020_post_albums_id_tracks_responde_404() {
        givenBackend()

        val error = await { repository.associateTrack(NON_EXISTENT_ALBUM_ID, track) }.exceptionOrNull()

        assertEquals(ClientError::class.java, error?.javaClass)
        assertEquals(404, (error as VolleyError).networkResponse.statusCode)
        assertEquals("/albums/$NON_EXISTENT_ALBUM_ID/tracks", server.takeRequest(1, TimeUnit.SECONDS)!!.path)
        assertEquals(1, tracks.size)
    }

    // PI-021: POST /albums/{id}/tracks responde 500 o no hay red (negativa).
    @Test
    fun pi021_post_albums_id_tracks_responde_500_o_sin_red() {
        // Caso 500: el servidor falla y la canción no se guarda
        givenBackend(postResponse = errorResponse(500))

        val serverError = await { repository.associateTrack(ALBUM_ID, track) }.exceptionOrNull()

        assertEquals(ServerError::class.java, serverError?.javaClass)
        assertEquals(500, (serverError as VolleyError).networkResponse.statusCode)
        assertEquals(1, tracks.size)

        // Caso sin red: el servidor deja de responder
        server.shutdown()
        isServerRunning = false

        val networkError = await { repository.associateTrack(ALBUM_ID, track) }.exceptionOrNull()

        assertEquals(NoConnectionError::class.java, networkError?.javaClass)
    }

    /**
     * Backend mínimo: un POST guarda la canción que envió la app y el GET del álbum devuelve
     * las canciones guardadas. Cualquier otra ruta da 404.
     */
    private fun givenBackend(postResponse: MockResponse? = null) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.method == "POST" && request.path == "/albums/$ALBUM_ID/tracks" ->
                    postResponse ?: createTrack(request)
                request.method == "GET" && request.path == "/albums/$ALBUM_ID" ->
                    jsonResponse(albumJson())
                else -> errorResponse(404)
            }
        }
    }

    private fun createTrack(request: RecordedRequest): MockResponse {
        // Se lee una copia para que la prueba también pueda consultar el cuerpo de la petición
        val body = JSONObject(request.body.clone().readUtf8())
        val created = trackJson(tracks.size + 1, body.getString("name"), body.getString("duration"))
        tracks.add(created)
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

    private fun trackJson(id: Int, name: String, duration: String) = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("duration", duration)
        .toString()

    /** Mismos campos que AlbumDetailParserTest, con las canciones que tenga guardadas el backend. */
    private fun albumJson() =
        """{"id":$ALBUM_ID,"name":"Buscando América","cover":"https://example.com/cover.jpg","releaseDate":"1984-08-01T00:00:00.000Z","description":"Álbum de prueba","genre":"Salsa","recordLabel":"Elektra","tracks":${tracks.joinToString(",", "[", "]")},"performers":[],"comments":[]}"""

    private companion object {
        const val ALBUM_ID = 100
        const val NON_EXISTENT_ALBUM_ID = 999
        const val TIMEOUT_MS = 15_000L
        const val POLL_INTERVAL_MS = 10L
    }
}

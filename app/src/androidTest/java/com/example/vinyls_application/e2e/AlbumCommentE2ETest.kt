package com.example.vinyls_application.e2e

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.R
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.MainActivity
import com.example.vinyls_application.ui.widgets.CommentCardWidget
import com.example.vinyls_application.utils.VolleyIdlingResource
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Pruebas E2E de HU07 - Comentar un álbum como coleccionista.
 *
 * PE-032/033 usa el backend real (NetworkServiceAdapter.BASE_URL): publica un comentario con
 * texto único en el primer álbum y verifica que la app lo muestre y que el backend lo guardó.
 * Cada corrida deja un comentario nuevo en ese álbum.
 *
 * PE-034 y PE-035 (validaciones del formulario) y PE-036 (error al guardar) usan MockWebServer.
 *
 * Espresso espera las peticiones de Volley con [VolleyIdlingResource], igual que en las demás E2E.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AlbumCommentE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

    /** Id del álbum que abrió [openFirstAlbumAsCollector]. */
    private var firstAlbumId = 0

    // Datos falsos para pe034, pe035 y pe036: un álbum en la lista y su detalle sin comentarios
    private val albumsListJson = """
        [{"id":100,"name":"Buscando América","cover":"http://localhost/cover.jpg","releaseDate":"1984-08-01T00:00:00.000Z","description":"Álbum.","genre":"Salsa","recordLabel":"Elektra"}]
    """.trimIndent()

    private val albumDetailJson = """
        {
          "id":100,
          "name":"Buscando América",
          "cover":"http://localhost/cover.jpg",
          "releaseDate":"1984-08-01T00:00:00.000Z",
          "description":"Álbum de salsa.",
          "genre":"Salsa",
          "recordLabel":"Elektra",
          "tracks":[],
          "performers":[],
          "comments":[]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        // 1. Servidor falso, solo lo usan pe034, pe035 y pe036 (cada uno define sus respuestas)
        server = MockWebServer()
        server.start()

        // 2. Guardar la URL del backend real; pe032/033 la usa tal cual
        originalBaseUrl = NetworkServiceAdapter.BASE_URL

        // 3. Cola de Volley vigilada por el IdlingResource
        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        // Dispatchers.Main: el parseo pasa por la cola del hilo principal, que Espresso sí vigila.
        // Con Dispatchers.Default el IdlingResource quedaría libre antes de que llegue el dato a la UI.
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue, Dispatchers.Main)
        IdlingRegistry.getInstance().register(idlingResource)

        // La app se abre dentro de cada test con launchApp()
    }

    @After
    fun tearDown() {
        scenario?.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        // Se detiene la cola para no dejar sus hilos vivos entre tests
        queue.stop()
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    @Test
    fun pe032_and_pe033_collectorCanCommentAlbumSuccessfully() {
        // Texto único para reconocer el comentario de esta corrida entre los que ya tiene el álbum
        val commentText = "Comentario E2E ${System.currentTimeMillis()}"

        // Paso 1: abrir la app contra el backend real y entrar al primer álbum como coleccionista
        launchApp()
        openFirstAlbumAsCollector()

        // Paso 2 (PE-032): el formulario para comentar está visible
        onView(withId(R.id.addCommentWidget)).perform(scrollTo()).check(matches(isDisplayed()))

        // Paso 3 (PE-033): enviar un comentario y una calificación válidos
        submitComment(commentText, R.id.rbRating5)

        // Paso 4: tras guardar, la app recarga el detalle y muestra el comentario con su calificación
        onView(
            allOf(
                isAssignableFrom(CommentCardWidget::class.java),
                isDescendantOfA(withId(R.id.comments_list)),
                hasDescendant(withText(commentText))
            )
        ).perform(scrollTo()).check(matches(hasDescendant(withText("5/5"))))

        // Paso 5: el backend real guardó el comentario en ese álbum
        val comments = fetchFromBackend("albums/$firstAlbumId").getJSONArray("comments")
        val saved = (0 until comments.length()).map { comments.getJSONObject(it) }
            .firstOrNull { it.getString("description") == commentText }
        assertNotNull("El backend no guardó \"$commentText\" en el álbum $firstAlbumId", saved)
        assertEquals("5", saved!!.get("rating").toString())
    }

    @Test
    fun pe034_emptyOrShortCommentBlocksSubmission() {
        // Paso 1: abrir la app apuntando al servidor falso y entrar al álbum como coleccionista
        givenMockBackend()
        launchApp(useMockServer = true)
        openFirstAlbumAsCollector()

        // Paso 2: enviar un comentario de 1 carácter con calificación
        submitComment("A", R.id.rbRating5)

        // Paso 3: el formulario sigue visible con el error del texto y no se envió nada
        assertFormError(R.string.add_comment_text_error)
        assertNoCommentPosted()
    }

    @Test
    fun pe035_missingRatingBlocksSubmission() {
        // Paso 1: abrir la app apuntando al servidor falso y entrar al álbum como coleccionista
        givenMockBackend()
        launchApp(useMockServer = true)
        openFirstAlbumAsCollector()

        // Paso 2: enviar un comentario válido sin elegir calificación
        submitComment("Excelente album clasico", ratingId = null)

        // Paso 3: el formulario sigue visible con el error de la calificación y no se envió nada
        assertFormError(R.string.add_comment_rating_error)
        assertNoCommentPosted()
    }

    @Test
    fun pe036_commentErrorShowsErrorScreen() {
        // Paso 1: abrir la app apuntando al servidor falso (el guardado responde 500)
        givenMockBackend(postResponse = MockResponse().setResponseCode(500))
        launchApp(useMockServer = true)
        openFirstAlbumAsCollector()

        // Paso 2: enviar un comentario válido
        submitComment("Comentario fallido", R.id.rbRating4)

        // Paso 3: la app muestra la pantalla de error
        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))

        // Paso 4: la app sí envió el comentario con el texto, la calificación y el coleccionista
        val post = recordedRequests().single { it.method == "POST" }
        assertEquals("/albums/100/comments", post.path)
        val body = JSONObject(post.body.readUtf8())
        assertEquals("Comentario fallido", body.getString("description"))
        assertEquals(4, body.getInt("rating"))
        assertEquals(100, body.getJSONObject("collector").getInt("id"))
    }

    /**
     * Abre la app. Con [useMockServer] la apunta antes al servidor falso; si no, usa el backend real.
     */
    private fun launchApp(useMockServer: Boolean = false) {
        if (useMockServer) {
            NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Entra como coleccionista, espera la lista de álbumes, guarda el id del primero y lo abre. */
    private fun openFirstAlbumAsCollector() {
        onView(withId(R.id.cardCollector)).check(matches(isDisplayed()))
        onView(withId(R.id.cardCollector)).perform(click())
        onView(withId(R.id.albumsRv)).check(matches(hasMinimumChildCount(1)))
        firstAlbumId = readFirstAlbumId()
        onView(withId(R.id.albumsRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    }

    /** Lee el id del primer álbum desde el tag de su tarjeta, como en AlbumDetailE2ETest. */
    private fun readFirstAlbumId(): Int {
        var albumId = 0
        scenario!!.onActivity { activity ->
            val recyclerView = activity.findViewById<RecyclerView>(R.id.albumsRv)
            albumId = recyclerView.findViewHolderForAdapterPosition(0)!!
                .itemView.getTag(R.id.tag_album_id) as Int
        }
        return albumId
    }

    /** Escribe el comentario, elige la calificación (si [ratingId] no es null) y toca enviar. */
    private fun submitComment(text: String, ratingId: Int?) {
        onView(withId(R.id.etComment)).perform(scrollTo(), replaceText(text), closeSoftKeyboard())
        if (ratingId != null) {
            onView(withId(ratingId)).perform(scrollTo(), click())
        }
        onView(withId(R.id.btnSubmitComment)).perform(scrollTo(), click())
    }

    /** El formulario sigue en pantalla y muestra el mensaje de validación indicado. */
    private fun assertFormError(messageRes: Int) {
        onView(withId(R.id.addCommentWidget)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.tvError), isDescendantOfA(withId(R.id.addCommentWidget))))
            .check(matches(allOf(isDisplayed(), withText(messageRes))))
    }

    /** Ninguna de las peticiones que recibió el servidor falso fue un POST. */
    private fun assertNoCommentPosted() {
        val requests = recordedRequests().map { "${it.method} ${it.path}" }
        assertTrue("La app envió un comentario inválido: $requests", requests.none { it.startsWith("POST") })
    }

    /** Todas las peticiones que recibió el servidor falso, en orden. */
    private fun recordedRequests(): List<RecordedRequest> =
        List(server.requestCount) { server.takeRequest(1, TimeUnit.SECONDS)!! }

    /**
     * Respuestas del servidor falso: la lista con un álbum y su detalle sin comentarios.
     * [postResponse] es la respuesta al guardar el comentario (por defecto 404: no se espera POST).
     */
    private fun givenMockBackend(postResponse: MockResponse = MockResponse().setResponseCode(404)) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/albums" -> jsonResponse(albumsListJson)
                request.path == "/albums/100" && request.method == "GET" -> jsonResponse(albumDetailJson)
                request.path == "/albums/100/comments" && request.method == "POST" -> postResponse
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    /**
     * Respuesta 200 con JSON. Sin `charset=utf-8` Volley decodifica en ISO-8859-1 y los
     * acentos llegan mal ("América" se vería "AmÃ©rica").
     */
    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    /**
     * Consulta el backend real desde el hilo del test. Timeout de 10 s: si el backend no
     * responde, el test falla rápido con un error claro.
     */
    private fun fetchFromBackend(path: String): JSONObject {
        val connection = URL(NetworkServiceAdapter.BASE_URL + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return try {
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}

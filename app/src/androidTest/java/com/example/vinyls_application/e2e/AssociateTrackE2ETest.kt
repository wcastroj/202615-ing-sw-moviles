package com.example.vinyls_application.e2e

import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.R
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.MainActivity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Pruebas E2E de HU06 - Asociar una canción (track) a un álbum.
 *
 * Las pruebas positivas usan el backend real (NetworkServiceAdapter.BASE_URL). Las negativas que
 * necesiten errores o datos controlados usarán MockWebServer con launchApp(useMockServer = true).
 *
 * Espresso espera las peticiones de Volley con [VolleyIdlingResource], igual que en las demás E2E,
 * así que las verificaciones no se reintentan.
 *
 * Compatibilidad: ejecutar en Android 13 (API 33) y Android 15 (API 35) con
 * `gradlew compatibilidadGroupDebugAndroidTest` o en dos emuladores con `connectedDebugAndroidTest`.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AssociateTrackE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

    // Datos falsos para las pruebas negativas: un álbum en la lista y su detalle con una canción
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
          "tracks":[{"id":1,"name":"Decisiones","duration":"5:05"}],
          "performers":[],
          "comments":[]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        // 1. Servidor falso para las pruebas negativas (cada una definirá sus respuestas)
        server = MockWebServer()
        server.start()

        // 2. Guardar la URL del backend real; las pruebas positivas la usan tal cual
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

    /** PE-025 (positiva): el botón del detalle del álbum abre el formulario para asociar una canción. */
    @Test
    fun pe025_albumDetailButtonOpensTrackForm() {
        // Paso 1: abrir la app contra el backend real y entrar al primer álbum como coleccionista
        launchApp()
        openFirstAlbumDetailAsCollector()

        // Paso 2: tocar "Asociar canción" en el detalle
        openTrackForm()

        // Paso 3: se abre el formulario con su título, los dos campos vacíos y el botón habilitado
        onView(withId(R.id.txtHeader)).check(matches(withText(R.string.associate_track_title)))
        onView(withId(R.id.btnReturn)).check(matches(isDisplayed()))
        onView(withId(R.id.etTrackName)).check(matches(allOf(isDisplayed(), withText(""))))
        onView(withId(R.id.etTrackDuration)).check(matches(allOf(isDisplayed(), withText(""))))
        onView(withId(R.id.btnAssociateTrack)).check(matches(allOf(isDisplayed(), isEnabled())))
    }

    /**
     * PE-026 (positiva): con una canción válida se muestra el éxito y, al volver, la lista de
     * canciones del álbum incluye la nueva. Cada corrida deja una canción en el primer álbum.
     */
    @Test
    fun pe026_validTrackShowsSuccessAndUpdatesTrackList() {
        // Nombre único para reconocer la canción de esta corrida entre las que ya tiene el álbum
        val trackName = "Track E2E ${System.currentTimeMillis()}"
        val trackDuration = "4:35"

        // Paso 1: abrir la app contra el backend real, entrar al primer álbum y abrir el formulario
        launchApp()
        openFirstAlbumDetailAsCollector()
        openTrackForm()

        // Paso 2: escribir una canción válida y asociarla
        fillTrackForm(trackName, trackDuration)
        onView(withId(R.id.btnAssociateTrack)).perform(click())

        // Paso 3: se muestra la pantalla de éxito
        onView(withId(R.id.successInformationWidget)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.successInformationWidget))))
            .check(matches(withText(R.string.associate_track_success_title)))

        // Paso 4: volver al detalle, que se recarga y muestra la canción nueva con su duración
        onView(allOf(withId(R.id.buttonPrimary), isDescendantOfA(withId(R.id.successInformationWidget))))
            .perform(click())
        onView(allOf(withParent(withId(R.id.tracksListWidget)), hasDescendant(withText(trackName))))
            .perform(scrollTo())
            .check(matches(hasDescendant(withText(trackDuration))))
    }

    /**
     * PE-027 (positiva): "Devolverse" (la flecha de regreso del encabezado) regresa al detalle del
     * álbum sin guardar.
     */
    @Test
    fun pe027_returnArrowGoesBackToAlbumDetailWithoutSaving() {
        // Nombre único: si la canción se guardara por error, aparecería en la lista del detalle
        val trackName = "Track E2E ${System.currentTimeMillis()}"

        // Paso 1: abrir la app contra el backend real, entrar al primer álbum y abrir el formulario
        launchApp()
        openFirstAlbumDetailAsCollector()
        openTrackForm()

        // Paso 2: escribir una canción válida y salir con la flecha de regreso, sin asociarla
        fillTrackForm(trackName, "4:35")
        onView(withId(R.id.btnReturn)).perform(click())

        // Paso 3: se vuelve al detalle del álbum y la canción no está en la lista
        onView(withId(R.id.detail_card)).check(matches(isDisplayed()))
        onView(withText(trackName)).check(doesNotExist())
    }

    /** PE-028 (negativa): con los campos vacíos se marcan como obligatorios y no se envía nada. */
    @Test
    fun pe028_emptyFieldsShowRequiredErrorsAndBlockSubmission() {
        // Paso 1: abrir la app apuntando al servidor falso, entrar al álbum y abrir el formulario
        givenMockBackend()
        launchApp(useMockServer = true)
        openFirstAlbumDetailAsCollector()
        openTrackForm()

        // Paso 2: tocar "Asociar Canción" sin escribir nada
        onView(withId(R.id.btnAssociateTrack)).perform(click())

        // Paso 3: los dos campos muestran "Este campo es obligatorio" y el formulario sigue en pantalla
        assertFieldError(R.id.tilTrackName, R.string.album_create_error_required)
        assertFieldError(R.id.tilTrackDuration, R.string.album_create_error_required)
        onView(withId(R.id.btnAssociateTrack)).check(matches(allOf(isDisplayed(), isEnabled())))

        // Paso 4: la app no envió la canción al servidor
        assertNoTrackPosted()
    }

    /**
     * PE-029 (negativa): una duración que no cumple mm:ss se marca con su mensaje y no se envía
     * nada. Se prueban dos casos: sin los dos puntos y con segundos mayores a 59.
     */
    @Test
    fun pe029_invalidDurationFormatShowsErrorAndBlocksSubmission() {
        // Paso 1: abrir la app apuntando al servidor falso, entrar al álbum y abrir el formulario
        givenMockBackend()
        launchApp(useMockServer = true)
        openFirstAlbumDetailAsCollector()
        openTrackForm()

        // Paso 2: con un nombre válido, cada duración inválida muestra el error de formato
        listOf("435", "4:75").forEach { duration ->
            fillTrackForm("Pedro Navaja", duration)
            assertFieldError(R.id.tilTrackDuration, R.string.associate_track_error_duration_format)
        }

        // Paso 3: al intentar asociar, el error sigue, el nombre no tiene error y el formulario sigue en pantalla
        onView(withId(R.id.btnAssociateTrack)).perform(click())
        assertFieldError(R.id.tilTrackDuration, R.string.associate_track_error_duration_format)
        onView(allOf(withText(R.string.album_create_error_required), isDescendantOfA(withId(R.id.tilTrackName))))
            .check(doesNotExist())
        onView(withId(R.id.btnAssociateTrack)).check(matches(allOf(isDisplayed(), isEnabled())))

        // Paso 4: la app no envió la canción al servidor
        assertNoTrackPosted()
    }

    /**
     * PE-030 (negativa): si falla el guardado se muestra la pantalla de error y, al volver, el
     * formulario conserva lo escrito para intentarlo de nuevo.
     */
    @Test
    fun pe030_saveFailureShowsErrorMessage() {
        // Paso 1: abrir la app apuntando al servidor falso (asociar responde 500) y abrir el formulario
        givenMockBackend(postResponse = MockResponse().setResponseCode(500))
        launchApp(useMockServer = true)
        openFirstAlbumDetailAsCollector()
        openTrackForm()

        // Paso 2: escribir una canción válida y asociarla
        fillTrackForm("Pedro Navaja", "7:21")
        onView(withId(R.id.btnAssociateTrack)).perform(click())

        // Paso 3: se muestra la pantalla de error con su título y el mensaje de que no se pudo asociar
        onView(withId(R.id.errorInformationWidget)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.errorInformationWidget))))
            .check(matches(withText(R.string.album_create_error_title)))
        onView(allOf(withId(R.id.description), isDescendantOfA(withId(R.id.errorInformationWidget))))
            .check(matches(withText(R.string.associate_track_save_error_description)))

        // Paso 4: la app sí envió la canción con el nombre y la duración escritos
        val post = List(server.requestCount) { server.takeRequest(1, TimeUnit.SECONDS)!! }
            .single { it.method == "POST" }
        assertEquals("/albums/100/tracks", post.path)
        val body = JSONObject(post.body.readUtf8())
        assertEquals("Pedro Navaja", body.getString("name"))
        assertEquals("7:21", body.getString("duration"))

        // Paso 5: "Volver al formulario" regresa al formulario con los datos que tenía
        onView(allOf(withId(R.id.buttonPrimary), isDescendantOfA(withId(R.id.errorInformationWidget))))
            .perform(click())
        onView(withId(R.id.etTrackName)).check(matches(withText("Pedro Navaja")))
        onView(withId(R.id.etTrackDuration)).check(matches(withText("7:21")))
        onView(withId(R.id.btnAssociateTrack)).check(matches(allOf(isDisplayed(), isEnabled())))
    }

    /**
     * PE-031 (negativa): una duración de cero (00:00 o 0:00) cumple el formato mm:ss, pero se marca
     * con su propio mensaje y no se envía nada.
     */
    @Test
    fun pe031_zeroDurationShowsErrorAndBlocksSubmission() {
        // Paso 1: abrir la app apuntando al servidor falso, entrar al álbum y abrir el formulario
        givenMockBackend()
        launchApp(useMockServer = true)
        openFirstAlbumDetailAsCollector()
        openTrackForm()

        // Paso 2: con un nombre válido, cada duración de cero muestra "La duración debe ser mayor a 00:00"
        listOf("00:00", "0:00").forEach { duration ->
            fillTrackForm("Pedro Navaja", duration)
            assertFieldError(R.id.tilTrackDuration, R.string.associate_track_error_duration_zero)
        }

        // Paso 3: al intentar asociar, el error sigue, el nombre no tiene error y el formulario sigue en pantalla
        onView(withId(R.id.btnAssociateTrack)).perform(click())
        assertFieldError(R.id.tilTrackDuration, R.string.associate_track_error_duration_zero)
        onView(allOf(withText(R.string.album_create_error_required), isDescendantOfA(withId(R.id.tilTrackName))))
            .check(doesNotExist())
        onView(withId(R.id.btnAssociateTrack)).check(matches(allOf(isDisplayed(), isEnabled())))

        // Paso 4: la app no envió la canción al servidor
        assertNoTrackPosted()
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

    /** Entra como coleccionista, espera la lista de álbumes y abre el detalle del primero. */
    private fun openFirstAlbumDetailAsCollector() {
        onView(withId(R.id.cardCollector)).check(matches(isDisplayed()))
        onView(withId(R.id.cardCollector)).perform(scrollTo(), click())
        onView(withId(R.id.albumsRv)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.albumsRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        onView(withId(R.id.detail_card)).check(matches(isDisplayed()))
    }

    /** Toca "Asociar canción" en el detalle del álbum. */
    private fun openTrackForm() {
        onView(withId(R.id.buttonAssociateTrack)).perform(scrollTo(), click())
    }

    /** Escribe el nombre y la duración de la canción y cierra el teclado. */
    private fun fillTrackForm(name: String, duration: String) {
        onView(withId(R.id.etTrackName)).perform(replaceText(name), closeSoftKeyboard())
        onView(withId(R.id.etTrackDuration)).perform(replaceText(duration), closeSoftKeyboard())
    }

    /** El campo [layoutId] muestra el mensaje de error [messageRes] debajo. */
    private fun assertFieldError(@IdRes layoutId: Int, @StringRes messageRes: Int) {
        onView(allOf(withText(messageRes), isDescendantOfA(withId(layoutId))))
            .check(matches(isDisplayed()))
    }

    /** Ninguna de las peticiones que recibió el servidor falso fue un POST. */
    private fun assertNoTrackPosted() {
        val requests = List(server.requestCount) { server.takeRequest(1, TimeUnit.SECONDS)!! }
            .map { "${it.method} ${it.path}" }
        assertTrue("La app envió una canción inválida: $requests", requests.none { it.startsWith("POST") })
    }

    /**
     * Respuestas del servidor falso: la lista con un álbum y su detalle.
     * [postResponse] es la respuesta al asociar la canción (por defecto 404: no se espera POST).
     */
    private fun givenMockBackend(postResponse: MockResponse = MockResponse().setResponseCode(404)) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/albums" -> jsonResponse(albumsListJson)
                request.path == "/albums/100" && request.method == "GET" -> jsonResponse(albumDetailJson)
                request.path == "/albums/100/tracks" && request.method == "POST" -> postResponse
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
}

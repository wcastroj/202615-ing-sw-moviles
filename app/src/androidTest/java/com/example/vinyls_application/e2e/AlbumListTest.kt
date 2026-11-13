package com.example.vinyls_application.e2e

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.R
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.MainActivity
import com.example.vinyls_application.utils.VolleyIdlingResource
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pruebas E2E de HU01 - Consulta del catálogo de álbumes (FC-01, Objetivo 1).
 *
 * PE-001 consulta el backend real (NetworkServiceAdapter.BASE_URL), que debe estar
 * corriendo y tener al menos un álbum. PE-002 y PE-003 simulan GET /albums con
 * MockWebServer (catálogo vacío y servidor apagado).
 *
 * Espresso espera las peticiones de Volley con [VolleyIdlingResource], igual que en
 * AlbumDetailE2ETest, así que no hace falta reintentar las verificaciones.
 *
 * Compatibilidad: ejecutar en Android 13 (API 33) y Android 15 (API 35) con
 * `gradlew compatibilidadGroupDebugAndroidTest` o en dos emuladores con `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AlbumListTest {

    // DIAGNÓSTICO TEMPORAL: duración total de cada test en logcat (E2E-TIMING)
    @get:org.junit.Rule
    val timing = com.example.vinyls_application.utils.TimingRule()

    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String
    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        originalBaseUrl = NetworkServiceAdapter.BASE_URL

        // Cola de Volley vigilada por el IdlingResource: Espresso espera a que terminen las peticiones
        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue)
        IdlingRegistry.getInstance().register(idlingResource)
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

    /**
     * PE-001 (positiva): el catálogo se ve como lista de tarjetas con imagen, nombre,
     * género y botón de detalle, con los álbumes del backend real.
     */
    @Test
    fun pe001_catalogShowsAlbumCardsWithCoverNameGenreAndDetailButton() {
        // Primero se piden al backend real los datos esperados
        val albums = fetchAlbumsFromBackend()
        assertTrue("El backend no tiene álbumes en ${NetworkServiceAdapter.BASE_URL}", albums.length() > 0)

        openCatalogAsVisitor()

        // La app ya cargó la lista del backend real (Espresso esperó la petición)
        com.example.vinyls_application.utils.TimingLog.measure("pe001 albumsRv >= 1 (idling)") {
            onView(withId(R.id.albumsRv)).check(matches(hasMinimumChildCount(1)))
        }

        com.example.vinyls_application.utils.TimingLog.measure("pe001 assertions") {
            onView(withId(R.id.txtHeader)).check(matches(withText(R.string.header_home)))
            onView(withText("Catálogo de Álbumes")).check(matches(isDisplayed()))
            // Las dos primeras tarjetas son visibles en la grilla de 2 columnas
            for (i in 0 until minOf(2, albums.length())) {
                val album = albums.getJSONObject(i)
                assertAlbumCard(name = album.getString("name"), genre = album.getString("genre"))
            }
            assertGone(R.id.layoutEmpty, R.id.layoutError)
        }
    }

    /** PE-002 (negativa): sin álbumes se muestra "No se encontraron álbumes". */
    @Test
    fun pe002_emptyCatalogShowsNoAlbumsFoundMessage() {
        givenAlbumsResponse(MockResponse().setResponseCode(200).setBody("[]"))

        openCatalogAsVisitor()

        com.example.vinyls_application.utils.TimingLog.measure("pe002 layoutEmpty visible (idling)") {
            onView(withId(R.id.layoutEmpty)).check(matches(isDisplayed()))
        }
        onView(withText("No se encontraron álbumes")).check(matches(isDisplayed()))
        assertGone(R.id.layoutContent, R.id.layoutError)
    }

    /** PE-003 (negativa): sin conexión se muestra "¡Ups! Algo se desafinó en la señal". */
    @Test
    fun pe003_noConnectionShowsSignalErrorMessage() {
        // Se apaga el servidor: nada escucha en ese puerto y Volley reporta NoConnectionError
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.shutdown()

        openCatalogAsVisitor()

        com.example.vinyls_application.utils.TimingLog.measure("pe003 layoutError visible (idling)") {
            onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
        }
        onView(withText("¡Ups! Algo se\ndesafinó en la señal")).check(matches(isDisplayed()))
        assertGone(R.id.layoutContent, R.id.layoutEmpty)
    }

    /**
     * Consulta GET /albums directamente al backend real para conocer los datos esperados.
     * Corre en el hilo del test (no en el de UI), antes de abrir la app. Timeout de 10 s: si el
     * backend no responde, el test falla rápido con un error claro.
     */
    private fun fetchAlbumsFromBackend(): JSONArray = com.example.vinyls_application.utils.TimingLog.measure("test-fetch GET albums") {
        val connection = URL(NetworkServiceAdapter.BASE_URL + "albums").openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun givenAlbumsResponse(response: MockResponse) {
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.path == "/albums") response else MockResponse().setResponseCode(404)
        }
    }

    private fun launchApp() = com.example.vinyls_application.utils.TimingLog.measure("launchApp") {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Abre la app y entra como visitante; Espresso espera a que la app pida GET /albums. */
    private fun openCatalogAsVisitor() {
        launchApp()
        com.example.vinyls_application.utils.TimingLog.measure("click cardVisitor") {
            onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
            onView(withId(R.id.cardVisitor)).perform(click())
        }
    }

    /** Verifica que cada vista indicada esté oculta (GONE). */
    private fun assertGone(vararg viewIds: Int) {
        viewIds.forEach { id ->
            onView(withId(id)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    private fun assertAlbumCard(name: String, genre: String) {
        onView(withId(R.id.albumsRv)).check(
            matches(
                hasDescendant(
                    allOf(
                        withId(R.id.cardCatalog),
                        hasDescendant(allOf(withId(R.id.imgCover), isDisplayed())),
                        hasDescendant(allOf(withId(R.id.txtTitle), withText(name))),
                        hasDescendant(allOf(withId(R.id.txtSubtitle), withText(genre))),
                        hasDescendant(
                            allOf(
                                withId(R.id.btnAction),
                                withText(R.string.catalog_card_action_default),
                                isDisplayed()
                            )
                        )
                    )
                )
            )
        )
    }
}

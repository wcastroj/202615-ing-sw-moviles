package com.example.vinyls_application.e2e

import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vinyls_application.R
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.MainActivity
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
 * Pruebas E2E de HU03 - Consulta del listado de artistas (bandas y músicos).
 *
 * PE-007 consulta el backend real (NetworkServiceAdapter.BASE_URL), que debe estar corriendo
 * y tener al menos una banda y un músico. PE-008, PE-009 y PE-010 simulan GET /bands y
 * GET /musicians con MockWebServer (datos conocidos, listado vacío y servidor apagado).
 *
 * En todas se abre la app y se navega a "Artistas" desde la barra inferior.
 *
 * Compatibilidad: ejecutar en Android 13 (API 33) y Android 15 (API 35) con
 * `gradlew compatibilidadGroupDebugAndroidTest` o en dos emuladores con `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class PerformerListTest {

    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String
    private var scenario: ActivityScenario<MainActivity>? = null

    // Datos conocidos para PE-008: una banda y un músico
    private val bandsJson = """
        [{"id":1,"name":"Queen","image":"http://localhost/queen.jpg",
          "description":"Banda británica de rock","creationDate":"1970-01-01T00:00:00.000Z"}]
    """.trimIndent()

    private val musiciansJson = """
        [{"id":100,"name":"Rubén Blades","image":"http://localhost/ruben.jpg",
          "description":"Cantante panameño","birthDate":"1948-07-16T00:00:00.000Z"}]
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        originalBaseUrl = NetworkServiceAdapter.BASE_URL
    }

    @After
    fun tearDown() {
        scenario?.close()
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    /**
     * PE-007 (positiva): bandas y músicos del backend real se ven en la misma vista.
     */
    @Test
    fun pe007_bandsAndMusiciansAreShownInTheSameView() {
        val bands = fetchFromBackend("bands")
        val musicians = fetchFromBackend("musicians")
        assertTrue("El backend no tiene bandas en ${NetworkServiceAdapter.BASE_URL}", bands.length() > 0)
        assertTrue("El backend no tiene músicos en ${NetworkServiceAdapter.BASE_URL}", musicians.length() > 0)

        launchAppAndOpenArtists()

        waitUntil(timeoutMs = 30_000) {
            onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
        }
        onView(withId(R.id.txtPerformersTitle)).check(matches(withText(R.string.title_performers)))

        // Una banda y un músico en el mismo RecyclerView (se desplaza hasta cada uno)
        assertPerformerIsListed(name = bands.getJSONObject(0).getString("name"), type = "Banda")
        assertPerformerIsListed(name = musicians.getJSONObject(0).getString("name"), type = "Músico")

        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /**
     * PE-008 (positiva): cada tarjeta muestra nombre, imagen, fecha, tipo y botón de detalle.
     */
    @Test
    fun pe008_eachCardShowsNameImageDateTypeAndDetailButton() {
        givenPerformersResponses(bands = jsonResponse(bandsJson), musicians = jsonResponse(musiciansJson))

        launchAppAndOpenArtists()

        waitUntil { onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(2))) }
        // La fecha se muestra como dd/MM/yyyy: creationDate en bandas y birthDate en músicos
        assertPerformerCard(name = "Queen", type = "Banda", date = "01/01/1970")
        assertPerformerCard(name = "Rubén Blades", type = "Músico", date = "16/07/1948")
    }

    /** PE-009 (negativa): sin artistas se muestra "No se encontraron artistas". */
    @Test
    fun pe009_emptyListShowsNoArtistsFoundMessage() {
        givenPerformersResponses(bands = jsonResponse("[]"), musicians = jsonResponse("[]"))

        launchAppAndOpenArtists()

        waitUntil { onView(withId(R.id.layoutEmpty)).check(matches(isDisplayed())) }
        onView(withText("No se encontraron artistas")).check(matches(isDisplayed()))
        onView(withId(R.id.layoutContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /** PE-010 (negativa): sin conexión se muestra "¡Ups! Algo se desafinó en la señal". */
    @Test
    fun pe010_noConnectionShowsSignalErrorMessage() {
        // Se apaga el servidor: nada escucha en ese puerto y Volley reporta NoConnectionError
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.shutdown()

        launchAppAndOpenArtists()

        waitUntil { onView(withId(R.id.layoutError)).check(matches(isDisplayed())) }
        onView(withText("¡Ups! Algo se\ndesafinó en la señal")).check(matches(isDisplayed()))
        onView(withId(R.id.layoutContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /**
     * Consulta el backend real para conocer los datos esperados. También "despierta" el
     * backend antes de abrir la app, porque Volley espera solo 2,5 s por intento.
     */
    private fun fetchFromBackend(path: String): JSONArray {
        val connection = URL(NetworkServiceAdapter.BASE_URL + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        return try {
            JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    /** Apunta la app al servidor falso. /albums responde vacío porque la app abre en el catálogo. */
    private fun givenPerformersResponses(bands: MockResponse, musicians: MockResponse) {
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/bands" -> bands
                "/musicians" -> musicians
                "/albums" -> jsonResponse("[]")
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    /**
     * Respuesta 200 con JSON. Sin `charset=utf-8` Volley decodifica en ISO-8859-1 y los
     * acentos llegan mal (p. ej. "Rubén" se vería "RubÃ©n"); el backend real sí lo envía.
     */
    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    /** Abre la app (inicia en el catálogo) y toca "Artistas" en la barra inferior. */
    private fun launchAppAndOpenArtists() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitUntil { onView(withId(R.id.navigation_artists)).perform(click()) }
        // La pantalla de artistas ya está cargada cuando existe su título. Se busca por id porque
        // "Artistas" también es el texto del botón de la barra inferior, y no se exige que sea
        // visible porque en los estados vacío y de error el contenido se oculta.
        waitUntil {
            onView(withId(R.id.txtPerformersTitle)).check(matches(withText(R.string.title_performers)))
        }
    }

    /** Desplaza la lista hasta el artista y verifica su nombre y tipo. */
    private fun assertPerformerIsListed(name: String, type: String) {
        val card = allOf(
            withId(R.id.cardCatalog),
            hasDescendant(allOf(withId(R.id.txtTitle), withText(name))),
            hasDescendant(allOf(withId(R.id.txtSubtitle), withText(type)))
        )
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(card)))
        onView(card).check(matches(isDisplayed()))
    }

    private fun assertPerformerCard(name: String, type: String, date: String) {
        onView(withId(R.id.performersRv)).check(
            matches(
                hasDescendant(
                    allOf(
                        withId(R.id.cardCatalog),
                        hasDescendant(allOf(withId(R.id.imgCover), isDisplayed())),
                        hasDescendant(allOf(withId(R.id.txtTitle), withText(name))),
                        hasDescendant(allOf(withId(R.id.txtSubtitle), withText(type))),
                        hasDescendant(allOf(withId(R.id.txtDetail), withText(date))),
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

    /**
     * Volley no está sincronizado con Espresso, así que se reintenta la verificación
     * hasta que pase o se agote el tiempo.
     */
    private fun waitUntil(timeoutMs: Long = 10_000, assertion: () -> Unit) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (true) {
            try {
                assertion()
                return
            } catch (e: Throwable) {
                if (SystemClock.elapsedRealtime() > deadline) throw e
                SystemClock.sleep(200)
            }
        }
    }
}

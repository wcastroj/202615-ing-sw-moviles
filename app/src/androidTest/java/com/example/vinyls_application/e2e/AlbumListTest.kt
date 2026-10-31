package com.example.vinyls_application.e2e

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
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
 * Pruebas E2E de HU01 - Consulta del catálogo de álbumes (FC-01, Objetivo 1).
 *
 * PE-001 consulta el backend real (NetworkServiceAdapter.BASE_URL), que debe estar
 * corriendo y tener al menos un álbum. PE-002 y PE-003 simulan GET /albums con
 * MockWebServer (catálogo vacío y servidor apagado).
 *
 * Compatibilidad: ejecutar en Android 13 (API 33) y Android 15 (API 35) con
 * `gradlew compatibilidadGroupDebugAndroidTest` o en dos emuladores con `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AlbumListTest {

    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String
    private var scenario: ActivityScenario<MainActivity>? = null

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
     * PE-001 (positiva): el catálogo se ve como lista de tarjetas con imagen, nombre,
     * género y botón de detalle, con los álbumes del backend real.
     */
    @Test
    fun pe001_catalogShowsAlbumCardsWithCoverNameGenreAndDetailButton() {
        val albums = fetchAlbumsFromBackend()
        assertTrue("El backend no tiene álbumes en ${NetworkServiceAdapter.BASE_URL}", albums.length() > 0)

        launchApp()

        waitUntil(timeoutMs = 30_000) { onView(withId(R.id.albumsRv)).check(matches(hasMinimumChildCount(1))) }
        onView(withId(R.id.txtHeader)).check(matches(withText(R.string.header_home)))
        onView(withText("Catálogo de Álbumes")).check(matches(isDisplayed()))
        // Las dos primeras tarjetas son visibles en la grilla de 2 columnas
        for (i in 0 until minOf(2, albums.length())) {
            val album = albums.getJSONObject(i)
            assertAlbumCard(name = album.getString("name"), genre = album.getString("genre"))
        }
        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /** PE-002 (negativa): sin álbumes se muestra "No se encontraron álbumes". */
    @Test
    fun pe002_emptyCatalogShowsNoAlbumsFoundMessage() {
        givenAlbumsResponse(MockResponse().setResponseCode(200).setBody("[]"))

        launchApp()

        waitUntil { onView(withId(R.id.layoutEmpty)).check(matches(isDisplayed())) }
        onView(withText("No se encontraron álbumes")).check(matches(isDisplayed()))
        onView(withId(R.id.layoutContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /** PE-003 (negativa): sin conexión se muestra "¡Ups! Algo se desafinó en la señal". */
    @Test
    fun pe003_noConnectionShowsSignalErrorMessage() {
        // Se apaga el servidor: nada escucha en ese puerto y Volley reporta NoConnectionError
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.shutdown()

        launchApp()

        waitUntil { onView(withId(R.id.layoutError)).check(matches(isDisplayed())) }
        onView(withText("¡Ups! Algo se\ndesafinó en la señal")).check(matches(isDisplayed()))
        onView(withId(R.id.layoutContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /**
     * Consulta GET /albums directamente al backend para conocer los datos esperados.
     * También "despierta" el backend antes de abrir la app, porque Volley espera solo 2,5 s por intento.
     */
    private fun fetchAlbumsFromBackend(): JSONArray {
        val connection = URL(NetworkServiceAdapter.BASE_URL + "albums").openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        return try {
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

    private fun launchApp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
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

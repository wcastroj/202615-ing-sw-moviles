package com.example.vinyls_application.e2e

import android.content.Context
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
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
 * Pruebas E2E de HU03 - Consulta del listado de artistas (bandas y músicos).
 *
 * PE-007 y PE-008 consultan el backend real (NetworkServiceAdapter.BASE_URL), que debe estar
 * corriendo y tener al menos una banda y un músico. PE-009 y PE-010 simulan GET /bands y
 * GET /musicians con MockWebServer (listado vacío y servidor apagado).
 *
 * En todas se abre la app y se navega a "Artistas" desde la barra inferior.
 *
 * Espresso espera las peticiones de Volley con [VolleyIdlingResource], igual que en
 * AlbumListTest, así que las verificaciones no se reintentan.
 *
 * Compatibilidad: ejecutar en Android 13 (API 33) y Android 15 (API 35) con
 * `gradlew compatibilidadGroupDebugAndroidTest` o en dos emuladores con `connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class PerformerListTest {

    // DIAGNÓSTICO TEMPORAL: duración total de cada test en logcat (E2E-TIMING)
    @get:org.junit.Rule
    val timing = com.example.vinyls_application.utils.TimingRule()

    // DIAGNÓSTICO TEMPORAL: mide un paso y lo escribe en logcat (E2E-TIMING)
    private inline fun <T> t(label: String, block: () -> T): T =
        com.example.vinyls_application.utils.TimingLog.measure(label, block)

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
        t("tearDown") {
            scenario?.close()
            IdlingRegistry.getInstance().unregister(idlingResource)
            // Se detiene la cola para no dejar sus hilos vivos entre tests
            queue.stop()
            NetworkServiceAdapter.instance = null
            NetworkServiceAdapter.BASE_URL = originalBaseUrl
            server.shutdown()
        }
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

        t("pe007 performersRv >= 1 (idling)") {
            onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
        }
        onView(withId(R.id.txtPerformersTitle)).check(matches(withText(R.string.title_performers)))

        // Una banda y un músico en el mismo RecyclerView (se desplaza hasta cada uno)
        t("pe007 scroll + check band and musician") {
            assertPerformerIsListed(name = bands.getJSONObject(0).getString("name"), type = "Banda")
            assertPerformerIsListed(name = musicians.getJSONObject(0).getString("name"), type = "Músico")
        }

        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /**
     * PE-008 (positiva): cada tarjeta muestra nombre, imagen, fecha, tipo y botón de detalle,
     * con los datos de la primera banda y el primer músico del backend real.
     */
    @Test
    fun pe008_eachCardShowsNameImageDateTypeAndDetailButton() {
        // Paso 1: pedir al backend real los datos esperados
        val bands = fetchFromBackend("bands")
        val musicians = fetchFromBackend("musicians")
        assertTrue("El backend no tiene bandas en ${NetworkServiceAdapter.BASE_URL}", bands.length() > 0)
        assertTrue("El backend no tiene músicos en ${NetworkServiceAdapter.BASE_URL}", musicians.length() > 0)
        val band = bands.getJSONObject(0)
        val musician = musicians.getJSONObject(0)

        // Paso 2: abrir la app como visitante e ir a Artistas
        launchAppAndOpenArtists()
        t("pe008 performersRv >= 2 (idling)") { onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(2))) }

        // Paso 3: la tarjeta de cada uno muestra lo que entrega el backend. La fecha se muestra
        // como dd/MM/yyyy: creationDate en bandas y birthDate en músicos
        t("pe008 scroll + check band and musician cards") {
            assertPerformerCard(name = band.getString("name"), type = "Banda", date = toDisplayDate(band.getString("creationDate")))
            assertPerformerCard(name = musician.getString("name"), type = "Músico", date = toDisplayDate(musician.getString("birthDate")))
        }
    }

    /** PE-009 (negativa): sin artistas se muestra "No se encontraron artistas". */
    @Test
    fun pe009_emptyListShowsNoArtistsFoundMessage() {
        givenPerformersResponses(bands = jsonResponse("[]"), musicians = jsonResponse("[]"))

        launchAppAndOpenArtists()


        t("pe009 layoutEmpty visible (idling)") { onView(withId(R.id.layoutEmpty)).check(matches(isDisplayed())) }
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

        t("pe010 layoutError visible (idling)") { onView(withId(R.id.layoutError)).check(matches(isDisplayed())) }
        onView(withText("¡Ups! Algo se\ndesafinó en la señal")).check(matches(isDisplayed()))
        onView(withId(R.id.layoutContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /**
     * Consulta el backend real para conocer los datos esperados.
     * Timeout de 10 s: si el backend no responde, el test falla rápido con un error claro.
     */
    private fun fetchFromBackend(path: String): JSONArray = t("test-fetch GET $path") {
        val connection = URL(NetworkServiceAdapter.BASE_URL + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
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
        t("launchApp") { scenario = ActivityScenario.launch(MainActivity::class.java) }
        t("click cardVisitor") {
            onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
            onView(withId(R.id.cardVisitor)).perform(click())
        }
        // El clic en la barra inferior se reintenta: si llega mientras termina la navegación del
        // login, se pierde sin error. Es el único reintento de la clase; las verificaciones no lo
        // necesitan porque Espresso ya espera a Volley con el IdlingResource.
        waitUntil("click navigation_artists") { onView(withId(R.id.navigation_artists)).perform(click()) }
        // La pantalla de artistas ya está cargada cuando existe su título. Se busca por id porque
        // "Artistas" también es el texto del botón de la barra inferior, y no se exige que sea
        // visible porque en los estados vacío y de error el contenido se oculta.
        t("txtPerformersTitle (idling)") {
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

    /**
     * Desplaza la lista hasta la tarjeta del artista (la lista viene ordenada por nombre, así que
     * puede no estar en pantalla) y verifica imagen, nombre, tipo, fecha y botón de detalle.
     */
    private fun assertPerformerCard(name: String, type: String, date: String) {
        val card = allOf(
            withId(R.id.cardCatalog),
            hasDescendant(allOf(withId(R.id.txtTitle), withText(name))),
            hasDescendant(allOf(withId(R.id.txtSubtitle), withText(type))),
            hasDescendant(allOf(withId(R.id.txtDetail), withText(date)))
        )
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(card)))
        onView(card).check(
            matches(
                allOf(
                    isDisplayed(),
                    hasDescendant(allOf(withId(R.id.imgCover), isDisplayed())),
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
    }

    /** Convierte la fecha ISO del backend ("1948-07-16T00:00:00.000Z") a "16/07/1948". */
    private fun toDisplayDate(isoDate: String): String {
        val (year, month, day) = isoDate.take(10).split("-")
        return "$day/$month/$year"
    }

    /**
     * Reintenta la acción hasta que pase o se agote el tiempo. Solo se usa para el clic en
     * "Artistas"; las verificaciones esperan a Volley con el IdlingResource.
     */
    private fun waitUntil(label: String, timeoutMs: Long = 10_000, assertion: () -> Unit) {
        val start = SystemClock.elapsedRealtime()
        val deadline = start + timeoutMs
        var retries = 0
        while (true) {
            try {
                assertion()
                // DIAGNÓSTICO TEMPORAL: cuánto esperó y cuántas veces reintentó
                android.util.Log.d(com.example.vinyls_application.utils.TimingLog.TAG,
                    "waitUntil $label ${SystemClock.elapsedRealtime() - start}ms retries=$retries")
                return
            } catch (e: Throwable) {
                if (SystemClock.elapsedRealtime() > deadline) throw e
                retries++
                SystemClock.sleep(200)
            }
        }
    }
}

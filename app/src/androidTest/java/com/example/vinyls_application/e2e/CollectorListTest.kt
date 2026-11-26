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
import kotlinx.coroutines.Dispatchers
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

@RunWith(AndroidJUnit4::class)
class CollectorListTest {

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

        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue, Dispatchers.Main)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun tearDown() {
        scenario?.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        queue.stop()
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    /** PE-043 (positiva): el catálogo muestra las tarjetas de coleccionistas con imagen, nombre, teléfono, email y botón de detalle. */
    @Test
    fun pe043_catalogShowsCollectorCardsWithImageNameTelephoneEmailAndDetailButton() {
        val collectors = fetchCollectorsFromBackend()
        assertTrue(
            "El backend no tiene coleccionistas en ${NetworkServiceAdapter.BASE_URL}",
            collectors.length() > 0
        )

        launchAppAndOpenCollectors()

        onView(withId(R.id.collectorsRv)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.txtHeader)).check(matches(withText(R.string.header_home)))
        onView(withId(R.id.txtCollectorsTitle))
            .check(matches(allOf(withText(R.string.title_collectors), isDisplayed())))

        for (i in 0 until minOf(2, collectors.length())) {
            val collector = collectors.getJSONObject(i)
            assertCollectorCard(
                name = collector.getString("name"),
                telephone = collector.getString("telephone"),
                email = collector.getString("email")
            )
        }
        assertGone(R.id.layoutEmpty, R.id.layoutError)
    }

    /** PE-044 (negativa): sin coleccionistas se muestra "No se encontraron coleccionistas". */
    @Test
    fun pe044_emptyCatalogShowsNoCollectorsFoundMessage() {
        givenCollectorsResponse(jsonResponse("[]"))

        launchAppAndOpenCollectors()

        onView(withId(R.id.layoutEmpty)).check(matches(isDisplayed()))
        onView(withText("No se encontraron coleccionistas")).check(matches(isDisplayed()))
        onView(withText("Por ahora no hay coleccionistas disponibles")).check(matches(isDisplayed()))
        assertGone(R.id.layoutContent, R.id.layoutError)
    }

    /** PE-045 (negativa): sin conexión se muestra el mensaje de error de señal. */
    @Test
    fun pe045_noConnectionShowsSignalErrorMessage() {
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.shutdown()

        launchAppAndOpenCollectors()

        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
        onView(withText("¡Ups! Algo se\ndesafinó en la señal")).check(matches(isDisplayed()))
        assertGone(R.id.layoutContent, R.id.layoutEmpty)
    }

    private fun fetchCollectorsFromBackend(): JSONArray {
        val connection =
            URL(NetworkServiceAdapter.BASE_URL + "collectors").openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return try {
            JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private fun givenCollectorsResponse(response: MockResponse) {
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/collectors" -> response
                "/albums" -> jsonResponse("[]")
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private fun launchAppAndOpenCollectors() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
        onView(withId(R.id.cardVisitor)).perform(click())

        waitUntil { onView(withId(R.id.navigation_collectors)).perform(click()) }
        onView(withId(R.id.txtCollectorsTitle)).check(matches(withText(R.string.title_collectors)))
    }

    private fun assertGone(vararg viewIds: Int) {
        viewIds.forEach { id ->
            onView(withId(id)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    private fun assertCollectorCard(name: String, telephone: String, email: String) {
        val card = allOf(
            withId(R.id.cardCatalog),
            hasDescendant(allOf(withId(R.id.txtTitle), withText(name))),
            hasDescendant(allOf(withId(R.id.txtSubtitle), withText(telephone))),
            hasDescendant(allOf(withId(R.id.txtDetail), withText(email)))
        )
        onView(withId(R.id.collectorsRv))
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

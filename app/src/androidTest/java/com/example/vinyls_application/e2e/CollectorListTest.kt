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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectorListTest {

    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String
    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private var scenario: ActivityScenario<MainActivity>? = null

    private val sampleCollectorsJson = """
        [
          {
            "id": 1,
            "name": "Jaime Andrés Monsalve",
            "telephone": "3102178976",
            "email": "j.monsalve@gmail.com",
            "comments": [],
            "favoritePerformers": [],
            "collectorAlbums": []
          },
          {
            "id": 2,
            "name": "Manolo Bellon",
            "telephone": "3503102456",
            "email": "bellon@manolo.com",
            "comments": [],
            "favoritePerformers": [],
            "collectorAlbums": []
          }
        ]
    """.trimIndent()

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

    @Test
    fun collectorsAreShownWithAllDetails() {
        givenCollectorsResponse(jsonResponse(sampleCollectorsJson))

        launchAppAndOpenCollectors()

        onView(withId(R.id.collectorsRv)).check(matches(hasMinimumChildCount(2)))
        onView(withId(R.id.txtCollectorsTitle)).check(matches(withText(R.string.title_collectors)))

        assertCollectorCard(
            name = "Jaime Andrés Monsalve",
            telephone = "3102178976",
            email = "j.monsalve@gmail.com"
        )
        assertCollectorCard(
            name = "Manolo Bellon",
            telephone = "3503102456",
            email = "bellon@manolo.com"
        )

        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun emptyListShowsNoCollectorsFoundMessage() {
        givenCollectorsResponse(jsonResponse("[]"))

        launchAppAndOpenCollectors()

        onView(withId(R.id.layoutEmpty)).check(matches(isDisplayed()))
        onView(withText("No se encontraron coleccionistas")).check(matches(isDisplayed()))
        onView(withText("Por ahora no hay coleccionistas disponibles")).check(matches(isDisplayed()))

        onView(withId(R.id.layoutContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutError)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun serverErrorShowsSignalErrorMessage() {
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.shutdown()

        launchAppAndOpenCollectors()

        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
        onView(withText("¡Ups! Algo se\ndesafinó en la señal")).check(matches(isDisplayed()))

        onView(withId(R.id.layoutContent)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.layoutEmpty)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun detailButtonDoesNotExecuteAnything() {
        givenCollectorsResponse(jsonResponse(sampleCollectorsJson))

        launchAppAndOpenCollectors()

        val card = allOf(
            withId(R.id.cardCatalog),
            hasDescendant(allOf(withId(R.id.txtTitle), withText("Jaime Andrés Monsalve")))
        )
        onView(withId(R.id.collectorsRv))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(card)))

        onView(allOf(withId(R.id.btnAction), isDisplayed())).perform(click())

        onView(withId(R.id.txtCollectorsTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.collectorsRv)).check(matches(isDisplayed()))
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private fun givenCollectorsResponse(response: MockResponse) {
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.path?.startsWith("/collectors") == true) response
                else MockResponse().setResponseCode(404)
        }
    }

    private fun launchAppAndOpenCollectors() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
        onView(withId(R.id.cardVisitor)).perform(click())

        waitUntil { onView(withId(R.id.navigation_collectors)).perform(click()) }
        onView(withId(R.id.txtCollectorsTitle)).check(matches(withText(R.string.title_collectors)))
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

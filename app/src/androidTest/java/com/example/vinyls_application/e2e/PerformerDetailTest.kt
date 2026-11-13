package com.example.vinyls_application.e2e

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
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

@LargeTest
@RunWith(AndroidJUnit4::class)
class PerformerDetailTest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String
    private var scenario: ActivityScenario<MainActivity>? = null

    private val bandsListJson = """
        [{"id":1,"name":"Queen","image":"",
          "description":"Banda británica de rock","creationDate":"1970-01-01T00:00:00.000Z"}]
    """.trimIndent()

    private val musiciansListJson = """
        [{"id":100,"name":"Rubén Blades","image":"",
          "description":"Cantante panameño","birthDate":"1948-07-16T00:00:00.000Z"}]
    """.trimIndent()

    // La imagen no puede ir vacía: DetailCardWidget oculta la portada cuando no hay URL
    private val musicianDetailJson = """
        {"id":100,"name":"Rubén Blades","image":"http://localhost/ruben.jpg",
         "description":"Cantante panameño de salsa y jazz.",
         "birthDate":"1948-07-16T00:00:00.000Z",
         "albums":[{"id":10,"name":"Buscando América","cover":"",
                    "releaseDate":"1984-08-01T00:00:00.000Z","description":"Álbum de salsa.",
                    "genre":"Salsa","recordLabel":"Elektra"}],
         "performerPrizes":[]}
    """.trimIndent()

    private val bandDetailJson = """
        {"id":1,"name":"Queen","image":"",
         "description":"Banda británica de rock legendaria.",
         "creationDate":"1970-01-01T00:00:00.000Z",
         "albums":[{"id":20,"name":"A Night at the Opera","cover":"",
                    "releaseDate":"1975-11-21T00:00:00.000Z","description":"Álbum de rock.",
                    "genre":"Rock","recordLabel":"EMI"}],
         "performerPrizes":[]}
    """.trimIndent()

    private val musicianWithoutAlbumsDetailJson = """
        {"id":100,"name":"Rubén Blades","image":"",
         "description":"Cantante panameño de salsa y jazz.",
         "birthDate":"1948-07-16T00:00:00.000Z",
         "albums":[],
         "performerPrizes":[]}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        originalBaseUrl = NetworkServiceAdapter.BASE_URL
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()

        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        val queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        // Dispatchers.Main: el parseo pasa por la cola del hilo principal, que Espresso sí vigila.
        // Con Dispatchers.Default el IdlingResource quedaría libre antes de que llegue el dato a la UI.
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue, Dispatchers.Main)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun tearDown() {
        scenario?.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    @Test
    fun pe011_musicianDetailShowsInformationAndReturnButtonNavigatesToList() {
        configureMockDispatcher(
            bandsResponse = jsonResponse(bandsListJson),
            musiciansResponse = jsonResponse(musiciansListJson),
            musicianDetailResponse = jsonResponse(musicianDetailJson)
        )

        launchAppAndNavigateToArtists()

        onView(withId(R.id.performersRv)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Rubén Blades")), click()
            )
        )

        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.detailCard)))).check(
            matches(withText("Rubén Blades"))
        )
        onView(allOf(withId(R.id.cover), isDescendantOfA(withId(R.id.detailCard)))).check(
            matches(isDisplayed())
        )
        onView(allOf(withId(R.id.description), isDescendantOfA(withId(R.id.detailCard)))).check(
            matches(withText("Cantante panameño de salsa y jazz."))
        )

        onView(withId(R.id.btnReturn)).perform(click())

        onView(withId(R.id.performersRv)).check(matches(isDisplayed()))
    }

    @Test
    fun pe012_bandDetailShowsCreationDate() {
        configureMockDispatcher(
            bandsResponse = jsonResponse(bandsListJson),
            musiciansResponse = jsonResponse(musiciansListJson),
            bandDetailResponse = jsonResponse(bandDetailJson)
        )

        launchAppAndNavigateToArtists()

        onView(withId(R.id.performersRv)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Queen")), click()
            )
        )

        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.detailCard)))).check(
            matches(withText("Queen"))
        )
        onView(allOf(withId(R.id.description), isDescendantOfA(withId(R.id.detailCard)))).check(
            matches(withText("Banda británica de rock legendaria."))
        )
    }

    @Test
    fun pe013_showsAssociatedAlbumsListForPerformer() {
        configureMockDispatcher(
            bandsResponse = jsonResponse(bandsListJson),
            musiciansResponse = jsonResponse(musiciansListJson),
            musicianDetailResponse = jsonResponse(musicianDetailJson)
        )

        launchAppAndNavigateToArtists()

        onView(withId(R.id.performersRv)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Rubén Blades")), click()
            )
        )

        // En pantallas pequeñas la lista queda debajo del borde visible: se desplaza hasta ella
        onView(withId(R.id.rvRelatedAlbums)).perform(scrollTo()).check(matches(isDisplayed()))
        // Los álbumes relacionados usan SummaryCardWidget, cuyo título es summaryCardTitle
        onView(allOf(withId(R.id.summaryCardTitle), isDescendantOfA(withId(R.id.rvRelatedAlbums)))).check(
            matches(withText("Buscando América"))
        )
    }

    @Test
    fun pe014_performerWithoutAlbumsShowsEmptyAlbumsState() {
        configureMockDispatcher(
            bandsResponse = jsonResponse(bandsListJson),
            musiciansResponse = jsonResponse(musiciansListJson),
            musicianDetailResponse = jsonResponse(musicianWithoutAlbumsDetailJson)
        )

        launchAppAndNavigateToArtists()

        onView(withId(R.id.performersRv)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Rubén Blades")), click()
            )
        )

        onView(withText("Sin álbumes disponibles")).check(matches(isDisplayed()))
    }

    @Test
    fun pe015_loadingFailureShowsErrorMessage() {
        configureMockDispatcher(
            bandsResponse = jsonResponse(bandsListJson),
            musiciansResponse = jsonResponse(musiciansListJson),
            musicianDetailResponse = MockResponse().setResponseCode(500)
        )

        launchAppAndNavigateToArtists()

        onView(withId(R.id.performersRv)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("Rubén Blades")), click()
            )
        )

        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
        onView(withText("¡Ups! Algo se\ndesafinó en la señal")).check(matches(isDisplayed()))
        onView(withText("Algo inesperado ha ocurrido.\nIntenta más tarde.")).check(
            matches(isDisplayed())
        )
    }

    private fun configureMockDispatcher(
        bandsResponse: MockResponse,
        musiciansResponse: MockResponse,
        musicianDetailResponse: MockResponse = MockResponse().setResponseCode(404),
        bandDetailResponse: MockResponse = MockResponse().setResponseCode(404)
    ) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/albums" -> jsonResponse("[]")
                "/bands" -> bandsResponse
                "/musicians" -> musiciansResponse
                "/musicians/100" -> musicianDetailResponse
                "/bands/1" -> bandDetailResponse
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    private fun jsonResponse(body: String) = MockResponse().setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8").setBody(body)

    /**
     * La app abre en el login y la barra inferior está oculta hasta elegir un rol.
     * Se entra como visitante y luego se toca "Artistas".
     */
    private fun launchAppAndNavigateToArtists() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
        onView(withId(R.id.cardVisitor)).perform(click())
        onView(withId(R.id.navigation_artists)).perform(click())
        onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
    }
}

package com.example.vinyls_application.e2e

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AssociateAlbumE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var server: MockWebServer

    private val albumsListJson = """
        [
          {"id":100,"name":"Buscando América","cover":"","releaseDate":"1984-08-01T00:00:00.000Z","description":"Álbum.","genre":"Salsa","recordLabel":"Elektra"},
          {"id":101,"name":"Signos","cover":"","releaseDate":"1986-11-10T00:00:00.000Z","description":"Álbum de Soda Stereo.","genre":"Rock","recordLabel":"CBS"}
        ]
    """.trimIndent()

    private val musiciansListJson = """
        [{"id":100,"name":"Rubén Blades","image":"","description":"Cantante panameño","birthDate":"1948-07-16T00:00:00.000Z"}]
    """.trimIndent()

    private val musicianDetailWithAlbumJson = """
        {
          "id":100,
          "name":"Rubén Blades",
          "image":"",
          "description":"Cantante panameño",
          "birthDate":"1948-07-16T00:00:00.000Z",
          "albums":[{"id":100,"name":"Buscando América","cover":"","releaseDate":"1984-08-01T00:00:00.000Z","description":"Álbum.","genre":"Salsa","recordLabel":"Elektra"}],
          "performerPrizes":[]
        }
    """.trimIndent()

    private val musicianDetailAllAssociatedJson = """
        {
          "id":100,
          "name":"Rubén Blades",
          "image":"",
          "description":"Cantante panameño",
          "birthDate":"1948-07-16T00:00:00.000Z",
          "albums":[
            {"id":100,"name":"Buscando América","cover":"","releaseDate":"1984-08-01T00:00:00.000Z","description":"Álbum.","genre":"Salsa","recordLabel":"Elektra"},
            {"id":101,"name":"Signos","cover":"","releaseDate":"1986-11-10T00:00:00.000Z","description":"Álbum.","genre":"Rock","recordLabel":"CBS"}
          ],
          "performerPrizes":[]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)

        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue, Dispatchers.Main)
        IdlingRegistry.getInstance().register(idlingResource)

        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
    }

    @After
    fun tearDown() {
        scenario?.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        queue.stop()
        NetworkServiceAdapter.instance = null
        server.shutdown()
    }

    @Test
    fun pe037_collectorCanOpenAssociateScreenFromArtistDetail() {
        setupMockServerForMusicianDetail(musicianDetailWithAlbumJson)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        openFirstArtistDetailAsCollector()

        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        onView(withId(R.id.dropdownAlbums)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSubmitAssociate)).check(matches(isDisplayed()))
    }

    @Test
    fun pe038_associateAlbumSuccessfullyShowsUpdatedDetail() {
        var postCalled = false
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/albums" -> jsonResponse(albumsListJson)
                    request.path == "/bands" -> jsonResponse("[]")
                    request.path == "/musicians" -> jsonResponse(musiciansListJson)
                    request.path == "/musicians/100" && request.method == "GET" -> {
                        val body = if (postCalled) musicianDetailAllAssociatedJson else musicianDetailWithAlbumJson
                        jsonResponse(body)
                    }
                    request.path == "/musicians/100/albums/101" && request.method == "POST" -> {
                        postCalled = true
                        jsonResponse("""{"id":101}""")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)

        openFirstArtistDetailAsCollector()
        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        // Seleccionar álbum del desplegable y asociar
        onView(withId(R.id.autoCompleteTextView)).perform(click())
        onView(withText("Signos")).perform(click())
        onView(withId(R.id.btnSubmitAssociate)).perform(click())

        // Retorna al detalle del artista y el álbum recién asociado "Signos" es visible
        onView(withId(R.id.rvRelatedAlbums)).check(matches(hasDescendant(withText("Signos"))))
    }

    @Test
    fun pe039_associateDropdownExcludesAlreadyAssociatedAlbums() {
        setupMockServerForMusicianDetail(musicianDetailWithAlbumJson)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        openFirstArtistDetailAsCollector()
        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        onView(withId(R.id.autoCompleteTextView)).perform(click())

        // "Signos" está disponible
        onView(withText("Signos")).check(matches(isDisplayed()))
        // "Buscando América" NO aparece porque ya está asociado
        onView(withText("Buscando América")).check(doesNotExist())
    }

    @Test
    fun pe040_associateAlbumErrorShowsErrorScreen() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/albums" -> jsonResponse(albumsListJson)
                    request.path == "/bands" -> jsonResponse("[]")
                    request.path == "/musicians" -> jsonResponse(musiciansListJson)
                    request.path == "/musicians/100" && request.method == "GET" -> jsonResponse(musicianDetailWithAlbumJson)
                    request.path == "/musicians/100/albums/101" && request.method == "POST" -> MockResponse().setResponseCode(500)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)

        openFirstArtistDetailAsCollector()
        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        onView(withId(R.id.autoCompleteTextView)).perform(click())
        onView(withText("Signos")).perform(click())
        onView(withId(R.id.btnSubmitAssociate)).perform(click())

        // Muestra la vista de error
        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
    }

    @Test
    fun pe041_noAvailableAlbumsShowsEmptyState() {
        setupMockServerForMusicianDetail(musicianDetailAllAssociatedJson)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        openFirstArtistDetailAsCollector()
        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        // Muestra el widget de estado vacío "Sin álbumes que asociar"
        onView(withId(R.id.layoutEmptyAlbums)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.emptyAlbumsInformationWidget))))
            .check(matches(withText(R.string.associate_no_albums_title)))
    }

    @Test
    fun pe042_visitorRoleDoesNotSeeAssociateOption() {
        setupMockServerForMusicianDetail(musicianDetailWithAlbumJson)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Entrar como Visitante
        onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
        onView(withId(R.id.cardVisitor)).perform(scrollTo(), click())

        // Navegar a Artistas
        onView(withId(R.id.navigation_artists)).perform(click())
        onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // El botón para asociar NO está visible
        onView(withId(R.id.buttonAssociateAlbum)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
    }

    private fun openFirstArtistDetailAsCollector() {
        onView(withId(R.id.cardCollector)).check(matches(isDisplayed()))
        onView(withId(R.id.cardCollector)).perform(scrollTo(), click())

        onView(withId(R.id.navigation_artists)).perform(click())
        onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    }

    private fun setupMockServerForMusicianDetail(musicianDetailJson: String) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/albums" -> jsonResponse(albumsListJson)
                    request.path == "/bands" -> jsonResponse("[]")
                    request.path == "/musicians" -> jsonResponse(musiciansListJson)
                    request.path == "/musicians/100" -> jsonResponse(musicianDetailJson)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)
}

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
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

@LargeTest
@RunWith(AndroidJUnit4::class)
class AssociateAlbumE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

    private var firstArtistId = 0

    private val mockAlbumsListJson = """
        [
          {"id":100,"name":"Buscando América","cover":"","releaseDate":"1984-08-01T00:00:00.000Z","description":"Álbum.","genre":"Salsa","recordLabel":"Elektra"},
          {"id":101,"name":"Signos","cover":"","releaseDate":"1986-11-10T00:00:00.000Z","description":"Álbum de Soda Stereo.","genre":"Rock","recordLabel":"CBS"}
        ]
    """.trimIndent()

    private val mockMusiciansListJson = """
        [{"id":100,"name":"Rubén Blades","image":"","description":"Cantante panameño","birthDate":"1948-07-16T00:00:00.000Z"}]
    """.trimIndent()

    private val mockMusicianDetailWithAlbumJson = """
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

    private val mockMusicianDetailAllAssociatedJson = """
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
    fun pe037_collectorCanOpenAssociateScreenFromArtistDetail() {
        launchApp(useMockServer = false)

        openFirstArtistDetailAsCollector()

        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        onView(withId(R.id.dropdownAlbums)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSubmitAssociate)).check(matches(isDisplayed()))
    }

    @Test
    fun pe038_associateAlbumSuccessfullyShowsUpdatedDetail() {
        launchApp(useMockServer = false)

        openFirstArtistDetailAsCollector()

        val allCatalogAlbums = fetchArrayFromBackend("albums")
        val isMusician = isCurrentArtistMusician(firstArtistId)
        val path = if (isMusician) "musicians/$firstArtistId" else "bands/$firstArtistId"
        val artistDetail = fetchFromBackend(path)
        val associatedAlbumIds = (0 until artistDetail.getJSONArray("albums").length())
            .map { artistDetail.getJSONArray("albums").getJSONObject(it).getInt("id") }
            .toSet()

        var targetAlbumToAssociate: JSONObject? = null
        for (i in 0 until allCatalogAlbums.length()) {
            val album = allCatalogAlbums.getJSONObject(i)
            if (album.getInt("id") !in associatedAlbumIds) {
                targetAlbumToAssociate = album
                break
            }
        }

        if (targetAlbumToAssociate == null) {
            onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())
            onView(withId(R.id.layoutEmptyAlbums)).check(matches(isDisplayed()))
            return
        }

        val targetAlbumId = targetAlbumToAssociate.getInt("id")
        val targetAlbumTitle = targetAlbumToAssociate.getString("name")

        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        onView(withId(R.id.autoCompleteTextView)).perform(click())
        onView(withText(targetAlbumTitle)).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.btnSubmitAssociate)).perform(click())

        onView(withId(R.id.rvRelatedAlbums)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.rvRelatedAlbums))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(targetAlbumTitle))))
        onView(withId(R.id.rvRelatedAlbums)).check(matches(hasDescendant(withText(targetAlbumTitle))))

        val updatedArtistDetail = fetchFromBackend(path)
        val updatedAlbums = (0 until updatedArtistDetail.getJSONArray("albums").length())
            .map { updatedArtistDetail.getJSONArray("albums").getJSONObject(it).getInt("id") }
        assertTrue("No asoció el álbum $targetAlbumId al artista $firstArtistId", updatedAlbums.contains(targetAlbumId))
    }

    @Test
    fun pe039_associateDropdownExcludesAlreadyAssociatedAlbums() {
        launchApp(useMockServer = false)

        openFirstArtistDetailAsCollector()

        val isMusician = isCurrentArtistMusician(firstArtistId)
        val path = if (isMusician) "musicians/$firstArtistId" else "bands/$firstArtistId"
        val artistDetail = fetchFromBackend(path)
        val existingAlbums = artistDetail.getJSONArray("albums")

        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        if (existingAlbums.length() > 0) {
            val associatedAlbumName = existingAlbums.getJSONObject(0).getString("name")
            onView(withId(R.id.autoCompleteTextView)).perform(click())
            onView(withText(associatedAlbumName)).inRoot(isPlatformPopup()).check(doesNotExist())
        } else {
            onView(withId(R.id.dropdownAlbums)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun pe040_associateAlbumErrorShowsErrorScreen() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/albums" -> jsonResponse(mockAlbumsListJson)
                    request.path == "/bands" -> jsonResponse("[]")
                    request.path == "/musicians" -> jsonResponse(mockMusiciansListJson)
                    request.path == "/musicians/100" && request.method == "GET" -> jsonResponse(mockMusicianDetailWithAlbumJson)
                    request.path == "/musicians/100/albums/101" && request.method == "POST" -> MockResponse().setResponseCode(500)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        launchApp(useMockServer = true)

        openFirstArtistDetailAsCollector()
        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        onView(withId(R.id.autoCompleteTextView)).perform(click())
        onView(withText("Signos")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.btnSubmitAssociate)).perform(click())

        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
    }

    @Test
    fun pe041_noAvailableAlbumsShowsEmptyState() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/albums" -> jsonResponse(mockAlbumsListJson)
                    request.path == "/bands" -> jsonResponse("[]")
                    request.path == "/musicians" -> jsonResponse(mockMusiciansListJson)
                    request.path == "/musicians/100" && request.method == "GET" -> jsonResponse(mockMusicianDetailAllAssociatedJson)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        launchApp(useMockServer = true)

        openFirstArtistDetailAsCollector()
        onView(withId(R.id.buttonAssociateAlbum)).perform(scrollTo(), click())

        onView(withId(R.id.layoutEmptyAlbums)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.emptyAlbumsInformationWidget))))
            .check(matches(withText(R.string.associate_no_albums_title)))
    }

    @Test
    fun pe042_visitorRoleDoesNotSeeAssociateOption() {
        launchApp(useMockServer = false)

        onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
        onView(withId(R.id.cardVisitor)).perform(scrollTo(), click())

        onView(withId(R.id.navigation_artists)).perform(click())
        onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        onView(withId(R.id.buttonAssociateAlbum)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
    }

    private fun launchApp(useMockServer: Boolean = false) {
        if (useMockServer) {
            NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        } else {
            NetworkServiceAdapter.BASE_URL = originalBaseUrl
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun openFirstArtistDetailAsCollector() {
        onView(withId(R.id.cardCollector)).check(matches(isDisplayed()))
        onView(withId(R.id.cardCollector)).perform(scrollTo(), click())

        onView(withId(R.id.navigation_artists)).perform(click())
        onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
        firstArtistId = readFirstArtistId()
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    }

    private fun readFirstArtistId(): Int {
        var artistId = 0
        scenario!!.onActivity { activity ->
            val recyclerView = activity.findViewById<RecyclerView>(R.id.performersRv)
            artistId = recyclerView.findViewHolderForAdapterPosition(0)!!
                .itemView.getTag(R.id.tag_album_id) as Int
        }
        return artistId
    }

    private fun isCurrentArtistMusician(artistId: Int): Boolean {
        val musicians = fetchArrayFromBackend("musicians")
        for (i in 0 until musicians.length()) {
            if (musicians.getJSONObject(i).getInt("id") == artistId) {
                return true
            }
        }
        return false
    }

    private fun fetchFromBackend(path: String): JSONObject {
        val url = URL(NetworkServiceAdapter.BASE_URL.trimEnd('/') + "/" + path)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return try {
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchArrayFromBackend(path: String): JSONArray {
        val url = URL(NetworkServiceAdapter.BASE_URL.trimEnd('/') + "/" + path)
        val connection = url.openConnection() as HttpURLConnection
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
}

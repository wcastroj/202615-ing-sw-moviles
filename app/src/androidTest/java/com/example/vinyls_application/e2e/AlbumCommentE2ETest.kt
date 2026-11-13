package com.example.vinyls_application.e2e

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.R
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.MainActivity
import com.example.vinyls_application.utils.VolleyIdlingResource
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AlbumCommentE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

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
          "tracks":[],
          "performers":[],
          "comments":[]
        }
    """.trimIndent()

    private val albumDetailWithCommentJson = """
        {
          "id":100,
          "name":"Buscando América",
          "cover":"http://localhost/cover.jpg",
          "releaseDate":"1984-08-01T00:00:00.000Z",
          "description":"Álbum de salsa.",
          "genre":"Salsa",
          "recordLabel":"Elektra",
          "tracks":[],
          "performers":[],
          "comments":[{"id":1,"description":"Excelente album clasico","rating":"5"}]
        }
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
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun tearDown() {
        try { scenario.close() } catch (_: Exception) {}
        try { IdlingRegistry.getInstance().unregister(idlingResource) } catch (_: Exception) {}
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    @Test
    fun pe016_collectorCanCommentAlbumSuccessfully() {
        server.dispatcher = object : Dispatcher() {
            var commentPosted = false
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/albums" -> MockResponse().setResponseCode(200).setBody(albumsListJson)
                    request.path == "/albums/100" && request.method == "GET" -> {
                        val body = if (commentPosted) albumDetailWithCommentJson else albumDetailJson
                        MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json; charset=utf-8").setBody(body)
                    }
                    request.path == "/albums/100/comments" && request.method == "POST" -> {
                        commentPosted = true
                        MockResponse().setResponseCode(201).setBody("{\"id\":1,\"description\":\"Excelente album clasico\",\"rating\":\"5\"}")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.cardCollector)).perform(click())
        onView(withId(R.id.albumsRv)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.albumsRv)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        onView(withId(R.id.etComment)).perform(scrollTo(), replaceText("Excelente album clasico"), closeSoftKeyboard())
        onView(withId(R.id.rbRating5)).perform(scrollTo(), click())
        onView(withId(R.id.btnSubmitComment)).perform(scrollTo(), click())

        onView(withId(R.id.comments_list)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun pe017_commentErrorShowsErrorScreen() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path == "/albums" -> MockResponse().setResponseCode(200).setBody(albumsListJson)
                    request.path == "/albums/100" && request.method == "GET" -> MockResponse().setResponseCode(200).setBody(albumDetailJson)
                    request.path == "/albums/100/comments" && request.method == "POST" -> MockResponse().setResponseCode(500)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.cardCollector)).perform(click())
        onView(withId(R.id.albumsRv)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        onView(withId(R.id.etComment)).perform(scrollTo(), replaceText("Comentario fallido"), closeSoftKeyboard())
        onView(withId(R.id.rbRating4)).perform(scrollTo(), click())
        onView(withId(R.id.btnSubmitComment)).perform(scrollTo(), click())

        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
    }
}
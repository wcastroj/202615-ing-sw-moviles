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

class AlbumDetailE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

    // Datos falsos para pe006: la lista de álbumes
    private val albumsJson = """
        [{"id":100,"name":"Buscando América","cover":"http://localhost/cover.jpg","releaseDate":"1984-08-01T00:00:00.000Z",
          "description":"Primer álbum de Rubén Blades y Seis del Solar.","genre":"Salsa","recordLabel":"Elektra"}]
    """.trimIndent()

    @Before
    fun setUp() {
        // 1. Servidor falso, solo lo usa pe006: la lista responde bien y el detalle responde 500
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/albums" -> MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json; charset=utf-8")
                        .setBody(albumsJson)

                    "/albums/100" -> MockResponse().setResponseCode(500)

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        // 2. Guardar la URL del backend real; pe004 y pe005 la usan tal cual
        originalBaseUrl = NetworkServiceAdapter.BASE_URL

        // 3. Cola de Volley vigilada por el IdlingResource
        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        val queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue)
        IdlingRegistry.getInstance().register(idlingResource)

        // 4. Abrir la app
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    @Test
    fun pe004_OpenAlbumDetailAndBackToList() {
        // Paso 1: la lista de álbumes se ve
        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))

        // Paso 2: tocar el primer álbum
        onView(withId(R.id.albumsRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Paso 3: se abre el detalle en una pantalla aparte
        onView(withId(R.id.detail_card)).check(matches(isDisplayed()))
        onView(withId(R.id.albumsRv)).check(doesNotExist())

        // Paso 4: tocar el botón de regresar
        onView(withId(R.id.btnReturn)).perform(click())

        // Paso 5: se regresa a la lista
        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withText("Catálogo de Álbumes")).check(matches(isDisplayed()))
    }

    @Test
    fun pe005_AlbumDetailShowAllInformation() {
        // Paso 1: la lista muestra el álbum
        onView(withText("Buscando América")).check(matches(isDisplayed()))

        // Paso 2: abrir el álbum
        onView(withText("Buscando América")).perform(click())

        // Paso 3: portada y nombre
        onView(allOf(withId(R.id.cover), isDescendantOfA(withId(R.id.detail_card))))
            .check(matches(isDisplayed()))
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.detail_card))))
            .check(matches(withText("Buscando América")))

        // Paso 4: género
        onView(allOf(withId(R.id.tag), isDescendantOfA(withId(R.id.detail_card))))
            .check(matches(withText("Salsa")))

        // Paso 5: número de canciones
        onView(allOf(withId(R.id.subtitle), isDescendantOfA(withId(R.id.detail_card))))
            .check(matches(withText("• 2 Canciones")))

        // Paso 6: canciones relacionadas
        onView(allOf(withText("Decisiones"), isDescendantOfA(withId(R.id.tracksListWidget))))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(allOf(withText("Desapariciones"), isDescendantOfA(withId(R.id.tracksListWidget))))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // Paso 7: artistas relacionados
        onView(allOf(withText("Rubén Blades Bellido de Luna"), isDescendantOfA(withId(R.id.performersListWidget))))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // Paso 8: comentario y su calificación
        onView(allOf(withText("The most relevant album of Ruben Blades"), isDescendantOfA(withId(R.id.comments_list))))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(allOf(withText("5/5"), isDescendantOfA(withId(R.id.comments_list))))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun pe006_AlbumDetailErrorShowsMessageAndReturnsToList() {
        // Paso 1: apuntar la app al servidor falso (lista OK, detalle 500) y volver a abrirla
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        scenario.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Paso 2: la lista muestra el álbum del servidor falso
        onView(withText("Buscando América")).check(matches(isDisplayed()))

        // Paso 3: abrir el álbum; el detalle falla
        onView(withText("Buscando América")).perform(click())

        // Paso 4: se muestra la pantalla de error con el mensaje exacto
        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.description), isDescendantOfA(withId(R.id.errorInformationWidget))))
            .check(matches(withText("Algo inesperado ha ocurrido.\nIntenta más tarde.")))

        // Paso 5: tocar el botón de regresar
        onView(withId(R.id.btnReturn)).perform(click())

        // Paso 6: se vuelve a la lista
        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withText("Buscando América")).check(matches(isDisplayed()))
    }
}
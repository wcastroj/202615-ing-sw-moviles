package com.example.vinyls_application

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.vinyls_application.ui.MainActivity
import com.example.vinyls_application.utils.VolleyIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.network.NetworkServiceAdapter
import org.hamcrest.Matchers.allOf
import org.junit.Test
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA


@LargeTest
@RunWith(AndroidJUnit4::class)
class AlbumDetailE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        val queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue)
        IdlingRegistry.getInstance().register(idlingResource)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        NetworkServiceAdapter.instance = null
    }

    @Test
    fun pe004_OpenAlbumDetailAndBackToList() {
        // Paso 1: la lista de álbumes se ve
        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))

        // Paso 2: tocar el primer álbum
        onView(withId(R.id.albumsRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Paso 3: se abre el detalle en una pantalla aparte
        onView(withId(R.id.cover)).check(matches(isDisplayed()))
        onView(withId(R.id.albumsRv)).check(doesNotExist())

        // Paso 4: tocar el botón de regresar
        onView(withId(R.id.btnReturn)).perform(click())

        // Paso 5: se regresa a la lista
        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withText("Catálogo de Álbumes")).check(matches(isDisplayed()))
    }

    @Test
    fun pe005_AlbumDetailShowAllInformation() {
        // Paso 1: la lista de álbumes se ve
        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withText("Buscando América")).check(matches(isDisplayed()))

        // Paso 2: abrir el álbum que tiene toda la información
        onView(withText("Buscando América")).perform(click())

        // Paso 3: nombre y covert
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.detail_card))))
            .check(matches(withText("Buscando América")))
        // Paso 3: portada
        onView(allOf(withId(R.id.cover), isDescendantOfA(withId(R.id.detail_card))))
            .check(matches(isDisplayed()))

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
}

package com.example.vinyls_application.e2e

import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.R
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.MainActivity
import com.example.vinyls_application.utils.AlbumDateFormatter
import com.example.vinyls_application.utils.VolleyIdlingResource
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Pruebas E2E de HU05 - Crear un álbum (PE-016 a PE-024). Usan MockWebServer para no dejar álbumes
 * de prueba en el backend real. Espresso espera las peticiones de Volley con [VolleyIdlingResource].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AlbumCreateE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

    private val albums = CopyOnWriteArrayList(
        listOf(
            albumJson(100, "Buscando América", "Salsa", "Elektra"),
            albumJson(101, "A Night at the Opera", "Rock", "EMI")
        )
    )
    private val postedBodies = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        originalBaseUrl = NetworkServiceAdapter.BASE_URL
        NetworkServiceAdapter.BASE_URL = server.url("/").toString()

        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue, Dispatchers.Main)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun tearDown() {
        scenario.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        queue.stop()
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    /** PE-016 (positiva): el botón "Crear álbum" abre el formulario. */
    @Test
    fun pe016_createAlbumButtonOpensForm() {
        givenBackend()
        launchApp()
        onView(withId(R.id.cardCollector)).perform(scrollTo(), click())

        onView(withId(R.id.btnCreateAlbum)).check(matches(isDisplayed())).perform(click())

        onView(withText(R.string.album_create_title)).check(matches(isDisplayed()))
        onView(withId(R.id.etCover)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSaveAlbum)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.btnReturn)).check(matches(isDisplayed()))
    }

    /** PE-017 (positiva): con datos válidos se muestra el éxito y se regresa a la lista. */
    @Test
    fun pe017_validFormShowsSuccessAndReturnsToList() {
        givenBackend()
        openFormAsCollector()

        fillValidForm()
        saveAlbum()

        // Paso 1: se muestra "Álbum creado exitosamente"
        onView(withText(R.string.album_create_success_title)).check(matches(isDisplayed()))

        // Paso 2: el backend recibió los datos del formulario
        val body = JSONObject(postedBodies.single())
        assertEquals(ALBUM_NAME, body.getString("name"))
        assertEquals("https://example.com/portada.jpg", body.getString("cover"))
        assertEquals("Rock", body.getString("genre"))
        assertEquals("EMI", body.getString("recordLabel"))
        assertEquals("Un álbum de prueba", body.getString("description"))
        val today = AlbumDateFormatter.toDisplay(MaterialDatePicker.todayInUtcMilliseconds())
        assertEquals(AlbumDateFormatter.toIso(today), body.getString("releaseDate"))

        // Paso 3: se regresa a la lista, que ya incluye el álbum nuevo
        clickInformationButton(R.id.successInformationWidget)
        onView(withId(R.id.albumsRv)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(ALBUM_NAME)))
        )
        onView(withText(ALBUM_NAME)).check(matches(isDisplayed()))
    }

    /**
     * PE-018 (positiva): salir del formulario regresa a la lista sin guardar. El formulario no tiene
     * botón "Cancelar": se sale con la flecha de regreso del encabezado.
     */
    @Test
    fun pe018_returnArrowGoesBackToListWithoutSaving() {
        givenBackend()
        openFormAsCollector()
        typeText(R.id.etName, ALBUM_NAME)

        onView(withId(R.id.btnReturn)).perform(click())

        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withText(ALBUM_NAME)).check(doesNotExist())
        assertEquals(0, postedBodies.size)
    }

    /** PE-019 (negativa): guardar con campos vacíos muestra "Este campo es obligatorio". */
    @Test
    fun pe019_savingEmptyFormShowsRequiredErrors() {
        givenBackend()
        openFormAsCollector()

        saveAlbum()

        listOf(
            R.id.tilCover, R.id.tilName, R.id.tilRecordLabel,
            R.id.tilGenre, R.id.tilReleaseDate, R.id.tilDescription
        ).forEach { assertFieldError(it, R.string.album_create_error_required) }
        assertEquals(0, postedBodies.size)
    }

    /** PE-020 (negativa): un nombre de más de 50 caracteres muestra su mensaje de error. */
    @Test
    fun pe020_nameLongerThan50ShowsError() {
        givenBackend()
        openFormAsCollector()

        typeText(R.id.etName, "a".repeat(51))
        assertFieldError(R.id.tilName, R.string.album_create_error_name_length)

        saveAlbum()
        assertFieldError(R.id.tilName, R.string.album_create_error_name_length)
        assertEquals(0, postedBodies.size)

        typeText(R.id.etName, "a".repeat(50))
        assertFieldError(R.id.tilName, null)
    }

    /** PE-021 (negativa): una descripción de más de 250 caracteres muestra su mensaje de error. */
    @Test
    fun pe021_descriptionLongerThan250ShowsError() {
        givenBackend()
        openFormAsCollector()

        typeText(R.id.etDescription, "a".repeat(251))
        assertFieldError(R.id.tilDescription, R.string.album_create_error_description_length)

        saveAlbum()
        assertFieldError(R.id.tilDescription, R.string.album_create_error_description_length)
        assertEquals(0, postedBodies.size)

        typeText(R.id.etDescription, "a".repeat(250))
        assertFieldError(R.id.tilDescription, null)
    }

    /** PE-022 (negativa): un cover inválido muestra su mensaje de error. */
    @Test
    fun pe022_invalidCoverShowsError() {
        givenBackend()
        openFormAsCollector()

        typeText(R.id.etCover, "https://example.com/portada.gif")
        assertFieldError(R.id.tilCover, R.string.album_create_error_cover)

        saveAlbum()
        assertFieldError(R.id.tilCover, R.string.album_create_error_cover)
        assertEquals(0, postedBodies.size)

        typeText(R.id.etCover, "https://example.com/portada.webp")
        assertFieldError(R.id.tilCover, null)
    }

    /** PE-023 (negativa): si falla la creación se muestra "¡Ups! Algo se desafinó en la señal". */
    @Test
    fun pe023_createFailureShowsErrorMessage() {
        givenBackend(postResponse = MockResponse().setResponseCode(500))
        openFormAsCollector()

        fillValidForm()
        saveAlbum()

        onView(withText(R.string.album_create_error_title)).check(matches(isDisplayed()))
        assertEquals(1, postedBodies.size)

        // Lo escrito se conserva para poder intentarlo de nuevo
        clickInformationButton(R.id.errorInformationWidget)
        onView(withId(R.id.etName)).perform(scrollTo()).check(matches(withText(ALBUM_NAME)))
    }

    /** PE-024 (negativa): con rol Visitante no se ve el botón "Crear álbum". */
    @Test
    fun pe024_visitorDoesNotSeeCreateAlbumButton() {
        givenBackend()
        launchApp()

        onView(withId(R.id.cardVisitor)).perform(scrollTo(), click())

        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withId(R.id.btnCreateAlbum))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    private fun launchApp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun openFormAsCollector() {
        launchApp()
        onView(withId(R.id.cardCollector)).perform(scrollTo(), click())
        onView(withId(R.id.btnCreateAlbum)).perform(click())
        onView(withId(R.id.etCover)).check(matches(isDisplayed()))
    }

    private fun fillValidForm() {
        typeText(R.id.etCover, "https://example.com/portada.jpg")
        typeText(R.id.etName, ALBUM_NAME)
        selectOption(R.id.actvRecordLabel, "EMI")
        selectOption(R.id.actvGenre, "Rock")
        onView(withId(R.id.etReleaseDate)).perform(scrollTo(), click())
        onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
        typeText(R.id.etDescription, "Un álbum de prueba")
    }

    private fun typeText(@IdRes fieldId: Int, text: String) {
        onView(withId(fieldId)).perform(scrollTo(), replaceText(text), closeSoftKeyboard())
    }

    private fun selectOption(@IdRes fieldId: Int, option: String) {
        onView(withId(fieldId)).perform(scrollTo(), click())
        onView(withText(option)).inRoot(isPlatformPopup()).perform(click())
    }

    private fun saveAlbum() {
        onView(withId(R.id.btnSaveAlbum)).perform(scrollTo(), click())
    }

    private fun clickInformationButton(@IdRes widgetId: Int) {
        onView(allOf(withId(R.id.buttonPrimary), isDescendantOfA(withId(widgetId)))).perform(click())
    }

    private fun assertFieldError(@IdRes layoutId: Int, @StringRes messageRes: Int?) {
        onView(withId(layoutId)).perform(scrollTo())
        val expected = messageRes?.let {
            InstrumentationRegistry.getInstrumentation().targetContext.getString(it)
        }
        scenario.onActivity { activity ->
            assertEquals(expected, activity.findViewById<TextInputLayout>(layoutId).error?.toString())
        }
    }

    /** Servidor falso. Un POST exitoso agrega el álbum a la lista, como haría el backend. */
    private fun givenBackend(postResponse: MockResponse? = null) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path != "/albums" -> MockResponse().setResponseCode(404)
                request.method == "POST" -> {
                    val body = request.body.readUtf8()
                    postedBodies.add(body)
                    postResponse ?: createAlbum(JSONObject(body))
                }
                else -> jsonResponse(albums.joinToString(",", "[", "]"))
            }
        }
    }

    private fun createAlbum(body: JSONObject): MockResponse {
        val album = albumJson(
            200, body.getString("name"), body.getString("genre"), body.getString("recordLabel")
        )
        albums.add(album)
        return jsonResponse(album)
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private fun albumJson(id: Int, name: String, genre: String, recordLabel: String) =
        """{"id":$id,"name":"$name","cover":"http://localhost/$id.jpg","releaseDate":"1984-08-01T00:00:00.000Z","description":"Album","genre":"$genre","recordLabel":"$recordLabel"}"""

    private companion object {
        const val ALBUM_NAME = "Ecos del Atardecer"
    }
}

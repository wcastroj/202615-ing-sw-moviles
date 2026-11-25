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
import com.example.vinyls_application.utils.VolleyIdlingResource
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Pruebas E2E de HU05 - Crear un álbum (PE-016 a PE-024).
 *
 * Las pruebas positivas (PE-016 a PE-018) usan el backend real (NetworkServiceAdapter.BASE_URL);
 * PE-017 deja un álbum nuevo en cada corrida. Las negativas (PE-019 a PE-024) usan MockWebServer
 * con launchApp(useMockServer = true).
 *
 * Espresso espera las peticiones de Volley con [VolleyIdlingResource], igual que en las demás E2E,
 * así que las verificaciones no se reintentan.
 *
 * Compatibilidad: ejecutar en Android 13 (API 33) y Android 15 (API 35) con
 * `gradlew compatibilidadGroupDebugAndroidTest` o en dos emuladores con `connectedDebugAndroidTest`.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AlbumCreateE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

    // Datos falsos para las pruebas negativas: la lista de álbumes y los POST que recibió el servidor
    private val albums = listOf(
        albumJson(100, "Buscando América", "Salsa", "Elektra"),
        albumJson(101, "A Night at the Opera", "Rock", "EMI")
    )
    private val postedBodies = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        // 1. Servidor falso, solo lo usan las pruebas negativas (cada una define sus respuestas)
        server = MockWebServer()
        server.start()

        // 2. Guardar la URL del backend real; las pruebas positivas la usan tal cual
        originalBaseUrl = NetworkServiceAdapter.BASE_URL

        // 3. Cola de Volley vigilada por el IdlingResource
        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        // Dispatchers.Main: el parseo pasa por la cola del hilo principal, que Espresso sí vigila.
        // Con Dispatchers.Default el IdlingResource quedaría libre antes de que llegue el dato a la UI.
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue, Dispatchers.Main)
        IdlingRegistry.getInstance().register(idlingResource)

        // La app se abre dentro de cada test con launchApp()
    }

    @After
    fun tearDown() {
        scenario?.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        // Se detiene la cola para no dejar sus hilos vivos entre tests
        queue.stop()
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    /** PE-016 (positiva): el botón "Crear álbum" abre el formulario. */
    @Test
    fun pe016_createAlbumButtonOpensForm() {
        // Paso 1: abrir la app contra el backend real y entrar como coleccionista
        launchApp()
        onView(withId(R.id.cardCollector)).perform(scrollTo(), click())

        // Paso 2: tocar "Crear álbum"
        onView(withId(R.id.btnCreateAlbum)).check(matches(isDisplayed())).perform(click())

        // Paso 3: se abre el formulario con su título, sus campos, el botón de guardar y la flecha de regreso
        onView(withText(R.string.album_create_title)).check(matches(isDisplayed()))
        onView(withId(R.id.etCover)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSaveAlbum)).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withId(R.id.btnReturn)).check(matches(isDisplayed()))
    }

    /**
     * PE-017 (positiva): con datos válidos se muestra el éxito y se regresa a la lista, que incluye
     * el álbum nuevo. Cada corrida deja un álbum en el backend real.
     */
    @Test
    fun pe017_validFormShowsSuccessAndReturnsToList() {
        // Nombre único para reconocer el álbum de esta corrida entre los que ya tiene el catálogo
        val albumName = "Álbum E2E ${System.currentTimeMillis()}"

        // Paso 1: abrir la app contra el backend real y abrir el formulario como coleccionista
        openFormAsCollector()

        // Paso 2: llenar el formulario con datos válidos y guardar
        fillValidForm(albumName)
        saveAlbum()

        // Paso 3: se muestra "Álbum creado exitosamente"
        onView(withText(R.string.album_create_success_title)).check(matches(isDisplayed()))

        // Paso 4: se regresa a la lista, que ya incluye el álbum nuevo
        clickInformationButton(R.id.successInformationWidget)
        onView(withId(R.id.albumsRv)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(albumName)))
        )
        onView(withText(albumName)).check(matches(isDisplayed()))
    }

    /**
     * PE-018 (positiva): salir del formulario regresa a la lista sin guardar. El formulario no tiene
     * botón "Cancelar": se sale con la flecha de regreso del encabezado.
     */
    @Test
    fun pe018_returnArrowGoesBackToListWithoutSaving() {
        // Nombre único: si el álbum se guardara por error, aparecería en la lista
        val albumName = "Álbum E2E ${System.currentTimeMillis()}"

        // Paso 1: abrir la app contra el backend real y abrir el formulario como coleccionista
        openFormAsCollector()

        // Paso 2: escribir un nombre y salir con la flecha de regreso, sin guardar
        typeText(R.id.etName, albumName)
        onView(withId(R.id.btnReturn)).perform(click())

        // Paso 3: se vuelve a la lista y el álbum no está
        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withText(albumName)).check(doesNotExist())
    }

    /** PE-019 (negativa): guardar con campos vacíos muestra "Este campo es obligatorio". */
    @Test
    fun pe019_savingEmptyFormShowsRequiredErrors() {
        givenBackend()
        openFormAsCollector(useMockServer = true)

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
        openFormAsCollector(useMockServer = true)

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
        openFormAsCollector(useMockServer = true)

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
        openFormAsCollector(useMockServer = true)

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
        openFormAsCollector(useMockServer = true)

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
        launchApp(useMockServer = true)

        onView(withId(R.id.cardVisitor)).perform(scrollTo(), click())

        onView(withId(R.id.albumsRv)).check(matches(isDisplayed()))
        onView(withId(R.id.btnCreateAlbum))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    /**
     * Abre la app. Con [useMockServer] la apunta antes al servidor falso; si no, usa el backend real.
     */
    private fun launchApp(useMockServer: Boolean = false) {
        if (useMockServer) {
            NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Abre la app, entra como coleccionista y toca "Crear álbum". */
    private fun openFormAsCollector(useMockServer: Boolean = false) {
        launchApp(useMockServer)
        onView(withId(R.id.cardCollector)).perform(scrollTo(), click())
        onView(withId(R.id.btnCreateAlbum)).perform(click())
        onView(withId(R.id.etCover)).check(matches(isDisplayed()))
    }

    /** Llena todos los campos con datos válidos; la fecha es la de hoy. */
    private fun fillValidForm(name: String = ALBUM_NAME) {
        typeText(R.id.etCover, "https://example.com/portada.jpg")
        typeText(R.id.etName, name)
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
        scenario!!.onActivity { activity ->
            assertEquals(expected, activity.findViewById<TextInputLayout>(layoutId).error?.toString())
        }
    }

    /**
     * Servidor falso para las pruebas negativas: GET /albums devuelve la lista y cada POST se guarda
     * en [postedBodies]. [postResponse] es la respuesta al crear (por defecto 404: no se espera POST).
     */
    private fun givenBackend(postResponse: MockResponse = MockResponse().setResponseCode(404)) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path != "/albums" -> MockResponse().setResponseCode(404)
                request.method == "POST" -> {
                    postedBodies.add(request.body.readUtf8())
                    postResponse
                }
                else -> jsonResponse(albums.joinToString(",", "[", "]"))
            }
        }
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

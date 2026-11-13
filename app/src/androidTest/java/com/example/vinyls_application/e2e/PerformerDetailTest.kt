package com.example.vinyls_application.e2e

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withParentIndex
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.R
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.models.Band
import com.example.vinyls_application.models.Musician
import com.example.vinyls_application.models.PerformerBase
import com.example.vinyls_application.models.PerformerPrize
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.MainActivity
import com.example.vinyls_application.utils.VolleyIdlingResource
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.allOf
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * Pruebas E2E de HU04 - Detalle de un artista (banda o músico).
 *
 * PE-011, PE-012 y PE-013 usan el backend real (NetworkServiceAdapter.BASE_URL) y comparan la
 * pantalla con lo que el backend devuelve para ese artista. PE-014 (artista sin álbumes) y
 * PE-015 (error al cargar el detalle) usan MockWebServer.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PerformerDetailTest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var queue: RequestQueue
    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var server: MockWebServer
    private lateinit var originalBaseUrl: String

    // Datos falsos para pe014 y pe015: un álbum para que el catálogo inicial tenga una tarjeta,
    // una banda sin álbumes y un músico cuyo detalle falla
    private val albumsJson = """
        [{"id":100,"name":"Buscando América","cover":"","releaseDate":"1984-08-01T00:00:00.000Z",
          "description":"Primer álbum de Rubén Blades y Seis del Solar.","genre":"Salsa","recordLabel":"Elektra"}]
    """.trimIndent()

    private val bandsJson = """
        [{"id":1,"name":"Queen","image":"",
          "description":"Banda británica de rock","creationDate":"1970-01-01T00:00:00.000Z"}]
    """.trimIndent()

    private val musiciansJson = """
        [{"id":100,"name":"Rubén Blades","image":"",
          "description":"Cantante panameño","birthDate":"1948-07-16T00:00:00.000Z"}]
    """.trimIndent()

    private val bandWithoutAlbumsJson = """
        {"id":1,"name":"Queen","image":"",
         "description":"Banda británica de rock",
         "creationDate":"1970-01-01T00:00:00.000Z",
         "albums":[],
         "performerPrizes":[]}
    """.trimIndent()

    @Before
    fun setUp() {
        // 1. Servidor falso, solo lo usan pe014 y pe015: Queen no tiene álbumes y el detalle
        //    de Rubén Blades responde 500
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/albums" -> jsonResponse(albumsJson)
                    "/bands" -> jsonResponse(bandsJson)
                    "/musicians" -> jsonResponse(musiciansJson)
                    "/bands/1" -> jsonResponse(bandWithoutAlbumsJson)
                    "/musicians/100" -> MockResponse().setResponseCode(500)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        // 2. Guardar la URL del backend real; pe011, pe012 y pe013 la usan tal cual
        //    (pe014 y pe015 la cambian al servidor falso en launchApp)
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

        // La app se abre dentro de cada test con launchApp(), así pe014 y pe015 la abren una sola vez
    }

    @After
    fun tearDown() {
        scenario.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        // Se detiene la cola para no dejar sus hilos vivos entre tests
        queue.stop()
        NetworkServiceAdapter.instance = null
        NetworkServiceAdapter.BASE_URL = originalBaseUrl
        server.shutdown()
    }

    @Test
    fun pe011_musicianDetailShowsInformationAndReturnButtonNavigatesToList() {
        // Paso 1: abrir la app, entrar como visitante e ir a Artistas (la lista tiene al menos una tarjeta)
        launchApp()
        openArtistsAsVisitor()

        // Paso 2: obtener del backend real el detalle del primer músico de la lista
        val position = findFirstPerformerPosition(MUSICIAN_LABEL)
        val musician = getPerformerDetailFromBackend(position)

        // Paso 3: abrir el músico por su posición
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))

        // Paso 4: la tarjeta muestra lo mismo que entrega el backend, con el tipo "Músico/a"
        assertDetailCard(musician, expectedTag = "Músico/a")

        // Paso 5: tocar el botón de regresar
        onView(withId(R.id.btnReturn)).perform(click())

        // Paso 6: se vuelve a la lista de artistas
        onView(withId(R.id.performersRv)).check(matches(isDisplayed()))
        onView(withId(R.id.txtPerformersTitle)).check(matches(withText(R.string.title_performers)))
    }

    @Test
    fun pe012_bandDetailShowsBandInformation() {
        // Paso 1: abrir la app, entrar como visitante e ir a Artistas
        launchApp()
        openArtistsAsVisitor()

        // Paso 2: obtener del backend real el detalle de la primera banda de la lista
        val position = findFirstPerformerPosition(BAND_LABEL)
        val band = getPerformerDetailFromBackend(position)

        // Paso 3: abrir la banda por su posición
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))

        // Paso 4: la tarjeta muestra los datos de la banda, con el tipo "Banda"
        assertDetailCard(band, expectedTag = "Banda")
    }

    @Test
    fun pe013_showsAssociatedAlbumsListForPerformer() {
        // Paso 1: abrir la app, entrar como visitante e ir a Artistas
        launchApp()
        openArtistsAsVisitor()

        // Paso 2: obtener del backend real el detalle del primer músico; debe tener álbumes
        val position = findFirstPerformerPosition(MUSICIAN_LABEL)
        val musician = getPerformerDetailFromBackend(position)
        assertTrue(
            "${musician.name} no tiene álbumes en ${NetworkServiceAdapter.BASE_URL}",
            musician.albums.isNotEmpty()
        )

        // Paso 3: abrir el músico por su posición
        onView(withId(R.id.performersRv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, click()))

        // Paso 4: se ve la lista de álbumes y no el mensaje de "sin álbumes"
        onView(withId(R.id.rvRelatedAlbums)).check(matches(isDisplayed()))
        onView(withId(R.id.txtEmptyAlbums)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        // Paso 5: cada álbum con su nombre y fecha de lanzamiento, por su posición
        musician.albums.forEachIndexed { i, album ->
            onView(allOf(withParent(withId(R.id.rvRelatedAlbums)), withParentIndex(i)))
                .perform(scrollTo())
                .check(
                    matches(
                        allOf(
                            hasDescendant(allOf(withId(R.id.summaryCardTitle), withText(album.name))),
                            hasDescendant(allOf(withId(R.id.summaryCardSubtitle), withText(album.releaseDate.take(10))))
                        )
                    )
                )
        }
    }

    @Test
    fun pe014_performerWithoutAlbumsShowsEmptyAlbumsState() {
        // Paso 1: abrir la app apuntando al servidor falso (Queen sin álbumes)
        launchApp(useMockServer = true)
        openArtistsAsVisitor()

        // Paso 2: abrir la banda
        openPerformerByName("Queen")

        // Paso 3: se muestra "Sin álbumes disponibles" y no la lista
        onView(withText(R.string.performer_no_albums)).check(matches(isDisplayed()))
        onView(withId(R.id.rvRelatedAlbums)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(allOf(withId(R.id.subtitle), isDescendantOfA(withId(R.id.detailCard))))
            .check(matches(withText("• 0 Álbumes")))
    }

    @Test
    fun pe015_loadingFailureShowsErrorMessage() {
        // Paso 1: abrir la app apuntando al servidor falso (detalle de Rubén Blades con 500)
        launchApp(useMockServer = true)
        openArtistsAsVisitor()

        // Paso 2: abrir el músico; el detalle falla
        openPerformerByName("Rubén Blades")

        // Paso 3: se muestra la pantalla de error con el mensaje exacto
        onView(withId(R.id.layoutError)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.errorInformationWidget))))
            .check(matches(withText("¡Ups! Algo se\ndesafinó en la señal")))
        onView(allOf(withId(R.id.description), isDescendantOfA(withId(R.id.errorInformationWidget))))
            .check(matches(withText("Algo inesperado ha ocurrido.\nIntenta más tarde.")))

        // Paso 4: tocar el botón de regresar
        onView(withId(R.id.btnReturn)).perform(click())

        // Paso 5: se vuelve a la lista
        onView(withId(R.id.performersRv)).check(matches(isDisplayed()))
        onView(withText("Rubén Blades")).check(matches(isDisplayed()))
    }

    /** Entra como visitante y toca "Artistas" en la barra inferior. */
    private fun openArtistsAsVisitor() {
        onView(withId(R.id.cardVisitor)).check(matches(isDisplayed()))
        onView(withId(R.id.cardVisitor)).perform(click())
        // Esperar a que el catálogo de álbumes esté en pantalla con al menos una tarjeta
        onView(withId(R.id.albumsRv)).check(matches(hasMinimumChildCount(1)))
        onView(withId(R.id.navigation_artists)).perform(click())
        onView(withId(R.id.performersRv)).check(matches(hasMinimumChildCount(1)))
    }

    /**
     * Abre la app, como launchApp() de AlbumDetailE2ETest. Con [useMockServer] la apunta antes al
     * servidor falso, así los tests con mock la abren una sola vez.
     */
    private fun launchApp(useMockServer: Boolean = false) {
        if (useMockServer) {
            NetworkServiceAdapter.BASE_URL = server.url("/").toString()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** Solo para los tests con servidor falso, que entran por texto como pe006 de AlbumDetailE2ETest. */
    private fun openPerformerByName(name: String) {
        onView(withId(R.id.performersRv)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(allOf(withId(R.id.txtTitle), withText(name))), click()
            )
        )
    }

    /** Verifica la tarjeta de detalle contra los datos del backend. */
    private fun assertDetailCard(performer: PerformerBase, expectedTag: String) {
        onView(allOf(withId(R.id.title), isDescendantOfA(withId(R.id.detailCard))))
            .check(matches(withText(performer.name)))
        onView(allOf(withId(R.id.tag), isDescendantOfA(withId(R.id.detailCard))))
            .check(matches(withText(expectedTag)))
        onView(allOf(withId(R.id.subtag), isDescendantOfA(withId(R.id.detailCard))))
            .check(matches(withText("${performer.performerPrizes.size} Premios")))
        onView(allOf(withId(R.id.subtitle), isDescendantOfA(withId(R.id.detailCard))))
            .check(matches(withText("• ${performer.albums.size} Álbumes")))
        onView(allOf(withId(R.id.description), isDescendantOfA(withId(R.id.detailCard))))
            .check(matches(withText(performer.description)))
        // La portada solo se muestra si el artista tiene imagen
        if (performer.image.isNotBlank()) {
            onView(allOf(withId(R.id.cover), isDescendantOfA(withId(R.id.detailCard))))
                .check(matches(isDisplayed()))
        }
    }

    /**
     * Posición en la lista del primer artista del tipo indicado ("Banda" o "Músico").
     * La lista viene ordenada por nombre, así que bandas y músicos están mezclados.
     */
    private fun findFirstPerformerPosition(typeLabel: String): Int {
        // 1. Desplazar la lista hasta el primer artista de ese tipo para que su tarjeta exista
        onView(withId(R.id.performersRv)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                hasDescendant(allOf(withId(R.id.txtSubtitle), withText(typeLabel)))
            ).atPosition(0)
        )

        // 2. Recorrer las posiciones hasta la primera tarjeta con ese tipo
        var position = -1
        scenario.onActivity { activity ->
            val recyclerView = activity.findViewById<RecyclerView>(R.id.performersRv)
            position = (0 until recyclerView.adapter!!.itemCount).first { pos ->
                recyclerView.findViewHolderForAdapterPosition(pos)?.itemView
                    ?.findViewById<TextView>(R.id.txtSubtitle)?.text?.toString() == typeLabel
            }
        }
        return position
    }

    /**
     * Igual que getFirstAlbumDetailFromBackend de AlbumDetailE2ETest: lee el id desde el tag
     * de la tarjeta (por defecto la primera) y pide el artista al backend real. El tipo
     * ("Banda" o "Músico") se lee de la misma tarjeta para saber qué endpoint pedir.
     */
    private fun getPerformerDetailFromBackend(position: Int = 0): PerformerBase {
        // 1. Leer el id y el tipo del artista desde su tarjeta
        var performerId = 0
        var typeLabel = ""
        scenario.onActivity { activity ->
            val recyclerView = activity.findViewById<RecyclerView>(R.id.performersRv)
            val card = recyclerView.findViewHolderForAdapterPosition(position)!!.itemView
            performerId = card.getTag(R.id.tag_album_id) as Int
            typeLabel = card.findViewById<TextView>(R.id.txtSubtitle).text.toString()
        }

        // 2. Pedir el artista al backend real
        val isBand = typeLabel == BAND_LABEL
        val path = if (isBand) "/bands/" else "/musicians/"
        val url = URL(NetworkServiceAdapter.BASE_URL.trimEnd('/') + path + performerId)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        val json = try {
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }

        // 3. Convertir la respuesta al modelo Band o Musician
        val albumsJson = json.getJSONArray("albums")
        val albums = (0 until albumsJson.length()).map { i ->
            val a = albumsJson.getJSONObject(i)
            Album(
                albumId = a.getInt("id"), name = a.getString("name"), cover = a.getString("cover"),
                releaseDate = a.getString("releaseDate"), description = a.getString("description"),
                genre = a.getString("genre"), recordLabel = a.getString("recordLabel")
            )
        }
        val prizesJson = json.getJSONArray("performerPrizes")
        val prizes = (0 until prizesJson.length()).map { i ->
            val p = prizesJson.getJSONObject(i)
            PerformerPrize(id = p.getInt("id"), premiationDate = p.getString("premiationDate"))
        }
        return if (isBand) {
            Band(
                id = json.getInt("id"), name = json.getString("name"), image = json.getString("image"),
                description = json.getString("description"), albums = albums, performerPrizes = prizes,
                creationDate = json.getString("creationDate")
            )
        } else {
            Musician(
                id = json.getInt("id"), name = json.getString("name"), image = json.getString("image"),
                description = json.getString("description"), albums = albums, performerPrizes = prizes,
                birthDate = json.getString("birthDate")
            )
        }
    }

    /**
     * Respuesta 200 con JSON. Sin `charset=utf-8` Volley decodifica en ISO-8859-1 y los
     * acentos llegan mal ("Rubén" se vería "RubÃ©n").
     */
    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json; charset=utf-8")
        .setBody(body)

    private companion object {
        // Textos de tipo en las tarjetas de la lista (R.string.performer_type_*)
        const val BAND_LABEL = "Banda"
        const val MUSICIAN_LABEL = "Músico"
    }
}

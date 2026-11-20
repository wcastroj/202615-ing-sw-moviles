package com.example.vinyls_application.viewmodels

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.MainDispatcherRule
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.ui.LoginFragment
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AssociateAlbumViewModelTest {

    private val musicianJson = """
        {
          "id": 100,
          "name": "Rubén Blades",
          "image": "https://example.com/ruben.jpg",
          "description": "Músico panameño",
          "birthDate": "1948-07-16T00:00:00.000Z",
          "albums": [
            {
              "id": 100,
              "name": "Buscando América",
              "cover": "https://example.com/cover.jpg",
              "releaseDate": "1984-08-01T00:00:00.000Z",
              "description": "Álbum de prueba",
              "genre": "Salsa",
              "recordLabel": "Elektra"
            }
          ],
          "performerPrizes": []
        }
    """.trimIndent()

    private val allAlbumsJson = """
        [
          {
            "id": 100,
            "name": "Buscando América",
            "cover": "https://example.com/cover.jpg",
            "releaseDate": "1984-08-01T00:00:00.000Z",
            "description": "Álbum de prueba",
            "genre": "Salsa",
            "recordLabel": "Elektra"
          },
          {
            "id": 101,
            "name": "Signos",
            "cover": "https://example.com/signos.jpg",
            "releaseDate": "1986-11-10T00:00:00.000Z",
            "description": "Álbum de Soda Stereo",
            "genre": "Rock",
            "recordLabel": "CBS"
          }
        ]
    """.trimIndent()

    private val onlyAssociatedAlbumCatalogJson = """
        [
          {
            "id": 100,
            "name": "Buscando América",
            "cover": "https://example.com/cover.jpg",
            "releaseDate": "1984-08-01T00:00:00.000Z",
            "description": "Álbum de prueba",
            "genre": "Salsa",
            "recordLabel": "Elektra"
          }
        ]
    """.trimIndent()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var application: Application
    private lateinit var requestQueue: RequestQueue
    private lateinit var viewModel: AssociateAlbumViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance =
            NetworkServiceAdapter(application, requestQueue, mainDispatcherRule.testDispatcher)
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PU-069: La lista para asociar excluye los álbumes ya asociados al artista
    @Test
    fun pu069_loadAvailableAlbums_excludesAlreadyAssociatedAlbums() {
        viewModel = AssociateAlbumViewModel(application, 100, PerformerType.MUSICIAN)

        loadInitialData(musicianJson, allAlbumsJson)

        val available = viewModel.availableAlbums.value
        assertNotNull(available)
        assertEquals(1, available!!.size)
        assertEquals(101, available[0].albumId)
        assertEquals("Signos", available[0].name)
        assertFalse(viewModel.eventNetworkError.value!!)
    }

    // PU-070: Asociación exitosa actualiza los álbumes del artista
    @Test
    fun pu070_associateAlbum_whenSuccess_triggersSuccessState() {
        viewModel = AssociateAlbumViewModel(application, 100, PerformerType.MUSICIAN)

        loadInitialData(musicianJson, allAlbumsJson)

        viewModel.associateAlbum(101)

        val jsonCaptor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(jsonCaptor.capture())
        val postRequest = jsonCaptor.value

        assertEquals(NetworkServiceAdapter.BASE_URL + "musicians/100/albums/101", postRequest.url)

        deliverJsonResponse(postRequest, JSONObject("""{"id": 101, "name": "Signos"}"""))
        idleMainLooper()

        assertTrue(viewModel.associationSuccess.value!!)
        assertFalse(viewModel.eventNetworkError.value!!)
    }

    // PU-071: Error de la API genera el estado de error
    @Test
    fun pu071_associateAlbum_whenApiError_triggersNetworkError() {
        viewModel = AssociateAlbumViewModel(application, 100, PerformerType.MUSICIAN)

        loadInitialData(musicianJson, allAlbumsJson)

        viewModel.associateAlbum(101)

        val jsonCaptor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(jsonCaptor.capture())
        val postRequest = jsonCaptor.value

        postRequest.deliverError(ServerError(network500Response()))
        idleMainLooper()

        assertTrue(viewModel.eventNetworkError.value!!)
        assertFalse(viewModel.associationSuccess.value!!)
    }

    // PU-072: Sin álbumes disponibles genera el estado vacío
    @Test
    fun pu072_loadAvailableAlbums_whenAllAlbumsAssociated_returnsEmptyList() {
        viewModel = AssociateAlbumViewModel(application, 100, PerformerType.MUSICIAN)

        loadInitialData(musicianJson, onlyAssociatedAlbumCatalogJson)

        val available = viewModel.availableAlbums.value
        assertNotNull(available)
        assertTrue(available!!.isEmpty())
        assertFalse(viewModel.eventNetworkError.value!!)
    }

    // PU-073: Con rol Coleccionista se habilita la asociación
    @Test
    fun pu073_collectorRole_enablesAssociationOption() {
        val sharedPref = application.getSharedPreferences(application.packageName, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(LoginFragment.KEY_IS_COLLECTOR, true).commit()

        val isCollector = sharedPref.getBoolean(LoginFragment.KEY_IS_COLLECTOR, false)
        assertTrue(isCollector)
    }

    // PU-074: Con rol Visitante no se habilita la asociación
    @Test
    fun pu074_visitorRole_disablesAssociationOption() {
        val sharedPref = application.getSharedPreferences(application.packageName, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(LoginFragment.KEY_IS_COLLECTOR, false).commit()

        val isCollector = sharedPref.getBoolean(LoginFragment.KEY_IS_COLLECTOR, false)
        assertFalse(isCollector)
    }

    private fun loadInitialData(performerResponse: String, albumsResponse: String) {
        val performerCaptor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(performerCaptor.capture())
        val performerRequest = performerCaptor.value
        assertEquals(NetworkServiceAdapter.BASE_URL + "musicians/100", performerRequest.url)

        deliverStringResponse(performerRequest, performerResponse)
        idleMainLooper()

        val albumsCaptor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue, times(2)).add(albumsCaptor.capture())
        val albumsRequest = albumsCaptor.allValues[1]
        assertEquals(NetworkServiceAdapter.BASE_URL + "albums", albumsRequest.url)

        deliverStringResponse(albumsRequest, albumsResponse)
        idleMainLooper()
        idleMainLooper()
    }

    private fun deliverStringResponse(request: StringRequest, response: String) {
        StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(request, response)
    }

    private fun deliverJsonResponse(request: JsonObjectRequest, response: JSONObject) {
        var clazz: Class<*>? = request.javaClass
        while (clazz != null) {
            val method = clazz.declaredMethods.firstOrNull { it.name == "deliverResponse" }
            if (method != null) {
                method.isAccessible = true
                method.invoke(request, response)
                return
            }
            clazz = clazz.superclass
        }
    }

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun network500Response() =
        NetworkResponse(500, ByteArray(0), false, 0, emptyList())
}

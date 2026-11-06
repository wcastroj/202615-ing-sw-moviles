package com.example.vinyls_application.viewmodels

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.repositories.PerformerRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Prueba el ViewModel con el repositorio y el adapter reales, simulando solo la cola de
 * Volley: así se verifica el flujo completo bandas → músicos → lista unida.
 */
@RunWith(RobolectricTestRunner::class)
class PerformerViewModelTest {

    private val bandsJson = """
        [
          {
            "id": 1,
            "name": "Queen",
            "image": "https://example.com/queen.jpg",
            "description": "Banda británica de rock",
            "creationDate": "1970-01-01T00:00:00.000Z"
          }
        ]
    """.trimIndent()

    private val musiciansJson = """
        [
          {
            "id": 100,
            "name": "Rubén Blades Bellido de Luna",
            "image": "https://example.com/ruben.jpg",
            "description": "Cantante, compositor y actor panameño",
            "birthDate": "1948-07-16T00:00:00.000Z"
          },
          {
            "id": 101,
            "name": "Celia Cruz",
            "image": "https://example.com/celia.jpg",
            "description": "Reina de la salsa",
            "birthDate": "1925-10-21T00:00:00.000Z"
          }
        ]
    """.trimIndent()

    private lateinit var requestQueue: RequestQueue
    private lateinit var viewModel: PerformerViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue)
        // Al crearse, el ViewModel pide las bandas
        viewModel = PerformerViewModel(application, PerformerRepository(application))
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PU-013: Bandas y músicos se unen en una sola lista (positiva).
    @Test
    fun pu013_bandas_y_musicos_se_unen_en_una_sola_lista() {
        deliverResponse(bandsRequest(), bandsJson)
        deliverResponse(musiciansRequest(), musiciansJson)
        idleMainLooper()

        val performers = viewModel.performers.value
        assertNotNull(performers)
        assertEquals(3, performers!!.size)
        assertEquals(setOf(1, 100, 101), performers.map { it.id }.toSet())
        // La lista se muestra ordenada por nombre
        assertEquals(
            listOf("Celia Cruz", "Queen", "Rubén Blades Bellido de Luna"),
            performers.map { it.name }
        )
        assertFalse(viewModel.eventNetworkError.value!!)
    }

    // PU-014: En la lista unida cada artista conserva su tipo Band o Musician (positiva).
    @Test
    fun pu014_lista_unida_conserva_el_tipo_de_cada_artista() {
        deliverResponse(bandsRequest(), bandsJson)
        deliverResponse(musiciansRequest(), musiciansJson)
        idleMainLooper()

        val byId = viewModel.performers.value!!.associateBy { it.id }
        assertEquals(PerformerType.BAND, byId.getValue(1).type)
        assertEquals(PerformerType.MUSICIAN, byId.getValue(100).type)
        assertEquals(PerformerType.MUSICIAN, byId.getValue(101).type)
    }

    // PU-016: Lista vacía genera el estado "No se encontraron artistas" (negativa).
    @Test
    fun pu016_lista_vacia_genera_estado_sin_artistas() {
        deliverResponse(bandsRequest(), "[]")
        deliverResponse(musiciansRequest(), "[]")
        idleMainLooper()

        // El fragment muestra layoutEmpty cuando recibe una lista vacía sin error
        val performers = viewModel.performers.value
        assertNotNull(performers)
        assertTrue(performers!!.isEmpty())
        assertFalse(viewModel.eventNetworkError.value!!)
    }

    // PU-017: Error de la API al pedir bandas genera el estado de error (negativa).
    @Test
    fun pu017_error_api_en_bandas_genera_estado_de_error() {
        bandsRequest().deliverError(ServerError(network500Response()))
        idleMainLooper()

        assertTrue(viewModel.eventNetworkError.value!!)
        assertNull(viewModel.performers.value)
    }

    // PU-017: Error de la API al pedir músicos genera el estado de error (negativa).
    @Test
    fun pu017_error_api_en_musicos_genera_estado_de_error() {
        deliverResponse(bandsRequest(), bandsJson)
        musiciansRequest().deliverError(ServerError(network500Response()))
        idleMainLooper()

        assertTrue(viewModel.eventNetworkError.value!!)
        // No se publica una lista a medias solo con las bandas
        assertNull(viewModel.performers.value)
    }

    @Test
    fun onNetworkErrorShown_marcaElErrorComoMostrado() {
        bandsRequest().deliverError(ServerError(network500Response()))

        viewModel.onNetworkErrorShown()

        assertTrue(viewModel.isNetworkErrorShown.value!!)
    }

    /** Primera petición encolada: GET /bands (se hace al crear el ViewModel). */
    private fun bandsRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(captor.capture())
        assertEquals(NetworkServiceAdapter.BASE_URL + "bands", captor.value.url)
        return captor.value
    }

    /** Segunda petición encolada: GET /musicians (solo se hace cuando llegan las bandas). */
    private fun musiciansRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue, times(2)).add(captor.capture())
        val request = captor.allValues[1]
        assertEquals(NetworkServiceAdapter.BASE_URL + "musicians", request.url)
        return request
    }

    private fun deliverResponse(request: StringRequest, response: String) {
        StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(request, response)
    }

    /** `postValue` publica en el hilo principal; con Robolectric hay que procesar su cola. */
    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun network500Response() =
        NetworkResponse(500, ByteArray(0), false, 0, emptyList())
}

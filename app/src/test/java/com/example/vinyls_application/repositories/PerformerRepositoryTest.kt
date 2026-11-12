package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/**
 * Pruebas de integración de HU03: PerformerRepository + NetworkServiceAdapter + parser.
 * Solo se simula la cola de Volley: se capturan las peticiones y se les entrega la respuesta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformerRepositoryTest {

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

    // Corre las corrutinas de inmediato en el hilo de la prueba (ver MainDispatcherRule)
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: PerformerRepository
    private var result: List<Performer>? = null
    private var error: VolleyError? = null

    @Before
    fun setUp() {
        val application = mock(Application::class.java)
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue, testDispatcher)
        repository = PerformerRepository(application)
        result = null
        error = null
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PI-007: GET /bands y /musicians responden 200 y el repositorio entrega una sola lista
    @Test
    fun pi007_bands_y_musicians_responden_200_y_el_repositorio_entrega_una_sola_lista() = runTest(testDispatcher) {
        // Arrange
        loadPerformers()
        val bandsRequest = bandsRequest()

        // Act
        deliverResponse(bandsRequest, bandsJson)
        val musiciansRequest = musiciansRequest()
        deliverResponse(musiciansRequest, musiciansJson)

        // Assert: las dos peticiones son GET a su endpoint
        assertEquals(Request.Method.GET, bandsRequest.method)
        assertEquals(Request.Method.GET, musiciansRequest.method)

        // Assert: una sola lista con la banda y los dos músicos, ordenada por nombre
        assertNotNull(result)
        assertEquals(3, result?.size)
        assertEquals(
            listOf("Celia Cruz", "Queen", "Rubén Blades Bellido de Luna"),
            result?.map { it.name }
        )

        // Assert: cada artista conserva su tipo y su fecha
        val byId = result!!.associateBy { it.id }
        assertEquals(PerformerType.BAND, byId.getValue(1).type)
        assertEquals("1970-01-01T00:00:00.000Z", byId.getValue(1).creationDate)
        assertEquals(PerformerType.MUSICIAN, byId.getValue(100).type)
        assertEquals("1948-07-16T00:00:00.000Z", byId.getValue(100).birthDate)
        assertEquals(PerformerType.MUSICIAN, byId.getValue(101).type)
        assertNull(error)
    }

    // PI-008: GET /bands y /musicians responden con listas vacías
    @Test
    fun pi008_bands_y_musicians_responden_listas_vacias_y_el_repositorio_entrega_lista_vacia() = runTest(testDispatcher) {
        // Arrange
        loadPerformers()

        // Act
        deliverResponse(bandsRequest(), "[]")
        deliverResponse(musiciansRequest(), "[]")

        // Assert
        assertNotNull(result)
        assertTrue(result?.isEmpty() == true)
        assertNull(error)
    }

    // PI-009: GET /bands y /musicians responden 500 o no hay red (caso 500 en /bands)
    @Test
    fun pi009_bands_responde_500_y_el_repositorio_entrega_error() = runTest(testDispatcher) {
        // Arrange
        loadPerformers()

        // Act
        bandsRequest().deliverError(ServerError(networkResponse(500)))

        // Assert: falla y ni siquiera se piden los músicos
        assertTrue(error is ServerError)
        assertEquals(500, error?.networkResponse?.statusCode)
        assertNull(result)
        verify(requestQueue, times(1)).add(any(StringRequest::class.java))
    }

    // PI-009: GET /bands y /musicians responden 500 o no hay red (caso 500 en /musicians)
    @Test
    fun pi009_musicians_responde_500_y_el_repositorio_entrega_error() = runTest(testDispatcher) {
        // Arrange
        loadPerformers()
        deliverResponse(bandsRequest(), bandsJson)

        // Act
        musiciansRequest().deliverError(ServerError(networkResponse(500)))

        // Assert: no se entrega una lista a medias solo con las bandas
        assertTrue(error is ServerError)
        assertEquals(500, error?.networkResponse?.statusCode)
        assertNull(result)
    }

    // PI-009: GET /bands y /musicians responden 500 o no hay red (caso sin red en /bands)
    @Test
    fun pi009_sin_red_en_bands_el_repositorio_entrega_error() = runTest(testDispatcher) {
        // Arrange
        loadPerformers()

        // Act
        bandsRequest().deliverError(NoConnectionError())

        // Assert
        assertTrue(error is NoConnectionError)
        assertNull(result)
    }

    // PI-009: GET /bands y /musicians responden 500 o no hay red (caso sin red en /musicians)
    @Test
    fun pi009_sin_red_en_musicians_el_repositorio_entrega_error() = runTest(testDispatcher) {
        // Arrange
        loadPerformers()
        deliverResponse(bandsRequest(), bandsJson)

        // Act
        musiciansRequest().deliverError(NoConnectionError())

        // Assert
        assertTrue(error is NoConnectionError)
        assertNull(result)
    }

    /**
     * Lanza getPerformers() en una corrutina. Queda suspendida esperando /bands; cuando la prueba
     * entrega esa respuesta se reanuda y encola /musicians.
     */
    private fun CoroutineScope.loadPerformers() = launch {
        try {
            result = repository.getPerformers()
        } catch (e: VolleyError) {
            error = e
        }
    }

    /** Primera petición encolada: debe ser GET /bands. */
    private fun bandsRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(captor.capture())
        assertEquals(NetworkServiceAdapter.BASE_URL + "bands", captor.value.url)
        return captor.value
    }

    /** Segunda petición encolada (solo existe si /bands respondió bien): debe ser GET /musicians. */
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

    private fun networkResponse(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), false, 0, emptyList())
}

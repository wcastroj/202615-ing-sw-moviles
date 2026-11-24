package com.example.vinyls_application.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.example.vinyls_application.MainDispatcherRule
import com.example.vinyls_application.models.Track
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.repositories.TrackRepository
import com.example.vinyls_application.utils.TrackFieldError
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.robolectric.RobolectricTestRunner

/**
 * Pruebas unitarias de HU06: asociar una canción a un álbum. Se simula solo la cola de Volley,
 * igual que en AlbumCreateViewModelTest.
 */
@RunWith(RobolectricTestRunner::class)
class AssociateTrackViewModelTest {

    private val validTrack = Track(name = "Don't Stop Me Now", duration = "3:45")

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var requestQueue: RequestQueue
    private lateinit var viewModel: AssociateTrackViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance =
            NetworkServiceAdapter(application, requestQueue, mainDispatcherRule.testDispatcher)
        viewModel = AssociateTrackViewModel(application, ALBUM_ID, TrackRepository(application))
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PU-046: Nombre del track válido (positiva).
    @Test
    fun pu046_nombre_del_track_valido() {
        viewModel.onNameChanged("Don't Stop Me Now")

        assertEquals(TrackFieldError.NONE, viewModel.nameError.value)
    }

    // PU-047: Nombre del track vacío (negativa).
    @Test
    fun pu047_nombre_del_track_vacio() {
        viewModel.onNameChanged("")

        assertEquals(TrackFieldError.REQUIRED, viewModel.nameError.value)
    }

    // PU-048: Duración "03:45" (positiva).
    @Test
    fun pu048_duracion_valida() {
        viewModel.onDurationChanged("03:45")
        assertEquals(TrackFieldError.NONE, viewModel.durationError.value)

        viewModel.onDurationChanged("3:45")
        assertEquals(TrackFieldError.NONE, viewModel.durationError.value)
    }

    // PU-049: Duración "00:01" (positiva).
    @Test
    fun pu049_duracion_minima_valida() {
        viewModel.onDurationChanged("00:01")
        assertEquals(TrackFieldError.NONE, viewModel.durationError.value)

        viewModel.onDurationChanged("0:01")
        assertEquals(TrackFieldError.NONE, viewModel.durationError.value)
    }

    // PU-050: Duración vacía (negativa).
    @Test
    fun pu050_duracion_vacia() {
        viewModel.onDurationChanged("")
        assertEquals(TrackFieldError.REQUIRED, viewModel.durationError.value)

        viewModel.onDurationChanged("   ")
        assertEquals(TrackFieldError.REQUIRED, viewModel.durationError.value)
    }

    // PU-051: Duración con formato inválido (negativa).
    @Test
    fun pu051_duracion_con_formato_invalido() {
        val invalidDurations = listOf("345", "3.45", "3:5", "60:00", "3a:45", "3 :45")

        for (duration in invalidDurations) {
            viewModel.onDurationChanged(duration)
            assertEquals(
                "\"$duration\" debería ser inválida",
                TrackFieldError.INVALID_DURATION,
                viewModel.durationError.value
            )
        }
    }

    // PU-052: Duración con segundos fuera de rango (negativa).
    @Test
    fun pu052_duracion_con_segundos_fuera_de_rango() {
        viewModel.onDurationChanged("03:75")
        assertEquals(TrackFieldError.INVALID_DURATION, viewModel.durationError.value)

        viewModel.onDurationChanged("03:60")
        assertEquals(TrackFieldError.INVALID_DURATION, viewModel.durationError.value)
    }

    // PU-053: Formulario con todos los campos válidos permite enviar (positiva).
    @Test
    fun pu053_formulario_valido_permite_enviar() {
        val accepted = viewModel.associate(validTrack)

        assertTrue(accepted)
        assertEquals(AssociateTrackState.SAVING, viewModel.state.value)
        assertEquals(1, posts().size)
    }

    // PU-054: Formulario con un campo inválido bloquea el envío (negativa).
    @Test
    fun pu054_formulario_con_un_campo_invalido_bloquea_el_envio() {
        // Nombre inválido, duración válida
        var accepted = viewModel.associate(validTrack.copy(name = ""))

        assertFalse(accepted)
        assertEquals(AssociateTrackState.EDITING, viewModel.state.value)
        assertEquals(TrackFieldError.REQUIRED, viewModel.nameError.value)
        assertEquals(TrackFieldError.NONE, viewModel.durationError.value)
        assertTrue(posts().isEmpty())

        // Nombre válido, duración inválida
        accepted = viewModel.associate(validTrack.copy(duration = "345"))

        assertFalse(accepted)
        assertEquals(AssociateTrackState.EDITING, viewModel.state.value)
        assertEquals(TrackFieldError.NONE, viewModel.nameError.value)
        assertEquals(TrackFieldError.INVALID_DURATION, viewModel.durationError.value)
        assertTrue(posts().isEmpty())
    }

    // PU-055: Asociación exitosa genera el estado de éxito (positiva).
    @Test
    fun pu055_asociacion_exitosa_genera_estado_de_exito() {
        viewModel.associate(validTrack)

        deliverResponse(posts().single(), JSONObject("""{"id": 2}"""))

        assertEquals(AssociateTrackState.ASSOCIATED, viewModel.state.value)
    }

    // PU-056: Error de la API genera el estado de error (negativa).
    @Test
    fun pu056_error_de_la_api_genera_estado_de_error() {
        viewModel.associate(validTrack)

        posts().single().deliverError(ServerError(networkResponse(500)))

        assertEquals(AssociateTrackState.SAVE_FAILED, viewModel.state.value)
    }

    // PU-057: Nombre del track con solo espacios (negativa).
    @Test
    fun pu057_nombre_del_track_con_solo_espacios() {
        viewModel.onNameChanged("   ")

        assertEquals(TrackFieldError.REQUIRED, viewModel.nameError.value)
    }

    private fun requests(): List<Request<*>> =
        mockingDetails(requestQueue).invocations
            .filter { it.method.name == "add" }
            .map { it.arguments[0] as Request<*> }

    private fun posts() = requests().filter { it.method == Request.Method.POST }

    private fun deliverResponse(request: Request<*>, response: Any) {
        var type: Class<*>? = request.javaClass
        while (type != null) {
            val method = type.declaredMethods.firstOrNull { it.name == "deliverResponse" }
            if (method != null) {
                method.isAccessible = true
                method.invoke(request, response)
                return
            }
            type = type.superclass
        }
    }

    private fun networkResponse(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), false, 0, emptyList())

    private companion object {
        const val ALBUM_ID = 100
    }
}

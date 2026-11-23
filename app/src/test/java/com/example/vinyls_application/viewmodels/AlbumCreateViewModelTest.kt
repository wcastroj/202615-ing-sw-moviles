package com.example.vinyls_application.viewmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.example.vinyls_application.MainDispatcherRule
import com.example.vinyls_application.models.Album
import com.example.vinyls_application.models.Genre
import com.example.vinyls_application.models.RecordLabel
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.repositories.AlbumRepository
import com.example.vinyls_application.utils.AlbumFieldError
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
 * Pruebas unitarias de HU05: envío del formulario y estados del ViewModel. Se simula solo la cola
 * de Volley, igual que en PerformerViewModelTest.
 */
@RunWith(RobolectricTestRunner::class)
class AlbumCreateViewModelTest {

    private val validAlbum = Album(
        name = "Ecos del Atardecer",
        cover = "https://example.com/portada.jpg",
        releaseDate = "2024-05-10T00:00:00.000Z",
        description = "Un álbum de prueba",
        genre = Genre.ROCK.apiValue,
        recordLabel = RecordLabel.EMI.apiValue
    )

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var requestQueue: RequestQueue
    private lateinit var viewModel: AlbumCreateViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance =
            NetworkServiceAdapter(application, requestQueue, mainDispatcherRule.testDispatcher)
        viewModel = AlbumCreateViewModel(application, AlbumRepository(application))
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PU-042: Formulario con todos los campos válidos permite enviar (positiva).
    @Test
    fun pu042_formulario_valido_permite_enviar() {
        val accepted = viewModel.save(validAlbum)

        assertTrue(accepted)
        assertEquals(AlbumCreateState.SAVING, viewModel.state.value)
        assertEquals(1, posts().size)
    }

    // PU-043: Formulario con un campo inválido bloquea el envío (negativa).
    @Test
    fun pu043_formulario_con_un_campo_invalido_bloquea_el_envio() {
        val accepted = viewModel.save(validAlbum.copy(name = ""))

        assertFalse(accepted)
        assertEquals(AlbumCreateState.EDITING, viewModel.state.value)
        assertEquals(AlbumFieldError.REQUIRED, viewModel.errors.value?.name)
        assertEquals(AlbumFieldError.NONE, viewModel.errors.value?.cover)
        assertTrue(posts().isEmpty())
    }

    // PU-044: Creación exitosa genera el estado de éxito (positiva).
    @Test
    fun pu044_creacion_exitosa_genera_estado_de_exito() {
        viewModel.save(validAlbum)

        deliverResponse(posts().single(), JSONObject("""{"id": 200}"""))

        assertEquals(AlbumCreateState.CREATED, viewModel.state.value)
    }

    // PU-045: Error de la API genera el estado de error (negativa).
    @Test
    fun pu045_error_de_la_api_genera_estado_de_error() {
        viewModel.save(validAlbum)

        posts().single().deliverError(ServerError(networkResponse(500)))

        assertEquals(AlbumCreateState.SAVE_FAILED, viewModel.state.value)
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
}

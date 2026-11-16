package com.example.vinyls_application.viewmodels

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.MainDispatcherRule
import com.example.vinyls_application.models.Band
import com.example.vinyls_application.models.Musician
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.repositories.PerformerDetailRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class PerformerDetailViewModelTest {

    private val musicianJson = """
        {
          "id": 100,
          "name": "Rubén Blades Bellido de Luna",
          "image": "https://example.com/ruben.jpg",
          "description": "Cantante, compositor y actor panameño.",
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
          "performerPrizes": [
            {
              "id": 1,
              "premiationDate": "1978-12-10T00:00:00.000Z"
            }
          ]
        }
    """.trimIndent()

    private val bandJson = """
        {
          "id": 101,
          "name": "Queen",
          "image": "https://example.com/queen.jpg",
          "description": "Banda británica de rock.",
          "creationDate": "1970-01-01T00:00:00.000Z",
          "musicians": [],
          "albums": [
            {
              "id": 102,
              "name": "A Night at the Opera",
              "cover": "https://example.com/night.jpg",
              "releaseDate": "1975-11-21T00:00:00.000Z",
              "description": "Cuarto álbum de la banda",
              "genre": "Rock",
              "recordLabel": "EMI"
            }
          ],
          "performerPrizes": [
            {
              "id": 2,
              "premiationDate": "1980-12-10T00:00:00.000Z"
            }
          ]
        }
    """.trimIndent()

    // viewModelScope usa Dispatchers.Main: la regla lo cambia por un TestDispatcher
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var application: Application
    private lateinit var requestQueue: RequestQueue
    private lateinit var viewModel: PerformerDetailViewModel

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

    @Test
    fun getPerformerDetail_whenMusicianSuccess_updatesPerformerState() {
        viewModel = PerformerDetailViewModel(
            application,
            100,
            PerformerType.MUSICIAN,
            PerformerDetailRepository(application)
        )
        val request = capturedRequest()
        assertEquals(NetworkServiceAdapter.BASE_URL + "musicians/100", request.url)

        deliverResponse(request, musicianJson)
        idleMainLooper()

        val performer = viewModel.performer.value
        assertNotNull(performer)
        assertTrue(performer is Musician)
        val musician = performer as Musician
        assertEquals(100, musician.id)
        assertEquals("Rubén Blades Bellido de Luna", musician.name)
        assertFalse(viewModel.eventNetworkError.value!!)
        assertFalse(viewModel.isNetworkErrorShown.value!!)
    }

    @Test
    fun getPerformerDetail_whenBandSuccess_updatesPerformerState() {
        viewModel = PerformerDetailViewModel(
            application,
            101,
            PerformerType.BAND,
            PerformerDetailRepository(application)
        )
        val request = capturedRequest()
        assertEquals(NetworkServiceAdapter.BASE_URL + "bands/101", request.url)

        deliverResponse(request, bandJson)
        idleMainLooper()

        val performer = viewModel.performer.value
        assertNotNull(performer)
        assertTrue(performer is Band)
        val band = performer as Band
        assertEquals(101, band.id)
        assertEquals("Queen", band.name)
        assertFalse(viewModel.eventNetworkError.value!!)
        assertFalse(viewModel.isNetworkErrorShown.value!!)
    }

    @Test
    fun getPerformerDetail_whenApiError_triggersNetworkError() {
        viewModel = PerformerDetailViewModel(
            application,
            100,
            PerformerType.MUSICIAN,
            PerformerDetailRepository(application)
        )
        val request = capturedRequest()
        request.deliverError(ServerError(network500Response()))
        idleMainLooper()

        assertTrue(viewModel.eventNetworkError.value!!)
        assertNull(viewModel.performer.value)
    }

    @Test
    fun onNetworkErrorShown_whenCalled_setsIsNetworkErrorShownToTrue() {
        viewModel = PerformerDetailViewModel(
            application,
            100,
            PerformerType.MUSICIAN,
            PerformerDetailRepository(application)
        )
        val request = capturedRequest()
        request.deliverError(ServerError(network500Response()))
        idleMainLooper()

        viewModel.onNetworkErrorShown()

        assertTrue(viewModel.isNetworkErrorShown.value!!)
    }

    @Test
    fun getPerformerDetail_whenRetry_fetchesDataSuccessfully() {
        viewModel = PerformerDetailViewModel(
            application,
            100,
            PerformerType.MUSICIAN,
            PerformerDetailRepository(application)
        )
        val firstRequest = capturedRequest()
        firstRequest.deliverError(ServerError(network500Response()))
        idleMainLooper()

        assertTrue(viewModel.eventNetworkError.value!!)

        viewModel.getPerformerDetail()
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue, times(2)).add(captor.capture())
        val secondRequest = captor.allValues[1]

        deliverResponse(secondRequest, musicianJson)
        idleMainLooper()

        val performer = viewModel.performer.value
        assertNotNull(performer)
        assertEquals(100, performer?.id)
        assertFalse(viewModel.eventNetworkError.value!!)
    }

    @Test
    fun create_whenValidViewModelClass_returnsPerformerDetailViewModel() {
        val factory = PerformerDetailViewModel.Factory(application, 100, PerformerType.MUSICIAN)
        val createdViewModel = factory.create(PerformerDetailViewModel::class.java)

        assertNotNull(createdViewModel)
        assertEquals(PerformerDetailViewModel::class.java, createdViewModel.javaClass)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_whenUnknownViewModelClass_throwsIllegalArgumentException() {
        class UnknownViewModel : androidx.lifecycle.ViewModel()

        val factory = PerformerDetailViewModel.Factory(application, 100, PerformerType.MUSICIAN)
        factory.create(UnknownViewModel::class.java)
    }

    private fun capturedRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(captor.capture())
        return captor.value
    }

    private fun deliverResponse(request: StringRequest, response: String) {
        StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
            .apply { isAccessible = true }
            .invoke(request, response)
    }

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    private fun network500Response() =
        NetworkResponse(500, ByteArray(0), false, 0, emptyList())
}

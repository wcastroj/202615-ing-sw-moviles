package com.example.vinyls_application.viewmodels

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.android.volley.NetworkResponse
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.MainDispatcherRule
import com.example.vinyls_application.network.NetworkServiceAdapter
import com.example.vinyls_application.repositories.CollectorRepository
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
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CollectorViewModelTest {

    private val collectorsJson = """
        [
          {
            "id": 1,
            "name": "Jaime Andrés Monsalve",
            "telephone": "3102178976",
            "email": "j.monsalve@gmail.com"
          },
          {
            "id": 2,
            "name": "Manolo Bellon",
            "telephone": "3503102456",
            "email": "bellon@manolo.com"
          }
        ]
    """.trimIndent()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var requestQueue: RequestQueue
    private lateinit var viewModel: CollectorViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance =
            NetworkServiceAdapter(application, requestQueue, mainDispatcherRule.testDispatcher)
        viewModel = CollectorViewModel(application, CollectorRepository(application))
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    @Test
    fun collectors_are_loaded_successfully() {
        deliverResponse(collectorsRequest(), collectorsJson)
        idleMainLooper()

        val collectors = viewModel.collectors.value
        assertNotNull(collectors)
        assertEquals(2, collectors!!.size)
        assertEquals("Jaime Andrés Monsalve", collectors[0].name)
        assertEquals("3102178976", collectors[0].telephone)
        assertEquals("j.monsalve@gmail.com", collectors[0].email)
        assertFalse(viewModel.eventNetworkError.value!!)
        assertFalse(viewModel.isNetworkErrorShown.value!!)
    }

    @Test
    fun network_error_sets_eventNetworkError() {
        deliverError(collectorsRequest(), ServerError(network500Response()))
        idleMainLooper()

        assertNull(viewModel.collectors.value)
        assertTrue(viewModel.eventNetworkError.value!!)
        assertFalse(viewModel.isNetworkErrorShown.value!!)

        viewModel.onNetworkErrorShown()
        assertTrue(viewModel.isNetworkErrorShown.value!!)
    }

    @Test
    fun refreshCollectors_triggers_new_request() {
        deliverResponse(collectorsRequest(), collectorsJson)
        idleMainLooper()
        assertEquals(2, viewModel.collectors.value?.size)

        viewModel.refreshCollectors()
        idleMainLooper()

        deliverResponse(collectorsRequest(), "[]")
        idleMainLooper()

        assertEquals(0, viewModel.collectors.value?.size)
    }

    private fun collectorsRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue, org.mockito.Mockito.atLeastOnce()).add(captor.capture())
        return captor.allValues.last()
    }

    private fun deliverResponse(request: StringRequest, body: String) {
        val method = StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
        method.isAccessible = true
        method.invoke(request, body)
    }

    private fun deliverError(request: StringRequest, error: com.android.volley.VolleyError) {
        request.deliverError(error)
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun network500Response() =
        NetworkResponse(500, ByteArray(0), false, 0, emptyList())
}

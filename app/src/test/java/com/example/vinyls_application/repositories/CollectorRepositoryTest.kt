package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.models.Collector
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class CollectorRepositoryTest {

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

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: CollectorRepository
    private var result: List<Collector>? = null
    private var error: VolleyError? = null

    @Before
    fun setUp() {
        val application = mock(Application::class.java)
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue, testDispatcher)
        repository = CollectorRepository(application)
        result = null
        error = null
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    @Test
    fun getCollectors_whenResponseIs200_deliversCollectorList() = runTest(testDispatcher) {
        loadCollectors()
        val request = capturedRequest()

        deliverResponse(request, collectorsJson)

        assertEquals(Request.Method.GET, request.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "collectors", request.url)
        assertNotNull(result)
        assertEquals(2, result?.size)
        assertEquals(1, result?.get(0)?.collectorId)
        assertEquals("Jaime Andrés Monsalve", result?.get(0)?.name)
        assertEquals("3102178976", result?.get(0)?.telephone)
        assertEquals("j.monsalve@gmail.com", result?.get(0)?.email)
        assertEquals(2, result?.get(1)?.collectorId)
        assertEquals("Manolo Bellon", result?.get(1)?.name)
        assertNull(error)
    }

    @Test
    fun getCollectors_whenResponseIs200WithEmptyList_deliversEmptyList() = runTest(testDispatcher) {
        loadCollectors()
        val request = capturedRequest()

        deliverResponse(request, "[]")

        assertNotNull(result)
        assertTrue(result!!.isEmpty())
        assertNull(error)
    }

    @Test
    fun getCollectors_whenResponseIsServerError_deliversError() = runTest(testDispatcher) {
        loadCollectors()
        val request = capturedRequest()
        val serverError = ServerError(network500Response())

        deliverError(request, serverError)

        assertNull(result)
        assertNotNull(error)
        assertTrue(error is ServerError)
    }

    private fun CoroutineScope.loadCollectors() {
        launch {
            try {
                result = repository.getCollectors()
            } catch (e: VolleyError) {
                error = e
            }
        }
    }

    private fun capturedRequest(): StringRequest {
        val captor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(captor.capture())
        return captor.value
    }

    private fun deliverResponse(request: StringRequest, body: String) {
        val method = StringRequest::class.java.getDeclaredMethod("deliverResponse", String::class.java)
        method.isAccessible = true
        method.invoke(request, body)
    }

    private fun deliverError(request: StringRequest, error: VolleyError) {
        request.deliverError(error)
    }

    private fun network500Response() =
        NetworkResponse(500, ByteArray(0), false, 0, emptyList())
}

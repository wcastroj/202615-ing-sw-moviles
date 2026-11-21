package com.example.vinyls_application.repositories

import android.app.Application
import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.example.vinyls_application.models.Performer
import com.example.vinyls_application.models.PerformerType
import com.example.vinyls_application.network.NetworkServiceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class AssociateAlbumIntegrationTest {


    private val musicianWithAssociatedAlbumJson = """
        {
          "id": 100,
          "name": "Rubén Blades",
          "image": "https://example.com/ruben.jpg",
          "description": "Músico panameño",
          "birthDate": "1948-07-16T00:00:00.000Z",
          "albums": [
            {
              "id": 101,
              "name": "Signos",
              "cover": "https://example.com/signos.jpg",
              "releaseDate": "1986-11-10T00:00:00.000Z",
              "description": "Álbum de Soda Stereo",
              "genre": "Rock",
              "recordLabel": "CBS"
            }
          ],
          "performerPrizes": []
        }
    """.trimIndent()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var requestQueue: RequestQueue
    private lateinit var repository: PerformerDetailRepository
    private var result: Performer? = null
    private var error: VolleyError? = null
    private var postSuccess: Boolean = false

    @Before
    fun setUp() {
        val application = mock(Application::class.java)
        requestQueue = mock(RequestQueue::class.java)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(application, requestQueue, testDispatcher)
        repository = PerformerDetailRepository(application)
        result = null
        error = null
        postSuccess = false
    }

    @After
    fun tearDown() {
        NetworkServiceAdapter.instance = null
    }

    // PI-026:
    @Test
    fun pi026_postMusicianAlbum_associatesAlbumToMusician() = runTest(testDispatcher) {
        associateAlbum(100, 101, PerformerType.MUSICIAN)

        val captor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(captor.capture())
        val postRequest = captor.value

        assertEquals(Request.Method.POST, postRequest.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "musicians/100/albums/101", postRequest.url)

        deliverJsonResponse(postRequest, JSONObject("""{"id": 101, "name": "Signos"}"""))

        assertTrue(postSuccess)
        assertNull(error)
    }

    // PI-027:
    @Test
    fun pi027_postBandAlbum_associatesAlbumToBand() = runTest(testDispatcher) {
        associateAlbum(101, 102, PerformerType.BAND)

        val captor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(captor.capture())
        val postRequest = captor.value

        assertEquals(Request.Method.POST, postRequest.method)
        assertEquals(NetworkServiceAdapter.BASE_URL + "bands/101/albums/102", postRequest.url)

        deliverJsonResponse(postRequest, JSONObject("""{"id": 102, "name": "Night at the Opera"}"""))

        assertTrue(postSuccess)
        assertNull(error)
    }

    // PI-028:
    @Test
    fun pi028_associatedAlbum_appearsInSubsequentArtistGet() = runTest(testDispatcher) {
        // 1. POST para asociar álbum
        associateAlbum(100, 101, PerformerType.MUSICIAN)
        val jsonCaptor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(jsonCaptor.capture())
        deliverJsonResponse(jsonCaptor.value, JSONObject("""{"id": 101}"""))

        // 2. GET posterior del detalle del artista
        loadPerformer(100, PerformerType.MUSICIAN)
        val stringCaptor = ArgumentCaptor.forClass(StringRequest::class.java)
        verify(requestQueue).add(stringCaptor.capture())
        val getRequest = stringCaptor.value

        assertEquals(NetworkServiceAdapter.BASE_URL + "musicians/100", getRequest.url)
        deliverStringResponse(getRequest, musicianWithAssociatedAlbumJson)

        assertNotNull(result)
        assertEquals(1, result!!.albums.size)
        assertEquals(101, result!!.albums[0].albumId)
        assertEquals("Signos", result!!.albums[0].name)
    }

    // PI-029:
    @Test
    fun pi029_associateAlbum_returns404_deliversError() = runTest(testDispatcher) {
        associateAlbum(999, 999, PerformerType.MUSICIAN)

        val captor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(captor.capture())
        val postRequest = captor.value

        postRequest.deliverError(ClientError(networkResponse(404)))

        assertTrue(error is ClientError)
        assertEquals(404, error?.networkResponse?.statusCode)
        assertFalse(postSuccess)
    }

    // PI-030:
    @Test
    fun pi030_associateAlbum_returns500OrNoNetwork_deliversError() = runTest(testDispatcher) {
        associateAlbum(100, 101, PerformerType.MUSICIAN)

        val captor = ArgumentCaptor.forClass(JsonObjectRequest::class.java)
        verify(requestQueue).add(captor.capture())
        val postRequest = captor.value

        postRequest.deliverError(ServerError(networkResponse(500)))

        assertTrue(error is ServerError)
        assertEquals(500, error?.networkResponse?.statusCode)
        assertFalse(postSuccess)
    }

    private fun CoroutineScope.associateAlbum(performerId: Int, albumId: Int, type: PerformerType) = launch {
        try {
            repository.addAlbumToPerformer(performerId, albumId, type)
            postSuccess = true
        } catch (e: VolleyError) {
            error = e
        }
    }

    private fun CoroutineScope.loadPerformer(id: Int, type: PerformerType) = launch {
        try {
            result = repository.getPerformer(id, type)
        } catch (e: VolleyError) {
            error = e
        }
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

    private fun networkResponse(statusCode: Int) =
        NetworkResponse(statusCode, ByteArray(0), false, 0, emptyList())
}

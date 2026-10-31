package com.example.vinyls_application

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.vinyls_application.ui.MainActivity
import com.example.vinyls_application.utils.VolleyIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import android.content.Context
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import com.android.volley.toolbox.Volley
import com.example.vinyls_application.network.NetworkServiceAdapter

// los imports los vas agregando con Alt + Enter

@LargeTest
@RunWith(AndroidJUnit4::class)
class AlbumDetailE2ETest {

    private lateinit var idlingResource: VolleyIdlingResource
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        idlingResource = VolleyIdlingResource()
        val queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener(idlingResource)
        NetworkServiceAdapter.instance = NetworkServiceAdapter(context, queue)
        IdlingRegistry.getInstance().register(idlingResource)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        IdlingRegistry.getInstance().unregister(idlingResource)
        IdlingRegistry.getInstance().unregister(idlingResource)
        NetworkServiceAdapter.instance = null
    }
}
package com.example.vinyls_application.utils

import androidx.test.espresso.IdlingResource
import com.android.volley.Request
import com.android.volley.RequestQueue
import java.util.concurrent.atomic.AtomicInteger

class VolleyIdlingResource : IdlingResource , RequestQueue.RequestEventListener {


    private val counter = AtomicInteger(0)
    @Volatile
    private var resourceCallback : IdlingResource.ResourceCallback? = null


    override fun getName(): String? {
        return "VolleyIdlingResource"
    }

    override fun isIdleNow(): Boolean {
        return counter.get() == 0
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }

    override fun onRequestEvent(request: Request<*>?, event: Int) {
        when (event) {
            RequestQueue.RequestEvent.REQUEST_QUEUED -> counter.incrementAndGet()
            RequestQueue.RequestEvent.REQUEST_FINISHED -> { if (counter.decrementAndGet() == 0)
            {resourceCallback?.onTransitionToIdle()}

            }
        }
    }

}
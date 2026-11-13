package com.example.vinyls_application.utils

import android.os.SystemClock
import android.util.Log
import com.android.volley.CacheDispatcher
import com.android.volley.NetworkDispatcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * DIAGNÓSTICO TEMPORAL (rama test/diagnostico-e2e-tiempos): mide tiempos de las E2E.
 * Filtrar con: adb logcat -s E2E-TIMING
 */
object TimingLog {
    const val TAG = "E2E-TIMING"

    inline fun <T> measure(label: String, block: () -> T): T {
        val start = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            Log.d(TAG, "$label ${SystemClock.elapsedRealtime() - start}ms")
        }
    }

    /** Hilos de Volley vivos en el proceso: 5 por cada RequestQueue sin stop(). */
    fun volleyThreads(): Int = Thread.getAllStackTraces().keys.count {
        it.isAlive && (it is NetworkDispatcher || it is CacheDispatcher)
    }
}

/** Registra la duración total de cada test (incluye @Before/@After) y los hilos de Volley al terminar. */
class TimingRule : TestWatcher() {
    private var start = 0L

    override fun starting(description: Description) {
        start = SystemClock.elapsedRealtime()
        Log.d(TimingLog.TAG, "START ${description.className.substringAfterLast('.')}.${description.methodName}")
    }

    override fun finished(description: Description) {
        Log.d(
            TimingLog.TAG,
            "END ${description.className.substringAfterLast('.')}.${description.methodName} " +
                "${SystemClock.elapsedRealtime() - start}ms volleyThreads=${TimingLog.volleyThreads()} " +
                "allThreads=${Thread.getAllStackTraces().size}"
        )
    }

    override fun failed(e: Throwable, description: Description) {
        Log.d(TimingLog.TAG, "FAILED ${description.methodName}: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
    }
}

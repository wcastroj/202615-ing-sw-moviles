package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SummaryCardWidgetTest {

    @Test
    fun test_SummaryCardWidget_Properties() {

        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        val card = SummaryCardWidget(materialContext)

        card.setContent(
            id = "100",
            title = "Test",
            subtitle = "Prueba"
        )

        val id = card.findViewById<TextView>(R.id.summaryCardId)
        val title = card.findViewById<TextView>(R.id.summaryCardTitle)
        val subtitle = card.findViewById<TextView>(R.id.summaryCardSubtitle)

        assertEquals("100", id.text.toString())
        assertEquals("Test", title.text.toString())
        assertEquals("Prueba", subtitle.text.toString())
    }
}
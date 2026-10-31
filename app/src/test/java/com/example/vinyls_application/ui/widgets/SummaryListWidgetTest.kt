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
class SummaryListWidgetTest {

    @Test
    fun test_SummaryListWidget_ItemsPopulation() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        val listWidget = SummaryListWidget(materialContext)

        val items = listOf(
            SummaryItem(id = "101", title = "Endless Journey", subtitle = "3:45"),
            SummaryItem(id = "102", title = "Silent Waves", subtitle = "2:11")
        )

        listWidget.setItems(items)

        assertEquals(2, listWidget.childCount)

        val firstCard = listWidget.getChildAt(0) as SummaryCardWidget
        val firstId = firstCard.findViewById<TextView>(R.id.summaryCardId)
        val firstTitle = firstCard.findViewById<TextView>(R.id.summaryCardTitle)
        val firstSubtitle = firstCard.findViewById<TextView>(R.id.summaryCardSubtitle)

        assertEquals("101", firstId.text.toString())
        assertEquals("Endless Journey", firstTitle.text.toString())
        assertEquals("3:45", firstSubtitle.text.toString())

        val secondCard = listWidget.getChildAt(1) as SummaryCardWidget
        val secondId = secondCard.findViewById<TextView>(R.id.summaryCardId)
        val secondTitle = secondCard.findViewById<TextView>(R.id.summaryCardTitle)
        val secondSubtitle = secondCard.findViewById<TextView>(R.id.summaryCardSubtitle)

        assertEquals("102", secondId.text.toString())
        assertEquals("Silent Waves", secondTitle.text.toString())
        assertEquals("2:11", secondSubtitle.text.toString())
    }
}
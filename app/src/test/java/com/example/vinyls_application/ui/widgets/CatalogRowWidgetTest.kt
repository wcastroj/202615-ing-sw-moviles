package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogRowWidgetTest {

    private lateinit var materialContext: Context
    private lateinit var row: CatalogRowWidget

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        row = CatalogRowWidget(materialContext)
    }

    @Test
    fun test_CatalogRowWidget_SetContent_ShowsAllTexts() {
        row.setContent(
            title = "Rubén Blades",
            subtitle = "Músico",
            detail = "16/07/1948",
            onAction = {}
        )

        val title = row.findViewById<TextView>(R.id.txtTitle)
        val subtitle = row.findViewById<TextView>(R.id.txtSubtitle)
        val detail = row.findViewById<TextView>(R.id.txtDetail)
        val button = row.findViewById<MaterialButton>(R.id.btnAction)

        assertEquals("Rubén Blades", title.text.toString())
        assertEquals("Músico", subtitle.text.toString())
        assertEquals(View.VISIBLE, subtitle.visibility)
        assertEquals("16/07/1948", detail.text.toString())
        assertEquals(View.VISIBLE, detail.visibility)
        assertEquals(
            materialContext.getString(R.string.catalog_card_action_default),
            button.text.toString()
        )
        assertEquals(View.VISIBLE, button.visibility)
    }

    @Test
    fun test_CatalogRowWidget_EmptyOptionalFields_AreHidden() {
        row.setContent(title = "Sin datos", subtitle = "", detail = null, onAction = null)

        assertEquals(View.GONE, row.findViewById<TextView>(R.id.txtSubtitle).visibility)
        assertEquals(View.GONE, row.findViewById<TextView>(R.id.txtDetail).visibility)
        assertEquals(View.GONE, row.findViewById<MaterialButton>(R.id.btnAction).visibility)
    }

    @Test
    fun test_CatalogRowWidget_CustomActionText() {
        row.setContent(title = "Queen", actionText = "Ver más", onAction = {})

        assertEquals("Ver más", row.findViewById<MaterialButton>(R.id.btnAction).text.toString())
    }

    @Test
    fun test_CatalogRowWidget_ActionClick_InvokesListener() {
        var clicked = false
        row.setContent(title = "Queen", onAction = { clicked = true })

        row.findViewById<MaterialButton>(R.id.btnAction).performClick()

        assertTrue("El callback del botón debería ejecutarse al hacer clic", clicked)
    }
}

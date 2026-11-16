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
class CatalogCardWidgetTest {

    private lateinit var materialContext: Context
    private lateinit var card: CatalogCardWidget

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        card = CatalogCardWidget(materialContext)
    }

    @Test
    fun test_CatalogCardWidget_SetContent_ShowsAllTexts() {
        card.setContent(
            title = "Buscando América",
            subtitle = "Salsa",
            detail = "Elektra",
            onAction = {}
        )

        val title = card.findViewById<TextView>(R.id.txtTitle)
        val subtitle = card.findViewById<TextView>(R.id.txtSubtitle)
        val detail = card.findViewById<TextView>(R.id.txtDetail)
        val button = card.findViewById<MaterialButton>(R.id.btnAction)

        assertEquals("Buscando América", title.text.toString())
        assertEquals("Salsa", subtitle.text.toString())
        assertEquals(View.VISIBLE, subtitle.visibility)
        assertEquals("Elektra", detail.text.toString())
        assertEquals(View.VISIBLE, detail.visibility)
        assertEquals(
            materialContext.getString(R.string.catalog_card_action_default),
            button.text.toString()
        )
        assertEquals(View.VISIBLE, button.visibility)
    }

    @Test
    fun test_CatalogCardWidget_EmptyOptionalFields_AreHidden() {
        card.setContent(title = "Sin datos", subtitle = "", detail = null, onAction = null)

        assertEquals(View.GONE, card.findViewById<TextView>(R.id.txtSubtitle).visibility)
        assertEquals(View.GONE, card.findViewById<TextView>(R.id.txtDetail).visibility)
        assertEquals(View.GONE, card.findViewById<MaterialButton>(R.id.btnAction).visibility)
    }

    @Test
    fun test_CatalogCardWidget_CustomActionText() {
        card.setContent(title = "Álbum", actionText = "Ver más", onAction = {})

        assertEquals("Ver más", card.findViewById<MaterialButton>(R.id.btnAction).text.toString())
    }

    @Test
    fun test_CatalogCardWidget_ActionClick_InvokesListener() {
        var clicked = false
        card.setContent(title = "Álbum", onAction = { clicked = true })

        card.findViewById<MaterialButton>(R.id.btnAction).performClick()

        assertTrue("El callback del botón debería ejecutarse al hacer clic", clicked)
    }
}

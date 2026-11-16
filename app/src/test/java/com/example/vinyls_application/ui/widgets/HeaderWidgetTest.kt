package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.widget.ImageView

@RunWith(RobolectricTestRunner::class)
class HeaderWidgetTest {

    private lateinit var header: HeaderWidget

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        header = HeaderWidget(materialContext)
    }

    @Test
    fun test_HeaderWidget_IsHorizontal() {
        assertEquals(LinearLayout.HORIZONTAL, header.orientation)
    }

    @Test
    fun test_HeaderWidget_SetContent_WithReturn() {
        header.setContent(header = "Detalle del Álbum", hasReturn = true)

        assertEquals("Detalle del Álbum", header.findViewById<TextView>(R.id.txtHeader).text.toString())
        assertEquals(View.VISIBLE, header.findViewById<ImageButton>(R.id.btnReturn).visibility)
    }

    @Test
    fun test_HeaderWidget_SetContent_WithoutReturn() {
        header.setContent(header = "Inicio", hasReturn = false)

        assertEquals("Inicio", header.findViewById<TextView>(R.id.txtHeader).text.toString())
        assertEquals(View.GONE, header.findViewById<ImageButton>(R.id.btnReturn).visibility)
    }

    @Test
    fun test_HeaderWidget_ReturnClick_InvokesListener() {
        var returned = false
        header.setContent(header = "Detalle", hasReturn = true)
        header.setOnReturnClickListener { returned = true }

        header.findViewById<ImageButton>(R.id.btnReturn).performClick()

        assertTrue("El callback de regreso debería ejecutarse al hacer clic", returned)
    }
    @Test
    fun test_HeaderWidget_SetContent_WithLogo() {
        header.setContent(header = "Vivo Sonika", hasReturn = false, hasLogo = true)

        assertEquals(View.VISIBLE, header.findViewById<ImageView>(R.id.ivLogo).visibility)
        assertEquals(View.GONE, header.findViewById<ImageButton>(R.id.btnReturn).visibility)
    }

    @Test
    fun test_HeaderWidget_SetContent_WithoutLogo() {
        header.setContent(header = "Detalle del Álbum", hasReturn = true, hasLogo = false)

        assertEquals(View.GONE, header.findViewById<ImageView>(R.id.ivLogo).visibility)
    }

    @Test
    fun test_HeaderWidget_SetContent_LogoHiddenByDefault() {
        header.setContent(header = "Inicio", hasReturn = false)

        assertEquals(View.GONE, header.findViewById<ImageView>(R.id.ivLogo).visibility)
    }
}

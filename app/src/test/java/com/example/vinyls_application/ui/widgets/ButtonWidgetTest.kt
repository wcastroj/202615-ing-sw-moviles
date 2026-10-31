package com.example.vinyls_application.ui.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.view.ContextThemeWrapper

@RunWith(RobolectricTestRunner::class)
class ButtonWidgetTest {

    @Test
    fun test_ButtonWidget_PropertiesAndClick() {

        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, com.example.vinyls_application.R.style.Theme_Vinyls_Jetpack_Application)
        val button = ButtonWidget(materialContext)
        var isClicked = false

        button.setOnClick {
            isClicked = true
        }

        button.buttonText = "text"
        button.hasIcon = true
        button.performClick()

        assertEquals("text", button.buttonText)
        assertEquals(true, button.hasIcon)
        assertTrue("Button should be clicked", isClicked)
    }
}
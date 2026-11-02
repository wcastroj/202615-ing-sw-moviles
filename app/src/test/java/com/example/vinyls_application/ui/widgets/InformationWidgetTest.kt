package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InformationWidgetTest {

    @Test
    fun test_InformationWidget_SetContentAndClick() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        val widget = InformationWidget(materialContext)

        var buttonClicked = false
        val testTitle = "¡Ups! Algo salió mal"
        val testDescription = "Intenta nuevamente más tarde."
        val testButtonText = "Volver a Inicio"

        // Probamos la función principal setContent de tu InformationWidget
        widget.setContent(
            title = testTitle,
            description = testDescription,
            iconRes = R.drawable.ic_signal_off,
            buttonText = testButtonText,
            onButtonClick = {
                buttonClicked = true
            }
        )

        assertEquals(testTitle, widget.findViewById<TextView>(R.id.title).text.toString())
        assertEquals(testDescription, widget.findViewById<TextView>(R.id.description).text.toString())

        val button = widget.findViewById<ButtonWidget>(R.id.buttonPrimary)
        assertNotNull("El botón principal no debe ser nulo", button)
        assertEquals(testButtonText, button.buttonText)

        button.performClick()
        assertTrue("El callback del botón debería haberse ejecutado al hacer clic", buttonClicked)
    }
}
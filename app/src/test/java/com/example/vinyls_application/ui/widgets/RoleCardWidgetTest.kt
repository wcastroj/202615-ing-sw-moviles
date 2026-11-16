package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoleCardWidgetTest {

    @Test
    fun test_RoleCardWidget_VisitorPropertiesAndClick() {

        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        val card = RoleCardWidget(materialContext)
        var isClicked = false

        card.setOnEnterClickListener {
            isClicked = true
        }

        card.setPrimary(false)
        card.performClick()

        assertEquals(View.VISIBLE, card.findViewById<TextView>(R.id.btnRoleEnter).visibility)
        assertEquals(View.GONE, card.findViewById<TextView>(R.id.btnRoleEnterPrimary).visibility)
        assertTrue("Card should be clicked", isClicked)
    }

    @Test
    fun test_RoleCardWidget_CollectorPropertiesAndClick() {

        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        val card = RoleCardWidget(materialContext)
        var isClicked = false

        card.setOnEnterClickListener {
            isClicked = true
        }

        card.setPrimary(true)
        card.findViewById<TextView>(R.id.btnRoleEnterPrimary).performClick()

        assertEquals(View.GONE, card.findViewById<TextView>(R.id.btnRoleEnter).visibility)
        assertEquals(View.VISIBLE, card.findViewById<TextView>(R.id.btnRoleEnterPrimary).visibility)
        assertTrue("Primary button should be clicked", isClicked)
    }
}
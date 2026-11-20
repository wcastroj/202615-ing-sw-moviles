package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DropdownListWidgetTest {

    private lateinit var widget: DropdownListWidget

    private val items = listOf(
        DropdownItem(id = 1, title = "Buscando América"),
        DropdownItem(id = 2, title = "Signos")
    )

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        widget = DropdownListWidget(materialContext)
    }

    @Test
    fun test_initialState_noSelectedItem() {
        assertNull(widget.getSelectedItem())
    }

    @Test
    fun test_setItems_populatesList() {
        widget.setItems(items)
        assertNull(widget.getSelectedItem())
    }

    @Test
    fun test_dropdownItem_toStringReturnsTitle() {
        val item = DropdownItem(id = 100, title = "Buscando América")
        assertEquals("Buscando América", item.toString())
    }

    @Test
    fun test_clearSelection_resetsSelection() {
        widget.setItems(items)
        widget.clearSelection()
        assertNull(widget.getSelectedItem())
    }
}

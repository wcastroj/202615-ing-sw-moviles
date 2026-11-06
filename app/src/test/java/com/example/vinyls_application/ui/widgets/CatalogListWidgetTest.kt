package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogListWidgetTest {

    private lateinit var listWidget: CatalogListWidget

    private val items = listOf(
        CatalogItem(id = 100, title = "Buscando América", subtitle = "Salsa", detail = "Elektra"),
        CatalogItem(id = 101, title = "Poeta del pueblo", subtitle = "Salsa", detail = "EMI")
    )

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        listWidget = CatalogListWidget(materialContext)
    }

    /** El RecyclerView solo crea las tarjetas después de medirse y posicionarse. */
    private fun layoutWidget() {
        listWidget.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        listWidget.layout(0, 0, 1080, 1920)
    }

    @Test
    fun test_CatalogListWidget_DefaultGridWithTwoColumns() {
        val layoutManager = listWidget.layoutManager

        assertTrue(layoutManager is GridLayoutManager)
        assertEquals(2, (layoutManager as GridLayoutManager).spanCount)
    }

    @Test
    fun test_CatalogListWidget_ItemsPopulation() {
        listWidget.setItems(items)
        layoutWidget()

        assertEquals(2, listWidget.adapter!!.itemCount)

        val firstCard = listWidget.findViewHolderForAdapterPosition(0)!!.itemView as CatalogCardWidget
        assertEquals("Buscando América", firstCard.findViewById<TextView>(R.id.txtTitle).text.toString())
        assertEquals("Salsa", firstCard.findViewById<TextView>(R.id.txtSubtitle).text.toString())
        assertEquals("Elektra", firstCard.findViewById<TextView>(R.id.txtDetail).text.toString())

        val secondCard = listWidget.findViewHolderForAdapterPosition(1)!!.itemView as CatalogCardWidget
        assertEquals("Poeta del pueblo", secondCard.findViewById<TextView>(R.id.txtTitle).text.toString())
    }

    @Test
    fun test_CatalogListWidget_ItemTagContainsId() {
        listWidget.setItems(items)
        layoutWidget()

        val viewHolder = listWidget.findViewHolderForAdapterPosition(1)
        assertNotNull(viewHolder)
        assertEquals(101, viewHolder!!.itemView.getTag(R.id.tag_album_id))
    }

    @Test
    fun test_CatalogListWidget_CardClick_InvokesListenerWithItem() {
        var clickedItem: CatalogItem? = null
        listWidget.setOnItemClickListener { clickedItem = it }
        listWidget.setItems(items)
        layoutWidget()

        listWidget.findViewHolderForAdapterPosition(0)!!.itemView.performClick()

        assertEquals(items[0], clickedItem)
    }

    @Test
    fun test_CatalogListWidget_ActionButtonClick_InvokesListenerWithItem() {
        var clickedItem: CatalogItem? = null
        listWidget.setOnItemClickListener { clickedItem = it }
        listWidget.setItems(items)
        layoutWidget()

        listWidget.findViewHolderForAdapterPosition(1)!!.itemView
            .findViewById<MaterialButton>(R.id.btnAction)
            .performClick()

        assertEquals(items[1], clickedItem)
    }

    @Test
    fun test_CatalogListWidget_ListMode_UsesLinearLayoutAndRows() {
        listWidget.mode = CatalogListWidget.Mode.LIST
        listWidget.setItems(items)
        layoutWidget()

        val layoutManager = listWidget.layoutManager
        assertTrue(layoutManager is LinearLayoutManager && layoutManager !is GridLayoutManager)

        val firstRow = listWidget.findViewHolderForAdapterPosition(0)!!.itemView
        assertTrue(firstRow is CatalogRowWidget)
        assertEquals("Buscando América", firstRow.findViewById<TextView>(R.id.txtTitle).text.toString())
        assertEquals("Salsa", firstRow.findViewById<TextView>(R.id.txtSubtitle).text.toString())
        assertEquals("Elektra", firstRow.findViewById<TextView>(R.id.txtDetail).text.toString())
    }

    @Test
    fun test_CatalogListWidget_ListMode_ActionButtonClick_InvokesListenerWithItem() {
        var clickedItem: CatalogItem? = null
        listWidget.mode = CatalogListWidget.Mode.LIST
        listWidget.setOnItemClickListener { clickedItem = it }
        listWidget.setItems(items)
        layoutWidget()

        listWidget.findViewHolderForAdapterPosition(1)!!.itemView
            .findViewById<MaterialButton>(R.id.btnAction)
            .performClick()

        assertEquals(items[1], clickedItem)
    }

    @Test
    fun test_CatalogListWidget_SetItemsReplacesPreviousItems() {
        listWidget.setItems(items)
        listWidget.setItems(listOf(items[0]))

        assertEquals(1, listWidget.adapter!!.itemCount)
    }
}

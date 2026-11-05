package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationWidgetTest {

    private lateinit var materialContext: Context
    private lateinit var navigation: NavigationWidget

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        navigation = NavigationWidget(materialContext)
    }

    @Test
    fun test_NavigationWidget_ContainsBottomNavigation() {
        assertNotNull(navigation.findViewById<BottomNavigationView>(R.id.bottomNavigation))
    }

    @Test
    fun test_NavigationWidget_MenuItems() {
        val menu = navigation.findViewById<BottomNavigationView>(R.id.bottomNavigation).menu

        assertEquals(3, menu.size())
        assertEquals(R.id.albumFragment, menu.getItem(0).itemId)
        assertEquals(R.id.navigation_albums, menu.getItem(1).itemId)
        assertEquals(R.id.navigation_artists, menu.getItem(2).itemId)

        assertEquals(materialContext.getString(R.string.navigation_home), menu.getItem(0).title.toString())
        assertEquals(materialContext.getString(R.string.navigation_albums), menu.getItem(1).title.toString())
        assertEquals(materialContext.getString(R.string.navigation_artists), menu.getItem(2).title.toString())
    }

    @Test
    fun test_NavigationWidget_BackgroundAndElevation() {
        assertNotNull("El widget debe tener el fondo redondeado", navigation.background)
        assertTrue("El widget debe tener elevación", navigation.elevation > 0f)
        assertTrue("El widget debe recortar su contenido al fondo redondeado", navigation.clipToOutline)
    }
}

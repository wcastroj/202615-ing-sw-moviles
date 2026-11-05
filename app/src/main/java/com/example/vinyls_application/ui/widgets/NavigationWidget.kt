package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.ui.setupWithNavController
import com.example.vinyls_application.R
import com.example.vinyls_application.databinding.NavigationWidgetBinding

/**
 * Barra de navegación inferior (Custom View) con fondo redondeado, basada en
 * `BottomNavigationView` y en el menú `@menu/navigation_widget_menu`.
 *
 * **Ejemplo de uso en XML:**
 * ```xml
 * <com.example.vinyls_application.ui.widgets.NavigationWidget
 *     android:id="@+id/navigationWidget"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * **Ejemplo de uso en Kotlin:**
 * ```kotlin
 * binding.navigationWidget.setupWithNavController(navController)
 * ```
 */
class NavigationWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = NavigationWidgetBinding.inflate(LayoutInflater.from(context), this)

    init {
        // Se aplican sobre el widget para que la sombra no quede recortada
        background = ContextCompat.getDrawable(context, R.drawable.bg_navigation_widget)
        elevation = 8 * resources.displayMetrics.density
        clipToOutline = true
    }

    /** Conecta la barra con el [NavController] para navegar al seleccionar un ítem. */
    fun setupWithNavController(navController: NavController) {
        binding.bottomNavigation.setupWithNavController(navController)
    }
}

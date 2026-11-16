package com.example.vinyls_application.ui.adapters

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import com.example.vinyls_application.models.Album
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RelatedAlbumsAdapterTest {

    private lateinit var adapter: RelatedAlbumsAdapter
    private lateinit var context: Context

    @Before
    fun setUp() {
        adapter = RelatedAlbumsAdapter()
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
    }

    @Test
    fun getItemCount_initiallyEmpty_returnsZero() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun getItemCount_afterSettingAlbums_returnsCorrectSize() {
        val albums = listOf(
            Album(101, "A Night at the Opera", "https://cover.jpg", "1975-11-21T00:00:00-05:00", "Desc", "Rock", "EMI"),
            Album(102, "Abbey Road", "https://cover2.jpg", "1969-09-26T00:00:00-05:00", "Desc 2", "Rock", "Apple")
        )

        adapter.albums = albums

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun onCreateViewHolder_and_onBindViewHolder_bindsDataCorrectly() {
        val parent = FrameLayout(context)
        val album = Album(
            albumId = 100,
            name = "Buscando América",
            cover = "https://i.pinimg.com/564x/aa/5f/ed/aa5fed7dac6130f36d2f05280a9b50e2.jpg",
            releaseDate = "1984-08-01T00:00:00-05:00",
            description = "Buscando América es el primer álbum de la banda de Rubén Blades",
            genre = "Salsa",
            recordLabel = "Elektra"
        )

        adapter.albums = listOf(album)

        val viewHolder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(viewHolder, 0)

        val idTextView = viewHolder.widget.findViewById<TextView>(R.id.summaryCardId)
        val titleTextView = viewHolder.widget.findViewById<TextView>(R.id.summaryCardTitle)
        val subtitleTextView = viewHolder.widget.findViewById<TextView>(R.id.summaryCardSubtitle)

        assertEquals("100", idTextView.text.toString())
        assertEquals("Buscando América", titleTextView.text.toString())
        assertEquals("1984-08-01", subtitleTextView.text.toString())
    }
}
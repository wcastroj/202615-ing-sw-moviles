package com.example.vinyls_application.ui.widgets

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.EditText
import android.widget.RadioButton
import androidx.test.core.app.ApplicationProvider
import com.example.vinyls_application.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddCommentWidgetTest {

    private lateinit var widget: AddCommentWidget

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val materialContext = ContextThemeWrapper(baseContext, R.style.Theme_Vinyls_Jetpack_Application)
        widget = AddCommentWidget(materialContext)
    }

    @Test
    fun test_initialState_noRatingAndEmptyComment() {
        assertNull(widget.getSelectedRating())
        assertEquals("", widget.getCommentText())
    }

    @Test
    fun test_validation_failsWhenNoRatingSelected() {
        val etComment = widget.findViewById<EditText>(R.id.etComment)
        etComment.setText("Un comentario válido")

        val isValid = widget.validateInputs()

        assertFalse(isValid)
    }

    @Test
    fun test_validation_failsWhenCommentTooShort() {
        val rbRating5 = widget.findViewById<RadioButton>(R.id.rbRating5)
        rbRating5.isChecked = true
        val etComment = widget.findViewById<EditText>(R.id.etComment)
        etComment.setText("A")

        val isValid = widget.validateInputs()

        assertFalse(isValid)
        assertEquals(5, widget.getSelectedRating())
    }

    @Test
    fun test_validation_passesWhenValidInputs() {
        val rbRating4 = widget.findViewById<RadioButton>(R.id.rbRating4)
        rbRating4.isChecked = true
        val etComment = widget.findViewById<EditText>(R.id.etComment)
        etComment.setText("Excelente producción sonora.")

        val isValid = widget.validateInputs()

        assertTrue(isValid)
        assertEquals(4, widget.getSelectedRating())
        assertEquals("Excelente producción sonora.", widget.getCommentText())
    }

    @Test
    fun test_submitListener_triggeredWhenValid() {
        var submittedRating: Int? = null
        var submittedComment: String? = null

        widget.onSubmitCommentListener = { rating, comment ->
            submittedRating = rating
            submittedComment = comment
        }

        val rbRating3 = widget.findViewById<RadioButton>(R.id.rbRating3)
        rbRating3.isChecked = true
        val etComment = widget.findViewById<EditText>(R.id.etComment)
        etComment.setText("Buen disco.")

        widget.findViewById<ButtonWidget>(R.id.btnSubmitComment).performClick()

        assertEquals(3, submittedRating)
        assertEquals("Buen disco.", submittedComment)
    }

    @Test
    fun test_clearForm_resetsFields() {
        val rbRating2 = widget.findViewById<RadioButton>(R.id.rbRating2)
        rbRating2.isChecked = true
        val etComment = widget.findViewById<EditText>(R.id.etComment)
        etComment.setText("Comentario de prueba")

        widget.clearForm()

        assertNull(widget.getSelectedRating())
        assertEquals("", widget.getCommentText())
    }
}

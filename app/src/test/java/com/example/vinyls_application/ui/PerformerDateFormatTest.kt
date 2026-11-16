package com.example.vinyls_application.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PerformerDateFormatTest {

    @Test
    fun test_FormatPerformerDate_IsoDate_ReturnsDayMonthYear() {
        assertEquals("25/10/2009", PerformerFragment.formatPerformerDate("2009-10-25T05:00:00.000Z"))
    }

    @Test
    fun test_FormatPerformerDate_Null_ReturnsNull() {
        assertNull(PerformerFragment.formatPerformerDate(null))
    }

    @Test
    fun test_FormatPerformerDate_UnknownFormat_ReturnsRaw() {
        assertEquals("sin fecha", PerformerFragment.formatPerformerDate("sin fecha"))
    }
}

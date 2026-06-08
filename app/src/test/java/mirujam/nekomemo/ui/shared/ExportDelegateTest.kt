package mirujam.nekomemo.ui.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportDelegateTest {

    @Test
    fun exportState_equals_sameData() {
        val state1 = ExportState(
            json = "{\"test\":1}",
            fileName = "test.nekomemo.json",
            format = ExportFormat.JSON
        )
        val state2 = ExportState(
            json = "{\"test\":1}",
            fileName = "test.nekomemo.json",
            format = ExportFormat.JSON
        )

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun exportState_notEquals_differentJson() {
        val state1 = ExportState(json = "{\"a\":1}", fileName = "test.json", format = ExportFormat.JSON)
        val state2 = ExportState(json = "{\"b\":2}", fileName = "test.json", format = ExportFormat.JSON)

        assertNotEquals(state1, state2)
    }

    @Test
    fun exportState_equals_sameDocxBytes() {
        val bytes1 = byteArrayOf(1, 2, 3, 4)
        val bytes2 = byteArrayOf(1, 2, 3, 4)
        val state1 = ExportState(
            docxBytes = bytes1,
            fileName = "test.docx",
            format = ExportFormat.DOCX
        )
        val state2 = ExportState(
            docxBytes = bytes2,
            fileName = "test.docx",
            format = ExportFormat.DOCX
        )

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun exportState_notEquals_differentDocxBytes() {
        val state1 = ExportState(
            docxBytes = byteArrayOf(1, 2, 3),
            fileName = "test.docx",
            format = ExportFormat.DOCX
        )
        val state2 = ExportState(
            docxBytes = byteArrayOf(4, 5, 6),
            fileName = "test.docx",
            format = ExportFormat.DOCX
        )

        assertNotEquals(state1, state2)
    }

    @Test
    fun exportState_notEquals_nullVsNonNullDocx() {
        val state1 = ExportState(
            docxBytes = byteArrayOf(1, 2),
            fileName = "test.docx",
            format = ExportFormat.DOCX
        )
        val state2 = ExportState(
            docxBytes = null,
            fileName = "test.docx",
            format = ExportFormat.DOCX
        )

        assertNotEquals(state1, state2)
    }

    @Test
    fun exportState_isReady_jsonFormat() {
        val state = ExportState(
            json = "{\"test\":1}",
            fileName = "test.json",
            format = ExportFormat.JSON
        )

        assertTrue(state.isReady)
    }

    @Test
    fun exportState_isReady_docxFormat() {
        val state = ExportState(
            docxBytes = byteArrayOf(1, 2, 3),
            fileName = "test.docx",
            format = ExportFormat.DOCX
        )

        assertTrue(state.isReady)
    }

    @Test
    fun exportFormat_extensionsCorrect() {
        assertEquals("nekomemo.json", ExportFormat.JSON.extension)
        assertEquals("docx", ExportFormat.DOCX.extension)
    }
}

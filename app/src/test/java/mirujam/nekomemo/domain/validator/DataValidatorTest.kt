package mirujam.nekomemo.domain.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataValidatorTest {

    @Test
    fun sanitizeOptions_trimsLimitsAndDropsBlankOptions() {
        val options = listOf(" A ", "", "B", " ".repeat(3), "C")

        val sanitized = DataValidator.sanitizeOptions(options)

        assertEquals(listOf("A", "B", "C"), sanitized)
    }

    @Test
    fun validateCorrectIndex_clampsToAvailableOptions() {
        val options = listOf("A", "B", "C")

        assertEquals(0, DataValidator.validateCorrectIndex(-1, options))
        assertEquals(2, DataValidator.validateCorrectIndex(10, options))
        assertEquals(1, DataValidator.validateCorrectIndex(1, options))
    }

    @Test
    fun validateCorrectIndex_returnsZeroWhenOptionsAreEmpty() {
        assertEquals(0, DataValidator.validateCorrectIndex(10, emptyList()))
    }

    @Test
    fun validateCorrectIndices_filtersInvalidAndReturnsFirstAsFallback() {
        val options = listOf("A", "B", "C")

        val result = DataValidator.validateCorrectIndices(listOf(-1, 1, 5, 2), options)

        assertEquals(listOf(1, 2), result)
    }

    @Test
    fun validateCorrectIndices_returnsEmptyListWhenOptionsEmpty() {
        assertEquals(emptyList<Int>(), DataValidator.validateCorrectIndices(listOf(1, 2), emptyList()))
    }

    @Test
    fun sanitizeString_trimsAndTruncates() {
        val longString = "A".repeat(1000)

        val result = DataValidator.sanitizeString(longString, 100)

        assertEquals(100, result.length)
        assertEquals("A".repeat(100), result)
    }

    @Test
    fun sanitizeString_returnsDefaultForBlank() {
        assertEquals("default", DataValidator.sanitizeString("   ", 100, "default"))
        assertEquals("default", DataValidator.sanitizeString("", 100, "default"))
    }

    @Test
    fun sanitizeOptions_limitsToMaxCount() {
        val options = (1..15).map { "Option $it" }

        val result = DataValidator.sanitizeOptions(options)

        assertEquals(DataValidator.MAX_OPTIONS_COUNT, result.size)
    }

    @Test
    fun validateTitle_sanitizesAndTruncates() {
        val longTitle = "T".repeat(300)

        val result = DataValidator.validateTitle(longTitle)

        assertEquals(DataValidator.MAX_TITLE_LENGTH, result.length)
    }

    @Test
    fun isCategoryValid_checksLengthAndBlank() {
        assertFalse(DataValidator.isCategoryValid(""))
        assertFalse(DataValidator.isCategoryValid("   "))
        assertFalse(DataValidator.isCategoryValid("A".repeat(DataValidator.MAX_CATEGORY_LENGTH + 1)))
        assertTrue(DataValidator.isCategoryValid("Valid"))
    }

    @Test
    fun sanitizeContent_truncatesWhenTooLong() {
        val longContent = "C".repeat(15000)

        val result = DataValidator.sanitizeContent(longContent)

        assertEquals(DataValidator.MAX_TEXT_LENGTH, result.length)
    }
}

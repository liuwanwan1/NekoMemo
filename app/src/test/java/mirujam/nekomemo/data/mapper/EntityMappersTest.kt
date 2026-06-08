package mirujam.nekomemo.data.mapper

import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionBank
import mirujam.nekomemo.domain.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    @Test
    fun questionBankEntity_toDomainModel_mapsAllFields() {
        val entity = QuestionBankEntity(
            id = 1L,
            title = "Test Bank",
            categoryId = 2L,
            createdAt = 12345678L
        )

        val domain = entity.toDomainModel()

        assertEquals(1L, domain.id)
        assertEquals("Test Bank", domain.title)
        assertEquals(2L, domain.categoryId)
        assertEquals(12345678L, domain.createdAt)
    }

    @Test
    fun questionBank_toEntity_mapsAllFields() {
        val domain = QuestionBank(
            id = 1L,
            title = "Test Bank",
            categoryId = 2L,
            createdAt = 12345678L
        )

        val entity = domain.toEntity()

        assertEquals(1L, entity.id)
        assertEquals("Test Bank", entity.title)
        assertEquals(2L, entity.categoryId)
        assertEquals(12345678L, entity.createdAt)
    }

    @Test
    fun questionEntity_toDomainModel_mapsSingleChoiceCorrectly() {
        val entity = QuestionEntity(
            id = 1L,
            questionBankId = 10L,
            text = "What is 2+2?",
            questionType = "single",
            options = "[\"4\",\"3\",\"5\"]",
            correctIndex = 0,
            correctIndices = "[0]"
        )

        val domain = entity.toDomainModel()

        assertEquals(1L, domain.id)
        assertEquals(10L, domain.questionBankId)
        assertEquals("What is 2+2?", domain.text)
        assertEquals(QuestionType.SINGLE_CHOICE, domain.questionType)
        assertEquals(listOf("4", "3", "5"), domain.options)
        assertEquals(0, domain.correctIndex)
        assertEquals(listOf(0), domain.correctIndices)
    }

    @Test
    fun questionEntity_toDomainModel_usesCorrectIndex_whenCorrectIndicesEmpty() {
        val entity = QuestionEntity(
            id = 1L,
            questionBankId = 10L,
            text = "Question",
            questionType = "single",
            options = "[\"A\",\"B\"]",
            correctIndex = 1,
            correctIndices = "[]"
        )

        val domain = entity.toDomainModel()

        assertEquals(listOf(1), domain.correctIndices)
        assertEquals(1, domain.correctIndex)
    }

    @Test
    fun questionEntity_toDomainModel_mapsMultipleChoiceCorrectly() {
        val entity = QuestionEntity(
            id = 1L,
            questionBankId = 10L,
            text = "Select all primes",
            questionType = "multiple",
            options = "[\"2\",\"3\",\"4\",\"5\"]",
            correctIndex = 0,
            correctIndices = "[0,1,3]"
        )

        val domain = entity.toDomainModel()

        assertEquals(QuestionType.MULTIPLE_CHOICE, domain.questionType)
        assertEquals(listOf(0, 1, 3), domain.correctIndices)
        assertEquals(0, domain.correctIndex)
    }

    @Test
    fun question_toEntity_ensuresCorrectIndexEqualsFirstCorrectIndices() {
        val domain = Question(
            id = 1L,
            questionBankId = 10L,
            text = "Question",
            questionType = QuestionType.MULTIPLE_CHOICE,
            options = listOf("A", "B", "C"),
            correctIndex = 2,
            correctIndices = listOf(0, 1)
        )

        val entity = domain.toEntity()

        // correctIndex should be synced to correctIndices.first()
        assertEquals(0, entity.correctIndex)
        assertEquals("[0,1]", entity.correctIndices)
    }

    @Test
    fun question_toEntity_usesCorrectIndex_whenCorrectIndicesEmpty() {
        val domain = Question(
            id = 1L,
            questionBankId = 10L,
            text = "Question",
            questionType = QuestionType.SINGLE_CHOICE,
            options = listOf("A", "B"),
            correctIndex = 1,
            correctIndices = emptyList()
        )

        val entity = domain.toEntity()

        assertEquals(1, entity.correctIndex)
        assertEquals("[1]", entity.correctIndices)
    }

    @Test
    fun question_toDomainModel_withBookmarkedIds_setsIsBookmarked() {
        val entity = QuestionEntity(
            id = 5L,
            questionBankId = 10L,
            text = "Question",
            questionType = "single",
            options = "[\"A\",\"B\"]",
            correctIndex = 0,
            correctIndices = "[0]"
        )

        val domain = entity.toDomainModel(bookmarkedIds = setOf(5L, 10L))

        assertEquals(true, domain.isBookmarked)
    }

    @Test
    fun question_toDomainModel_withoutBookmarkedId_setsIsBookmarkedFalse() {
        val entity = QuestionEntity(
            id = 5L,
            questionBankId = 10L,
            text = "Question",
            questionType = "single",
            options = "[\"A\",\"B\"]",
            correctIndex = 0,
            correctIndices = "[0]"
        )

        val domain = entity.toDomainModel(bookmarkedIds = setOf(10L))

        assertEquals(false, domain.isBookmarked)
    }

    @Test
    fun roundTrip_entityToDomainToEntity_preservesData() {
        val original = QuestionEntity(
            id = 1L,
            questionBankId = 10L,
            text = "What is 2+2?",
            questionType = "multiple",
            options = "[\"4\",\"3\",\"5\"]",
            correctIndex = 0,
            correctIndices = "[0]"
        )

        val domain = original.toDomainModel()
        val backToEntity = domain.toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.questionBankId, backToEntity.questionBankId)
        assertEquals(original.text, backToEntity.text)
        assertEquals(original.questionType, backToEntity.questionType)
        assertEquals(original.options, backToEntity.options)
        // correctIndex should be synced to correctIndices.first()
        assertEquals(0, backToEntity.correctIndex)
    }

    @Test
    fun roundTrip_bankEntityToDomainToEntity_preservesData() {
        val original = QuestionBankEntity(
            id = 1L,
            title = "Math",
            categoryId = 2L,
            createdAt = 12345L
        )

        val domain = original.toDomainModel()
        val backToEntity = domain.toEntity()

        assertEquals(original.id, backToEntity.id)
        assertEquals(original.title, backToEntity.title)
        assertEquals(original.categoryId, backToEntity.categoryId)
        assertEquals(original.createdAt, backToEntity.createdAt)
    }

    @Test
    fun listExtensions_mapCorrectly() {
        val entities = listOf(
            QuestionBankEntity(1L, "Bank A", 1L, 0L),
            QuestionBankEntity(2L, "Bank B", 1L, 0L)
        )

        val domains = entities.toDomainBankModels()

        assertEquals(2, domains.size)
        assertEquals("Bank A", domains[0].title)
        assertEquals("Bank B", domains[1].title)
    }
}

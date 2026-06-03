package mirujam.nekomemo.domain.model

data class Question(
    val id: Long = 0,
    val questionBankId: Long,
    val text: String,
    val questionType: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String>,
    val correctIndex: Int,
    val correctIndices: List<Int> = listOf(correctIndex),
    val isBookmarked: Boolean = false
)

enum class QuestionType(val key: String) {
    SINGLE_CHOICE("single"),
    MULTIPLE_CHOICE("multiple"),
    TRUE_FALSE("true_false");

    companion object {
        fun fromKey(key: String): QuestionType =
            entries.find { it.key == key } ?: SINGLE_CHOICE
    }
}

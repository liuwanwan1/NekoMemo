package mirujam.nekomemo.domain.model

data class QuestionBank(
    val id: Long = 0,
    val title: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

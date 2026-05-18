package mirujam.nekomemo.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class QuestionBank(
    val id: Long = 0,
    val title: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

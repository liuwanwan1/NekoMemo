package mirujam.nekomemo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_banks")
data class QuestionBankEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

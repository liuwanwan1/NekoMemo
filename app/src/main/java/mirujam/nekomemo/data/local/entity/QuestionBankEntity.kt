package mirujam.nekomemo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_banks",
    indices = [Index("createdAt")]
)
data class QuestionBankEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)

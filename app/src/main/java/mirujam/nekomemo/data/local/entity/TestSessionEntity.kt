package mirujam.nekomemo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_sessions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionBankEntity::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bankId")]
)
data class TestSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankId: Long,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val percentage: Int,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

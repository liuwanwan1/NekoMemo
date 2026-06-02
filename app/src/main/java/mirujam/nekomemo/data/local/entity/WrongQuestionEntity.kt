package mirujam.nekomemo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wrong_questions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionBankEntity::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("questionId"),
        Index("bankId")
    ]
)
data class WrongQuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long,
    val bankId: Long,
    val wrongCount: Int = 1,
    val lastWrongAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

package mirujam.nekomemo.data.local.entity

import androidx.room.ColumnInfo

data class QuestionCountByBank(
    @ColumnInfo(name = "questionBankId") val bankId: Long,
    @ColumnInfo(name = "count") val count: Int
)

package mirujam.nekomemo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE question_banks ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        QuestionBankEntity::class,
        QuestionEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NekoMemoDatabase : RoomDatabase() {
    abstract fun questionBankDao(): QuestionBankDao
    abstract fun questionDao(): QuestionDao
}

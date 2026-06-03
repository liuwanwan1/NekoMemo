package mirujam.nekomemo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mirujam.nekomemo.data.local.dao.BookmarkDao
import mirujam.nekomemo.data.local.dao.CategoryDao
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import mirujam.nekomemo.data.local.dao.TestSessionDao
import mirujam.nekomemo.data.local.dao.WrongQuestionDao
import mirujam.nekomemo.data.local.entity.BookmarkEntity
import mirujam.nekomemo.data.local.entity.CategoryEntity
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.data.local.entity.TestSessionEntity
import mirujam.nekomemo.data.local.entity.WrongQuestionEntity

@Database(
    entities = [
        QuestionBankEntity::class,
        QuestionEntity::class,
        CategoryEntity::class,
        TestSessionEntity::class,
        WrongQuestionEntity::class,
        BookmarkEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NekoMemoDatabase : RoomDatabase() {
    abstract fun questionBankDao(): QuestionBankDao
    abstract fun questionDao(): QuestionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun testSessionDao(): TestSessionDao
    abstract fun wrongQuestionDao(): WrongQuestionDao
    abstract fun bookmarkDao(): BookmarkDao
}

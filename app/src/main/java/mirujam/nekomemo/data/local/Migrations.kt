package mirujam.nekomemo.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_banks_createdAt` ON `question_banks` (`createdAt`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `questions_temp` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `questionBankId` INTEGER NOT NULL,
                `text` TEXT NOT NULL,
                `options` TEXT NOT NULL,
                `correctIndex` INTEGER NOT NULL,
                FOREIGN KEY(`questionBankId`) REFERENCES `question_banks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("""
            INSERT INTO `questions_temp` (`id`, `questionBankId`, `text`, `options`, `correctIndex`)
            SELECT `id`, `questionBankId`, `text`, `options`, `correctIndex` FROM `questions`
        """.trimIndent())
        db.execSQL("DROP TABLE `questions`")
        db.execSQL("ALTER TABLE `questions_temp` RENAME TO `questions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_questionBankId` ON `questions` (`questionBankId`)")
    }
}

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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
        db.execSQL("INSERT OR IGNORE INTO `categories` (`name`, `createdAt`) VALUES ('GENERAL', ${System.currentTimeMillis()})")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `question_banks` ADD COLUMN `categoryId` INTEGER")
        
        db.execSQL("""
            UPDATE `question_banks` 
            SET `categoryId` = (
                SELECT `id` FROM `categories` WHERE `categories`.`name` = `question_banks`.`category`
            )
        """)
        
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_banks_categoryId` ON `question_banks` (`categoryId`)")
        
        db.execSQL("DROP INDEX IF EXISTS `index_question_banks_createdAt`")
        
        db.execSQL("""
            CREATE TABLE `question_banks_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE RESTRICT
            )
        """.trimIndent())
        
        db.execSQL("""
            INSERT INTO `question_banks_new` (`id`, `title`, `categoryId`, `createdAt`)
            SELECT `id`, `title`, `categoryId`, `createdAt` FROM `question_banks`
        """.trimIndent())
        
        db.execSQL("DROP TABLE `question_banks`")
        db.execSQL("ALTER TABLE `question_banks_new` RENAME TO `question_banks`")
        
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_question_banks_createdAt` ON `question_banks` (`createdAt`)")
        
        db.execSQL("DROP COLUMN IF EXISTS `category`")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `categories_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories_new` (`name`)")
        db.execSQL("INSERT INTO `categories_new` (`id`, `name`) SELECT `id`, `name` FROM `categories`")
        db.execSQL("DROP TABLE `categories`")
        db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
    }
}

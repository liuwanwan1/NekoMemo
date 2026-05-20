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
        val columns = db.query("PRAGMA table_info('questions')").use { cursor ->
            val columnNames = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex >= 0) {
                    columnNames.add(cursor.getString(nameIndex))
                }
            }
            columnNames
        }

        val bankIdColumn = when {
            columns.contains("questionBankId") -> "questionBankId"
            columns.contains("bankId") -> "bankId"
            columns.contains("question_bank_id") -> "question_bank_id"
            else -> null
        }

        val correctIndexColumn = when {
            columns.contains("correctIndex") -> "correctIndex"
            columns.contains("correct_index") -> "correct_index"
            columns.contains("answer") -> "answer"
            else -> null
        }

        val hasRequiredColumns = bankIdColumn != null && correctIndexColumn != null

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

        if (hasRequiredColumns && bankIdColumn != null && correctIndexColumn != null) {
            db.execSQL("""
                INSERT INTO `questions_temp` (`id`, `questionBankId`, `text`, `options`, `correctIndex`)
                SELECT `id`, `$bankIdColumn`, `text`, `options`, `$correctIndexColumn` FROM `questions`
            """.trimIndent())
        }

        db.execSQL("DROP TABLE IF EXISTS `questions`")
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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val questions = db.query("SELECT `id`, `options`, `correctIndex` FROM `questions`")
        val updates = mutableListOf<Triple<Long, String, Int>>()

        while (questions.moveToNext()) {
            val idIndex = questions.getColumnIndex("id")
            val optionsIndex = questions.getColumnIndex("options")
            val correctIndexColIndex = questions.getColumnIndex("correctIndex")

            if (idIndex < 0 || optionsIndex < 0 || correctIndexColIndex < 0) continue

            val id = questions.getLong(idIndex)
            val optionsJson = questions.getString(optionsIndex)
            val correctIndex = questions.getInt(correctIndexColIndex)

            try {
                val optionsArray = org.json.JSONArray(optionsJson)
                val cleanedOptions = mutableListOf<String>()
                val letterPrefixRegex = Regex("^[A-Ha-h]\\.\\s*")

                for (i in 0 until optionsArray.length()) {
                    val option = optionsArray.getString(i)
                    val cleaned = letterPrefixRegex.replace(option, "")
                    cleanedOptions.add(cleaned)
                }

                val originalCorrectOption = if (correctIndex >= 0 && correctIndex < cleanedOptions.size) {
                    cleanedOptions[correctIndex]
                } else null

                val newCorrectIndex = if (originalCorrectOption != null) {
                    cleanedOptions.indexOf(originalCorrectOption)
                } else correctIndex

                val newOptionsJson = org.json.JSONArray(cleanedOptions).toString()
                updates.add(Triple(id, newOptionsJson, newCorrectIndex))
            } catch (_: Exception) {
                // Skip malformed JSON
            }
        }
        questions.close()

        for ((id, options, newCorrectIndex) in updates) {
            val bindArgs = arrayOf<Any?>(options, newCorrectIndex, id)
            db.execSQL(
                "UPDATE `questions` SET `options` = ?, `correctIndex` = ? WHERE `id` = ?",
                bindArgs
            )
        }
    }
}

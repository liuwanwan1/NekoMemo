package mirujam.nekomemo.domain.usecase

import timber.log.Timber
import mirujam.nekomemo.data.repository.CategoryRepository
import mirujam.nekomemo.data.repository.QuestionRepository
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionBank
import mirujam.nekomemo.domain.model.QuestionType
import mirujam.nekomemo.domain.validator.DataValidator
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankExportImportUseCase @Inject constructor(
    private val repository: QuestionRepository,
    private val categoryRepository: CategoryRepository
) {

    companion object {
        private const val FORMAT_VERSION = 1
        private const val KEY_VERSION = "version"
        private const val KEY_NEKOMEMO = "nekomemo"
    }

    suspend fun exportBankToJson(bankId: Long): String? {
        val bank = repository.getBankById(bankId) ?: return null
        val questions = repository.getQuestionsForBankSync(bankId)

        val json = JSONObject()
        json.put("title", bank.title)
        json.put("categoryId", bank.categoryId)

        val questionsArray = JSONArray()
        questions.forEach { q ->
            val qJson = JSONObject()
            qJson.put("text", q.text)
            qJson.put("questionType", q.questionType.key)
            qJson.put("options", JSONArray(q.options))
            qJson.put("correctIndex", q.correctIndex)
            qJson.put("correctIndices", JSONArray(q.correctIndices))
            questionsArray.put(qJson)
        }
        json.put("questions", questionsArray)

        val wrapper = JSONObject()
        wrapper.put(KEY_VERSION, FORMAT_VERSION)
        wrapper.put(KEY_NEKOMEMO, json)
        return wrapper.toString(2)
    }

    suspend fun exportBankToDocx(bankId: Long): ByteArray? {
        val bank = repository.getBankById(bankId) ?: return null
        val questions = repository.getQuestionsForBankSync(bankId)

        val document = XWPFDocument()

        // Title
        val titlePara = document.createParagraph()
        val titleRun = titlePara.createRun()
        titleRun.setText(bank.title)
        titleRun.setBold(true)
        titleRun.setFontSize(18)

        // Separator
        val sepPara = document.createParagraph()
        val sepRun = sepPara.createRun()
        sepRun.setText("─".repeat(30))
        sepRun.setColor("999999")

        // Questions
        questions.forEachIndexed { index, q ->
            val typeLabel = when (q.questionType) {
                QuestionType.SINGLE_CHOICE -> "单选题"
                QuestionType.MULTIPLE_CHOICE -> "多选题"
                QuestionType.TRUE_FALSE -> "判断题"
            }

            // Question text
            val qPara = document.createParagraph()
            qPara.setSpacingBefore(200)
            val typeRun = qPara.createRun()
            typeRun.setText("[$typeLabel] ")
            typeRun.setColor("666666")
            typeRun.setFontSize(10)
            val textRun = qPara.createRun()
            textRun.setText("${index + 1}. ${q.text}")
            textRun.setBold(true)
            textRun.setFontSize(11)

            // Options
            q.options.forEachIndexed { optIndex, option ->
                val optPara = document.createParagraph()
                optPara.setIndentationLeft(360)
                val letter = ('A' + optIndex)
                val isCorrect = optIndex in q.correctIndices
                val optRun = optPara.createRun()
                optRun.setText("$letter. $option${if (isCorrect) " ✓" else ""}")
                optRun.setFontSize(10)
                if (isCorrect) {
                    optRun.setBold(true)
                    optRun.setColor("2E7D32")
                }
            }
        }

        // Answer key
        if (questions.isNotEmpty()) {
            val answerTitlePara = document.createParagraph()
            answerTitlePara.setSpacingBefore(400)
            val answerTitleRun = answerTitlePara.createRun()
            answerTitleRun.setText("答案")
            answerTitleRun.setBold(true)
            answerTitleRun.setFontSize(14)

            val answerSepPara = document.createParagraph()
            val answerSepRun = answerSepPara.createRun()
            answerSepRun.setText("─".repeat(30))
            answerSepRun.setColor("999999")

            val answerLine = document.createParagraph()
            val answers = questions.mapIndexed { index, q ->
                val letters = q.correctIndices.sorted().map { ('A' + it) }.joinToString("")
                "${index + 1}. $letters"
            }
            val answerRun = answerLine.createRun()
            answerRun.setText(answers.joinToString("    "))
            answerRun.setFontSize(10)
        }

        val outputStream = ByteArrayOutputStream()
        document.write(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    suspend fun importBankFromJson(jsonString: String): Long {
        Timber.d("Starting import, JSON size: ${jsonString.length} bytes")

        if (jsonString.isBlank()) {
            Timber.w("Import failed: Empty JSON string")
            throw IllegalArgumentException("JSON string is empty")
        }

        if (jsonString.length > DataValidator.MAX_JSON_SIZE) {
            Timber.w("Import failed: JSON too large (${jsonString.length} > ${DataValidator.MAX_JSON_SIZE})")
            throw IllegalArgumentException("JSON size exceeds maximum limit of ${DataValidator.MAX_JSON_SIZE / 1024 / 1024}MB")
        }

        val wrapper: JSONObject
        try {
            wrapper = JSONObject(jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Import failed: Invalid JSON format")
            throw IllegalArgumentException("Invalid JSON format: ${e.message}")
        }

        val bankJson = wrapper.optJSONObject(KEY_NEKOMEMO) ?: wrapper

        val version = wrapper.optInt(KEY_VERSION, 0)
        if (version > FORMAT_VERSION) {
            Timber.w("Import warning: file version $version is newer than supported $FORMAT_VERSION, attempting best-effort import")
        }

        val title = DataValidator.validateTitle(bankJson.optString("title", "Imported Bank"))

        if (title.isBlank()) {
            Timber.w("Import failed: Invalid title after sanitization")
            throw IllegalArgumentException("Invalid bank title")
        }

        val categoryId = resolveCategoryId(bankJson)

        Timber.d("Creating bank with title='$title', categoryId=$categoryId")

        val bankId = repository.insertBank(
            QuestionBank(title = title, categoryId = categoryId)
        )

        val questionsArray = bankJson.optJSONArray("questions")
        if (questionsArray == null) {
            Timber.d("No questions array found, returning empty bank with id=$bankId")
            return bankId
        }

        if (questionsArray.length() > DataValidator.MAX_QUESTIONS_COUNT) {
            Timber.w("Questions count ${questionsArray.length()} exceeds limit ${DataValidator.MAX_QUESTIONS_COUNT}, truncating")
        }

        val validQuestions = mutableListOf<Question>()
        var skippedCount = 0

        val maxQuestions = minOf(questionsArray.length(), DataValidator.MAX_QUESTIONS_COUNT)

        for (i in 0 until maxQuestions) {
            try {
                val qJson = questionsArray.getJSONObject(i)
                val question = validateAndCreateQuestion(qJson, bankId, i)
                if (question != null) {
                    validQuestions.add(question)
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                Timber.w("Skipping invalid question at index $i: ${e.message}")
                skippedCount++
            }
        }

        if (validQuestions.isNotEmpty()) {
            Timber.d("Inserting ${validQuestions.size} valid questions ($skippedCount skipped)")
            repository.insertQuestions(validQuestions)
        } else if (skippedCount > 0) {
            Timber.w("All $skippedCount questions were invalid, created empty bank")
        }

        Timber.d("Import completed: bankId=$bankId, questions=${validQuestions.size}, skipped=$skippedCount")
        return bankId
    }

    private fun validateAndCreateQuestion(qJson: JSONObject, bankId: Long, index: Int): Question? {
        val rawText = qJson.optString("text", "")
        val text = DataValidator.sanitizeString(rawText, DataValidator.MAX_TEXT_LENGTH, "")

        if (text.isBlank()) {
            Timber.d("Question $index: Empty or invalid text, skipping")
            return null
        }

        val optionsArray = qJson.optJSONArray("options")
        val options = if (optionsArray != null) {
            parseAndValidateOptions(optionsArray)
        } else {
            emptyList()
        }

        if (options.size < DataValidator.MIN_OPTIONS_COUNT) {
            Timber.d("Question $index: Insufficient options (${options.size} < ${DataValidator.MIN_OPTIONS_COUNT}), skipping")
            return null
        }

        val questionType = QuestionType.fromKey(qJson.optString("questionType", "single"))
        val correctIndex = DataValidator.validateCorrectIndex(qJson.optInt("correctIndex", 0), options)

        val rawIndices = qJson.optJSONArray("correctIndices")
        val correctIndices = if (rawIndices != null) {
            val list = (0 until rawIndices.length()).mapNotNull { j ->
                try { rawIndices.getInt(j) } catch (e: Exception) { null }
            }
            DataValidator.validateCorrectIndices(list, options)
        } else {
            listOf(correctIndex)
        }

        return Question(
            questionBankId = bankId,
            text = text,
            questionType = questionType,
            options = options,
            correctIndex = correctIndex,
            correctIndices = correctIndices
        )
    }

    private fun parseAndValidateOptions(optionsArray: JSONArray): List<String> {
        val validatedOptions = mutableListOf<String>()

        val maxOptions = minOf(optionsArray.length(), DataValidator.MAX_OPTIONS_COUNT)

        for (i in 0 until maxOptions) {
            try {
                val option = optionsArray.getString(i)
                val sanitizedOption = DataValidator.sanitizeString(option, DataValidator.MAX_OPTION_LENGTH, "")
                if (sanitizedOption.isNotBlank()) {
                    validatedOptions.add(sanitizedOption)
                }
            } catch (e: Exception) {
                Timber.w(e, "Invalid option at index $i, skipping")
            }
        }

        return validatedOptions
    }

    suspend fun duplicateBank(bankId: Long): Long {
        return repository.duplicateBank(bankId)
    }

    private suspend fun resolveCategoryId(bankJson: JSONObject): Long {
        val categoryId = bankJson.optLong("categoryId", 0L)
        if (categoryId > 0) {
            return categoryId
        }

        val categoryName = DataValidator.validateCategory(bankJson.optString("category", CategoryRepository.DEFAULT_CATEGORY_NAME))
        val existingCategory = categoryRepository.getCategoryByName(categoryName)
        if (existingCategory != null) {
            return existingCategory.id
        }

        return categoryRepository.addCategory(categoryName).getOrElse {
            categoryRepository.getCategoryByName(CategoryRepository.DEFAULT_CATEGORY_NAME)?.id
                ?: throw IllegalStateException("Default category not found")
        }
    }
}

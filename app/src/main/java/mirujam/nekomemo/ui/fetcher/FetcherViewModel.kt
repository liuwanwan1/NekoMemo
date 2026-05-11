package mirujam.nekomemo.ui.fetcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mirujam.nekomemo.data.local.Converters
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.data.repository.QuestionRepository
import javax.inject.Inject

data class ParsedQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

@HiltViewModel
class FetcherViewModel @Inject constructor(
    private val repository: QuestionRepository,
    private val converters: Converters
) : ViewModel() {

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _parseResult = MutableStateFlow<String?>(null)
    val parseResult: StateFlow<String?> = _parseResult.asStateFlow()

    private val _parsedQuestions = MutableStateFlow<List<ParsedQuestion>>(emptyList())
    val parsedQuestions: StateFlow<List<ParsedQuestion>> = _parsedQuestions.asStateFlow()

    fun onQuestionsParsed(questions: List<ParsedQuestion>) {
        _parsedQuestions.value = questions
        if (questions.isNotEmpty()) {
            _parseResult.value = "Found ${questions.size} questions"
        } else {
            _parseResult.value = "No questions found on this page"
        }
    }

    fun saveQuestions(bankTitle: String, category: String) {
        viewModelScope.launch {
            _isParsing.value = true
            try {
                val bankId = repository.insertBank(
                    QuestionBankEntity(
                        title = bankTitle,
                        category = category
                    )
                )
                val entities = _parsedQuestions.value.map { q ->
                    QuestionEntity(
                        questionBankId = bankId,
                        text = q.text,
                        options = converters.fromStringList(q.options),
                        correctIndex = q.correctIndex
                    )
                }
                repository.insertQuestions(entities)
                _parseResult.value = "Saved ${entities.size} questions!"
                _parsedQuestions.value = emptyList()
            } catch (e: Exception) {
                _parseResult.value = "Error: ${e.message}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun clearResult() {
        _parseResult.value = null
        _parsedQuestions.value = emptyList()
    }
}

package mirujam.nekomemo.ui.extract

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
import mirujam.nekomemo.data.model.ExtractedQuestionBank
import mirujam.nekomemo.data.repository.QuestionRepository
import javax.inject.Inject

@HiltViewModel
class ExtractViewModel @Inject constructor(
    private val repository: QuestionRepository,
    private val converters: Converters
) : ViewModel() {

    private var _questionBank: ExtractedQuestionBank? = null

    private val _questionBankFlow = MutableStateFlow(_questionBank)
    val questionBank: StateFlow<ExtractedQuestionBank?> = _questionBankFlow.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult: StateFlow<String?> = _saveResult.asStateFlow()

    fun initFromJson(jsonData: String?) {
        if (jsonData != null) {
            _questionBank = ExtractedQuestionBank.fromJson(jsonData)
            _questionBankFlow.value = _questionBank
        }
    }

    fun saveQuestions(bankTitle: String, category: String) {
        val bank = _questionBank ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val bankId = repository.insertBank(
                    QuestionBankEntity(
                        title = bankTitle,
                        category = category
                    )
                )
                val entities = bank.questions.map { q ->
                    QuestionEntity(
                        questionBankId = bankId,
                        text = q.content,
                        options = converters.fromStringList(q.options),
                        correctIndex = q.correctIndex
                    )
                }
                repository.insertQuestions(entities)
                _saveResult.value = "Saved ${entities.size} questions!"
            } catch (e: Exception) {
                _saveResult.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}

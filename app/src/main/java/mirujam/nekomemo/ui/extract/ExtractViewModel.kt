package mirujam.nekomemo.ui.extract

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mirujam.nekomemo.R
import mirujam.nekomemo.data.model.ExtractedQuestionBank
import mirujam.nekomemo.data.model.ExtractedQuestionBankSerializer
import mirujam.nekomemo.data.repository.QuestionRepository
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionBank
import mirujam.nekomemo.ui.model.UiText
import mirujam.nekomemo.ui.shared.SharedDataStore
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class ExtractViewModel @Inject constructor(
    private val repository: QuestionRepository,
    private val sharedDataStore: SharedDataStore
) : ViewModel() {

    companion object {
        private const val TAG = "ExtractViewModel"
    }

    private var _questionBank: ExtractedQuestionBank? = null

    private val _questionBankFlow = MutableStateFlow(_questionBank)
    val questionBank: StateFlow<ExtractedQuestionBank?> = _questionBankFlow.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveResult = MutableStateFlow<UiText?>(null)
    val saveResult: StateFlow<UiText?> = _saveResult.asStateFlow()

    private val _isSaveSuccess = MutableStateFlow(false)
    val isSaveSuccess: StateFlow<Boolean> = _isSaveSuccess.asStateFlow()

    fun initFromJson(jsonData: String?) {
        Log.d(TAG, "initFromJson() called with data length: ${jsonData?.length ?: 0}")
        if (jsonData != null) {
            _questionBank = ExtractedQuestionBankSerializer.fromJson(jsonData)
            Log.d(TAG, "Parsed question bank: name='${_questionBank?.name}', questions=${_questionBank?.questions?.size ?: 0}")
            _questionBankFlow.value = _questionBank
        } else {
            Log.w(TAG, "initFromJson() called with null jsonData!")
        }
    }

    fun saveQuestions(bankTitle: String, category: String) {
        val bank = _questionBank
        if (bank == null) {
            Log.w(TAG, "saveQuestions() called but questionBank is null!")
            return
        }

        Log.d(TAG, "Saving ${bank.questions.size} questions with title='$bankTitle'")
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val bankId = repository.insertBank(
                    QuestionBank(
                        title = bankTitle,
                        category = category
                    )
                )
                val questions = bank.questions.map { q ->
                    Question(
                        questionBankId = bankId,
                        text = q.content,
                        options = q.options,
                        correctIndex = q.correctIndex
                    )
                }
                repository.insertQuestions(questions)
                Log.d(TAG, "Successfully saved ${questions.size} questions")
                _saveResult.value = UiText.PluralStringResource(R.plurals.extract_save_success, questions.size, arrayOf(questions.size))
                _isSaveSuccess.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving questions: ${e.message}", e)
                _saveResult.value = UiText.StringResource(
                    R.string.extract_save_error,
                    arrayOf(e.message ?: "")
                )
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun onNavigatedBack() {
        _isSaveSuccess.value = false
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    fun loadFromSharedDataStore(): String? {
        return sharedDataStore.getExtractedJson()
    }

    fun clearSharedDataStore(): Boolean {
        return sharedDataStore.clearExtractedJson()
    }
}

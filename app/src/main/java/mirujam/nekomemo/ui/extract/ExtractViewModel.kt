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
import mirujam.nekomemo.ui.shared.SharedDataStore
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class ExtractViewModel @Inject constructor(
    private val repository: QuestionRepository,
    private val converters: Converters,
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

    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult: StateFlow<String?> = _saveResult.asStateFlow()

    fun initFromJson(jsonData: String?) {
        Log.d(TAG, "initFromJson() called with data length: ${jsonData?.length ?: 0}")
        if (jsonData != null) {
            _questionBank = ExtractedQuestionBank.fromJson(jsonData)
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
                Log.d(TAG, "Successfully saved ${entities.size} questions")
                _saveResult.value = "Saved ${entities.size} questions!"
            } catch (e: Exception) {
                Log.e(TAG, "Error saving questions: ${e.message}", e)
                _saveResult.value = "Error: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }

    suspend fun loadFromSharedDataStore(): String? {
        return sharedDataStore.getExtractedJson()
    }

    suspend fun clearSharedDataStore(): Boolean {
        return sharedDataStore.clearExtractedJson()
    }
}

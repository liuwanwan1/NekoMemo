package mirujam.nekomemo.ui.wrong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mirujam.nekomemo.domain.model.QuestionBank
import mirujam.nekomemo.data.repository.QuestionRepository
import mirujam.nekomemo.data.repository.TestHistoryRepository
import mirujam.nekomemo.data.repository.WrongQuestionWithQuestion
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WrongQuestionsViewModel @Inject constructor(
    private val testHistoryRepository: TestHistoryRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _selectedBankId = MutableStateFlow<Long?>(null)
    val selectedBankId: StateFlow<Long?> = _selectedBankId.asStateFlow()

    val wrongQuestions: StateFlow<List<WrongQuestionWithQuestion>> = _selectedBankId
        .flatMapLatest { bankId ->
            if (bankId != null) {
                testHistoryRepository.getUnresolvedWrongQuestions(bankId)
            } else {
                testHistoryRepository.getAllUnresolvedWrongQuestions()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUnresolvedCount: StateFlow<Int> = testHistoryRepository
        .getTotalUnresolvedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unresolvedBankIds: StateFlow<List<Long>> = testHistoryRepository
        .getUnresolvedBankIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val banks: StateFlow<List<QuestionBank>> = questionRepository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectBank(bankId: Long?) {
        _selectedBankId.value = bankId
    }

    fun markResolved(wrongQuestionId: Long) {
        viewModelScope.launch {
            testHistoryRepository.markWrongQuestionResolved(wrongQuestionId)
        }
    }

    fun markQuestionResolved(questionId: Long) {
        viewModelScope.launch {
            testHistoryRepository.markQuestionResolved(questionId)
        }
    }
}

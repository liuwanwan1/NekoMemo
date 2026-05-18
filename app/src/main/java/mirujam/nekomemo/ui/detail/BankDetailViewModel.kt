package mirujam.nekomemo.ui.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mirujam.nekomemo.data.repository.QuestionRepository
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.domain.model.QuestionBank
import mirujam.nekomemo.domain.usecase.BankExportImportUseCase
import mirujam.nekomemo.ui.model.QuestionUiModel
import mirujam.nekomemo.ui.shared.ExportDelegate
import mirujam.nekomemo.ui.shared.ExportState
import javax.inject.Inject

private const val TAG = "BankDetailViewModel"

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BankDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuestionRepository,
    bankExportImportUseCase: BankExportImportUseCase
) : ViewModel() {

    private val bankId: Long = savedStateHandle["bankId"] ?: -1L

    private val exportDelegate = ExportDelegate(viewModelScope, bankExportImportUseCase)
    val exportState: StateFlow<ExportState> = exportDelegate.exportState

    val pagedQuestions: Flow<PagingData<QuestionUiModel>> = repository.getPagedQuestionsForBank(bankId)
        .map { pagingData -> pagingData.map { QuestionUiModel.fromDomainModel(it) } }
        .cachedIn(viewModelScope)

    val questions: StateFlow<List<Question>> = repository.getQuestionsForBank(bankId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _questionCount = MutableStateFlow(0)
    val questionCount: StateFlow<Int> = _questionCount.asStateFlow()

    private val _bankTitle = MutableStateFlow("")
    val bankTitle: StateFlow<String> = _bankTitle.asStateFlow()

    private val _bankCategory = MutableStateFlow("")
    val bankCategory: StateFlow<String> = _bankCategory.asStateFlow()

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _showAddQuestionDialog = MutableStateFlow(false)
    val showAddQuestionDialog: StateFlow<Boolean> = _showAddQuestionDialog.asStateFlow()

    private val _editingQuestionId = MutableStateFlow<Long?>(null)
    val editingQuestionId: StateFlow<Long?> = _editingQuestionId.asStateFlow()

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    private val _showDeleteBankConfirmDialog = MutableStateFlow(false)
    val showDeleteBankConfirmDialog: StateFlow<Boolean> = _showDeleteBankConfirmDialog.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredQuestions: StateFlow<List<QuestionUiModel>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                questions.map { list ->
                    list.filter { it.text.contains(query, ignoreCase = true) }
                        .map { QuestionUiModel.fromDomainModel(it) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pendingDeleteQuestion: Question? = null

    private val _currentBank = MutableStateFlow<QuestionBank?>(null)

    init {
        viewModelScope.launch {
            val bank = repository.getBankById(bankId)
            bank?.let {
                _currentBank.value = it
                _bankTitle.value = it.title
                _bankCategory.value = it.category
            }
        }
        viewModelScope.launch {
            repository.getQuestionCountForBank(bankId).collect { count ->
                _questionCount.value = count
            }
        }
    }

    fun deleteQuestion(question: Question) {
        pendingDeleteQuestion = question
        _showDeleteConfirmDialog.value = true
    }

    fun confirmDeleteQuestion() {
        val question = pendingDeleteQuestion ?: return
        viewModelScope.launch {
            try {
                repository.deleteQuestion(question)
                Log.d(TAG, "Deleted question ${question.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting question", e)
            } finally {
                _showDeleteConfirmDialog.value = false
                pendingDeleteQuestion = null
            }
        }
    }

    fun dismissDeleteConfirmDialog() {
        _showDeleteConfirmDialog.value = false
        pendingDeleteQuestion = null
    }

    fun showDeleteBankDialog() {
        _showDeleteBankConfirmDialog.value = true
    }

    fun dismissDeleteBankDialog() {
        _showDeleteBankConfirmDialog.value = false
    }

    fun confirmDeleteBank() {
        val bank = _currentBank.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteBank(bank)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting bank", e)
            } finally {
                _showDeleteBankConfirmDialog.value = false
            }
        }
    }

    fun prepareExport() {
        exportDelegate.prepareExport(bankId, _bankTitle.value)
    }

    fun clearExportState() {
        exportDelegate.clearExportState()
    }

    fun showEditDialog() {
        _showEditDialog.value = true
    }

    fun dismissEditDialog() {
        _showEditDialog.value = false
    }

    fun updateBank(title: String, category: String) {
        viewModelScope.launch {
            _currentBank.value?.let { bank ->
                val updated = bank.copy(title = title, category = category)
                repository.updateBank(updated)
                _bankTitle.value = title
                _bankCategory.value = category
                _showEditDialog.value = false
                _currentBank.value = updated
            }
        }
    }

    fun showAddQuestionDialog() {
        _showAddQuestionDialog.value = true
    }

    fun dismissAddQuestionDialog() {
        _showAddQuestionDialog.value = false
    }

    fun addQuestion(text: String, options: List<String>, correctIndex: Int) {
        viewModelScope.launch {
            val question = Question(
                questionBankId = bankId,
                text = text,
                options = options,
                correctIndex = correctIndex
            )
            repository.insertQuestions(listOf(question))
            _showAddQuestionDialog.value = false
        }
    }

    fun showEditQuestionDialog(question: Question) {
        _editingQuestionId.value = question.id
    }

    fun dismissEditQuestionDialog() {
        _editingQuestionId.value = null
    }

    fun updateQuestion(questionId: Long, text: String, options: List<String>, correctIndex: Int) {
        viewModelScope.launch {
            val existing = repository.getQuestionById(questionId) ?: return@launch

            val success = repository.updateQuestionWithVersionCheck(
                id = questionId,
                text = text,
                options = options,
                correctIndex = correctIndex,
                expectedVersion = existing.version
            )

            if (success) {
                _editingQuestionId.value = null
            } else {
                Log.w(TAG, "Update failed: version conflict for question $questionId")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

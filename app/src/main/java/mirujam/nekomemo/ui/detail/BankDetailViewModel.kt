package mirujam.nekomemo.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.data.repository.QuestionRepository
import mirujam.nekomemo.domain.mapper.QuestionMapper
import mirujam.nekomemo.domain.usecase.BankExportImportUseCase
import javax.inject.Inject

@HiltViewModel
class BankDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuestionRepository,
    private val questionMapper: QuestionMapper,
    private val bankExportImportUseCase: BankExportImportUseCase
) : ViewModel() {

    private val bankId: Long = savedStateHandle["bankId"] ?: -1L

    val questions: StateFlow<List<QuestionEntity>> = repository.getQuestionsForBank(bankId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson.asStateFlow()

    private val _exportFileName = MutableStateFlow("")
    val exportFileName: StateFlow<String> = _exportFileName.asStateFlow()

    private var currentBank: QuestionBankEntity? = null

    init {
        viewModelScope.launch {
            val bank = repository.getBankById(bankId)
            bank?.let {
                currentBank = it
                _bankTitle.value = it.title
                _bankCategory.value = it.category
            }
        }
    }

    fun toOptionList(optionsJson: String): List<String> {
        return questionMapper.mapJsonToOptions(optionsJson)
    }

    fun deleteQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            repository.deleteQuestion(question)
        }
    }

    fun prepareExport() {
        viewModelScope.launch {
            val json = bankExportImportUseCase.exportBankToJson(bankId)
            _exportJson.value = json
            _exportFileName.value = "${_bankTitle.value.replace(" ", "_")}.nekomemo.json"
        }
    }

    fun clearExportState() {
        _exportJson.value = null
        _exportFileName.value = ""
    }

    fun showEditDialog() {
        _showEditDialog.value = true
    }

    fun dismissEditDialog() {
        _showEditDialog.value = false
    }

    fun updateBank(title: String, category: String) {
        viewModelScope.launch {
            currentBank?.let { bank ->
                val updated = bank.copy(title = title, category = category)
                repository.updateBank(updated)
                _bankTitle.value = title
                _bankCategory.value = category
                _showEditDialog.value = false
                currentBank = updated
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
            val entity = QuestionEntity(
                questionBankId = bankId,
                text = text,
                options = questionMapper.mapOptionsToJson(options),
                correctIndex = correctIndex
            )
            repository.insertQuestions(listOf(entity))
            _showAddQuestionDialog.value = false
        }
    }

    fun showEditQuestionDialog(question: QuestionEntity) {
        _editingQuestionId.value = question.id
    }

    fun dismissEditQuestionDialog() {
        _editingQuestionId.value = null
    }

    fun updateQuestion(questionId: Long, text: String, options: List<String>, correctIndex: Int) {
        viewModelScope.launch {
            val existing = repository.getQuestionById(questionId) ?: return@launch
            val updated = existing.copy(
                text = text,
                options = questionMapper.mapOptionsToJson(options),
                correctIndex = correctIndex
            )
            repository.insertQuestions(listOf(updated))
            _editingQuestionId.value = null
        }
    }
}

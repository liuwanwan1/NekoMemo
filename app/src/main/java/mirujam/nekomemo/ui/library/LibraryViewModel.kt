package mirujam.nekomemo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.repository.QuestionRepository
import mirujam.nekomemo.domain.usecase.BankExportImportUseCase
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: QuestionRepository,
    private val bankExportImportUseCase: BankExportImportUseCase
) : ViewModel() {

    val banks: StateFlow<List<QuestionBankEntity>> = repository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionCounts: StateFlow<Map<Long, Int>> = repository.getQuestionCountsByBank()
        .map { list -> list.associate { it.bankId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson.asStateFlow()

    private val _exportFileName = MutableStateFlow("")
    val exportFileName: StateFlow<String> = _exportFileName.asStateFlow()

    private val _showDeleteConfirmDialog = MutableStateFlow(false)
    val showDeleteConfirmDialog: StateFlow<Boolean> = _showDeleteConfirmDialog.asStateFlow()

    private var pendingDeleteBank: QuestionBankEntity? = null

    fun deleteBank(bank: QuestionBankEntity) {
        pendingDeleteBank = bank
        _showDeleteConfirmDialog.value = true
    }

    fun confirmDeleteBank() {
        val bank = pendingDeleteBank ?: return
        viewModelScope.launch {
            try {
                repository.deleteBank(bank)
                _snackbarMessage.value = "Bank '${bank.title}' deleted successfully"
                android.util.Log.d("LibraryViewModel", "Deleted bank: ${bank.title}")
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Error deleting bank", e)
                _snackbarMessage.value = "Error deleting bank: ${e.message}"
            } finally {
                _showDeleteConfirmDialog.value = false
                pendingDeleteBank = null
            }
        }
    }

    fun dismissDeleteConfirmDialog() {
        _showDeleteConfirmDialog.value = false
        pendingDeleteBank = null
    }

    fun duplicateBank(bank: QuestionBankEntity) {
        viewModelScope.launch {
            val newId = bankExportImportUseCase.duplicateBank(bank.id)
            _snackbarMessage.value = if (newId > 0) "Bank duplicated" else "Failed to duplicate bank"
        }
    }

    fun prepareExport(bank: QuestionBankEntity) {
        viewModelScope.launch {
            val json = bankExportImportUseCase.exportBankToJson(bank.id)
            _exportJson.value = json
            _exportFileName.value = "${bank.title.replace(" ", "_")}.nekomemo.json"
        }
    }

    fun clearExportState() {
        _exportJson.value = null
        _exportFileName.value = ""
    }

    fun onExportError(message: String) {
        clearExportState()
        _snackbarMessage.value = message
    }

    fun importBank(jsonString: String) {
        viewModelScope.launch {
            try {
                val bankId = bankExportImportUseCase.importBankFromJson(jsonString)
                _snackbarMessage.value = if (bankId > 0) "Bank imported successfully" else "Failed to import bank"
            } catch (e: Exception) {
                _snackbarMessage.value = "Import error: ${e.message}"
            }
        }
    }

    fun onImportError(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

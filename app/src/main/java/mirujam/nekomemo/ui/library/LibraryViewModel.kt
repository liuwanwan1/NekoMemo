package mirujam.nekomemo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mirujam.nekomemo.data.local.entity.QuestionBankEntity
import mirujam.nekomemo.data.repository.QuestionRepository
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: QuestionRepository
) : ViewModel() {

    val banks: StateFlow<List<QuestionBankEntity>> = repository.getAllBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionCounts: StateFlow<Map<Long, Int>> = banks.flatMapLatest { bankList ->
        if (bankList.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                bankList.map { bank ->
                    repository.getQuestionCountForBank(bank.id)
                }
            ) { counts ->
                bankList.mapIndexed { index, bank ->
                    bank.id to counts[index]
                }.toMap()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson.asStateFlow()

    private val _exportFileName = MutableStateFlow("")
    val exportFileName: StateFlow<String> = _exportFileName.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun deleteBank(bank: QuestionBankEntity) {
        viewModelScope.launch {
            repository.deleteBank(bank)
        }
    }

    fun duplicateBank(bank: QuestionBankEntity) {
        viewModelScope.launch {
            val newId = repository.duplicateBank(bank.id)
            if (newId > 0) {
                _snackbarMessage.value = "Bank duplicated"
            } else {
                _snackbarMessage.value = "Failed to duplicate bank"
            }
        }
    }

    fun prepareExport(bank: QuestionBankEntity) {
        viewModelScope.launch {
            val json = repository.exportBankToJson(bank.id)
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
                val bankId = repository.importBankFromJson(jsonString)
                if (bankId > 0) {
                    _snackbarMessage.value = "Bank imported successfully"
                } else {
                    _snackbarMessage.value = "Failed to import bank"
                }
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

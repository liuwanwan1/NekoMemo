package mirujam.nekomemo.ui.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mirujam.nekomemo.domain.usecase.BankExportImportUseCase
import mirujam.nekomemo.util.FileNameSanitizer

data class ExportState(
    val json: String? = null,
    val fileName: String = ""
) {
    val isReady: Boolean get() = json != null && fileName.isNotBlank()
}

class ExportDelegate(
    private val scope: CoroutineScope,
    private val bankExportImportUseCase: BankExportImportUseCase
) {
    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun prepareExport(bankId: Long, bankTitle: String) {
        scope.launch {
            val json = bankExportImportUseCase.exportBankToJson(bankId)
            _exportState.value = ExportState(
                json = json,
                fileName = "${FileNameSanitizer.sanitize(bankTitle)}.nekomemo.json"
            )
        }
    }

    fun clearExportState() {
        _exportState.value = ExportState()
    }
}

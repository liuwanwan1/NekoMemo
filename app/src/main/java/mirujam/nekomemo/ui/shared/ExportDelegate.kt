package mirujam.nekomemo.ui.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mirujam.nekomemo.domain.usecase.BankExportImportUseCase
import mirujam.nekomemo.util.FileNameSanitizer

enum class ExportFormat(val extension: String, val mimeType: String) {
    JSON("nekomemo.json", "application/json"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
}

data class ExportState(
    val json: String? = null,
    val docxBytes: ByteArray? = null,
    val fileName: String = "",
    val format: ExportFormat = ExportFormat.JSON
) {
    val isReady: Boolean
        get() = when (format) {
            ExportFormat.JSON -> json != null && fileName.isNotBlank()
            ExportFormat.DOCX -> docxBytes != null && fileName.isNotBlank()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExportState) return false
        if (json != other.json) return false
        if (docxBytes != null) {
            if (other.docxBytes == null) return false
            if (!docxBytes.contentEquals(other.docxBytes)) return false
        } else if (other.docxBytes != null) return false
        if (fileName != other.fileName) return false
        if (format != other.format) return false
        return true
    }

    override fun hashCode(): Int {
        var result = json?.hashCode() ?: 0
        result = 31 * result + (docxBytes?.contentHashCode() ?: 0)
        result = 31 * result + fileName.hashCode()
        result = 31 * result + format.hashCode()
        return result
    }
}

class ExportDelegate(
    private val scope: CoroutineScope,
    private val bankExportImportUseCase: BankExportImportUseCase
) {
    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun prepareExport(bankId: Long, bankTitle: String, format: ExportFormat = ExportFormat.JSON) {
        scope.launch {
            val sanitizedTitle = FileNameSanitizer.sanitize(bankTitle)
            when (format) {
                ExportFormat.JSON -> {
                    val json = withContext(Dispatchers.IO) {
                        bankExportImportUseCase.exportBankToJson(bankId)
                    }
                    _exportState.value = ExportState(
                        json = json,
                        fileName = "$sanitizedTitle.${format.extension}",
                        format = format
                    )
                }
                ExportFormat.DOCX -> {
                    val docxBytes = withContext(Dispatchers.IO) {
                        bankExportImportUseCase.exportBankToDocx(bankId)
                    }
                    _exportState.value = ExportState(
                        docxBytes = docxBytes,
                        fileName = "$sanitizedTitle.${format.extension}",
                        format = format
                    )
                }
            }
        }
    }

    fun clearExportState() {
        _exportState.value = ExportState()
    }
}

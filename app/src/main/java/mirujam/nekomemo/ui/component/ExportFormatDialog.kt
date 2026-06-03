package mirujam.nekomemo.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mirujam.nekomemo.R
import mirujam.nekomemo.ui.shared.ExportFormat
import mirujam.nekomemo.ui.theme.AppShapes

@Composable
fun ExportFormatDialog(
    onDismiss: () -> Unit,
    onFormatSelected: (ExportFormat) -> Unit
) {
    DialogWithIcon(
        onDismiss = onDismiss,
        icon = Icons.Outlined.IosShare,
        title = stringResource(R.string.export_format_title),
        confirmText = "",
        onConfirm = {},
        dismissText = stringResource(R.string.common_cancel),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ExportFormat.entries.forEach { format ->
                    val (title, desc) = when (format) {
                        ExportFormat.JSON -> stringResource(R.string.export_format_json) to stringResource(R.string.export_format_json_desc)
                        ExportFormat.DOCX -> stringResource(R.string.export_format_docx) to stringResource(R.string.export_format_docx_desc)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.medium)
                            .clickable { onFormatSelected(format) },
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}

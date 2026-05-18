package mirujam.nekomemo.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mirujam.nekomemo.R
import mirujam.nekomemo.domain.validator.DataValidator
import mirujam.nekomemo.ui.theme.AppShapes
import mirujam.nekomemo.ui.theme.ButtonShapes
import androidx.compose.material3.MaterialTheme

@Composable
fun EditBankDialog(
    initialTitle: String,
    initialCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }

    val isTitleValid = title.isNotBlank() && title.length <= DataValidator.MAX_TITLE_LENGTH
    val isCategoryValid = category.length <= DataValidator.MAX_CATEGORY_LENGTH

    DialogWithIcon(
        onDismiss = onDismiss,
        icon = Icons.Outlined.Edit,
        title = stringResource(R.string.detail_edit_dialog_title),
        confirmButton = {
            Button(
                onClick = {
                    val trimmedTitle = title.trim().take(DataValidator.MAX_TITLE_LENGTH)
                    val trimmedCategory = category.trim().take(DataValidator.MAX_CATEGORY_LENGTH)
                    onConfirm(trimmedTitle, trimmedCategory)
                },
                enabled = isTitleValid && isCategoryValid,
                shape = ButtonShapes
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        content = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.extract_bank_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.extraSmall,
                textStyle = MaterialTheme.typography.bodyMedium,
                isError = title.length > DataValidator.MAX_TITLE_LENGTH,
                supportingText = if (title.length > DataValidator.MAX_TITLE_LENGTH) {
                    { Text("${title.length}/${DataValidator.MAX_TITLE_LENGTH}") }
                } else null
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.extract_category_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.extraSmall,
                textStyle = MaterialTheme.typography.bodyMedium,
                isError = category.length > DataValidator.MAX_CATEGORY_LENGTH,
                supportingText = if (category.length > DataValidator.MAX_CATEGORY_LENGTH) {
                    { Text("${category.length}/${DataValidator.MAX_CATEGORY_LENGTH}") }
                } else null
            )
        }
    )
}

package mirujam.nekomemo.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mirujam.nekomemo.R
import mirujam.nekomemo.data.local.entity.CategoryEntity
import mirujam.nekomemo.data.repository.CategoryRepository
import mirujam.nekomemo.domain.validator.DataValidator
import mirujam.nekomemo.ui.theme.AppShapes
import mirujam.nekomemo.ui.theme.ButtonShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBankDialog(
    initialTitle: String,
    initialCategoryId: Long,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var selectedCategoryId by remember(initialCategoryId, categories) {
        mutableLongStateOf(
            if (categories.any { it.id == initialCategoryId }) initialCategoryId
            else categories.firstOrNull()?.id ?: initialCategoryId
        )
    }
    var expanded by remember { mutableStateOf(false) }

    val isTitleValid = title.isNotBlank() && title.length <= DataValidator.MAX_TITLE_LENGTH
    val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name 
        ?: if (selectedCategoryId == 0L && categories.isNotEmpty()) categories.first().name else ""

    DialogWithIcon(
        onDismiss = onDismiss,
        icon = Icons.Outlined.Edit,
        title = stringResource(R.string.detail_edit_dialog_title),
        confirmButton = {
            Button(
                onClick = {
                    val trimmedTitle = title.trim().take(DataValidator.MAX_TITLE_LENGTH)
                    onConfirm(trimmedTitle, selectedCategoryId)
                },
                enabled = isTitleValid && selectedCategoryId > 0,
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
            Column {
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
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedCategoryId == CategoryRepository.DEFAULT_CATEGORY_NAME.hashCode().toLong()) {
                            stringResource(R.string.category_general_display)
                        } else selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.extract_category_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = AppShapes.extraSmall,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            val displayName = if (category.name == CategoryRepository.DEFAULT_CATEGORY_NAME) {
                                stringResource(R.string.category_general_display)
                            } else category.name
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }
    )
}

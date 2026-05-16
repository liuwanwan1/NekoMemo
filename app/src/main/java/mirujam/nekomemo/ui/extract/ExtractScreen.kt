package mirujam.nekomemo.ui.extract

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mirujam.nekomemo.data.model.ExtractedQuestion
import mirujam.nekomemo.navigation.Route
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.component.DialogWithIcon
import mirujam.nekomemo.ui.component.LocalSnackbarHostState
import mirujam.nekomemo.ui.theme.ButtonShapes

import androidx.compose.ui.res.stringResource
import mirujam.nekomemo.R
import mirujam.nekomemo.ui.theme.AppShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractScreen(
    onBack: () -> Unit,
    viewModel: ExtractViewModel = hiltViewModel()
) {
    val questionBank by viewModel.questionBank.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val isSaveSuccess by viewModel.isSaveSuccess.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        try {
            val jsonData = viewModel.loadFromSharedDataStore()
            if (jsonData != null) {
                Log.d("ExtractScreen", "Received JSON from SharedDataStore, length: ${jsonData.length}")
                viewModel.initFromJson(jsonData)
                val cleared = viewModel.clearSharedDataStore()
                if (cleared) {
                    Log.d("ExtractScreen", "Cleared SharedDataStore successfully")
                } else {
                    Log.w("ExtractScreen", "Failed to clear SharedDataStore")
                }
            } else {
                Log.w("ExtractScreen", "No JSON data found in SharedDataStore")
            }
        } catch (e: Exception) {
            Log.e("ExtractScreen", "Error loading data from SharedDataStore", e)
        }
    }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var bankTitle by rememberSaveable { mutableStateOf("") }
    val defaultCategory = stringResource(R.string.default_category)
    var category by rememberSaveable { mutableStateOf(defaultCategory) }

    LaunchedEffect(questionBank?.name) {
        if (questionBank != null && bankTitle.isBlank()) {
            bankTitle = questionBank!!.name
            Log.d("ExtractScreen", "Auto-filled bank title: '${questionBank!!.name}'")
        }
    }

    LaunchedEffect(saveResult) {
        saveResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveResult()
        }
    }

    LaunchedEffect(isSaveSuccess) {
        if (isSaveSuccess) {
            onBack()
            viewModel.onNavigatedBack()
        }
    }

    if (showSaveDialog) {
        DialogWithIcon(
            onDismiss = { showSaveDialog = false },
            icon = Icons.Outlined.SaveAlt,
            title = stringResource(R.string.extract_save_dialog_title),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveQuestions(bankTitle, category)
                        showSaveDialog = false
                    },
                    enabled = bankTitle.isNotBlank() && !isSaving,
                    shape = ButtonShapes
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.common_save))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    enabled = !isSaving
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            content = {
                OutlinedTextField(
                    value = bankTitle,
                    onValueChange = { bankTitle = it },
                    label = { Text(stringResource(R.string.extract_bank_title_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.extraSmall,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.extract_category_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.extraSmall,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.extract_save_summary, questionBank?.questions?.size ?: 0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Route.Extract.titleResId),
                onNavigationClick = onBack
            )
        }
    ) { paddingValues ->
        val bank = questionBank
        if (bank == null || bank.questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.extract_no_questions),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = bank.name.ifBlank { stringResource(R.string.extract_untitled_bank) },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.extract_questions_extracted, bank.questions.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (bank.unsupportedTypeCount > 0 || bank.skippedCount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val skipMessages = mutableListOf<String>()
                                    if (bank.unsupportedTypeCount > 0) {
                                        skipMessages.add(stringResource(R.string.extract_skipped_unsupported, bank.unsupportedTypeCount))
                                    }
                                    if (bank.skippedCount > 0) {
                                        skipMessages.add(stringResource(R.string.extract_skipped_no_answer, bank.skippedCount))
                                    }
                                    Text(
                                        text = skipMessages.joinToString("; "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(bank.questions) { index, question ->
                        ExtractedQuestionCard(
                            index = index,
                            question = question
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = ButtonShapes
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.extract_save_to_library))
                }
            }
        }
    }
}

@Composable
private fun ExtractedQuestionCard(
    index: Int,
    question: ExtractedQuestion
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                shape = AppShapes.extraSmall,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                val localizedType = when (question.type) {
                    "Single Choice" -> stringResource(R.string.question_type_single)
                    "Multiple Choice" -> stringResource(R.string.question_type_multiple)
                    "True/False" -> stringResource(R.string.question_type_true_false)
                    "Fill in the Blank" -> stringResource(R.string.question_type_fill_blank)
                    "Short Answer" -> stringResource(R.string.question_type_short_answer)
                    else -> question.type.ifBlank { stringResource(R.string.question_type_unknown) }
                }
                Text(
                    text = localizedType,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${index + 1}. ${question.content}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            question.options.forEachIndexed { optIndex, option ->
                val isCorrect = optIndex == question.correctIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCorrect) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (question.correctAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.extract_correct_answer, question.correctAnswer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

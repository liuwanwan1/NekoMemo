package mirujam.nekomemo.ui.detail

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.component.DialogWithIcon
import mirujam.nekomemo.ui.component.EditBankDialog
import mirujam.nekomemo.ui.component.LocalSnackbarHostState
import mirujam.nekomemo.ui.theme.ButtonShapes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.res.stringResource
import mirujam.nekomemo.R
import mirujam.nekomemo.ui.theme.AppShapes

private const val TAG = "BankDetailScreen"

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun BankDetailScreen(
    onStartTest: (Long, Int, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: BankDetailViewModel = hiltViewModel()
) {
    val cachedQuestions by viewModel.cachedQuestions.collectAsState()
    val bankTitle by viewModel.bankTitle.collectAsState()
    val bankCategory by viewModel.bankCategory.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showAddQuestionDialog by viewModel.showAddQuestionDialog.collectAsState()
    val editingQuestionId by viewModel.editingQuestionId.collectAsState()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    val showDeleteBankConfirmDialog by viewModel.showDeleteBankConfirmDialog.collectAsState()
    
    val questions by viewModel.questions.collectAsState()
    val editingQuestion = editingQuestionId?.let { id -> questions.find { it.id == id } }

    var showTestConfigDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }

    val filteredQuestions = remember(cachedQuestions, searchQuery) {
        if (searchQuery.isBlank()) cachedQuestions
        else cachedQuestions.filter {
            it.text.contains(searchQuery, ignoreCase = true)
        }
    }

    val questionsSize = questions.size
    val filteredSize = filteredQuestions.size
    val isSearchBlank = searchQuery.isBlank()
    val questionCountText = if (isSearchBlank) {
        stringResource(R.string.library_questions_count, questionsSize)
    } else {
        stringResource(R.string.detail_questions_count_filtered, filteredSize, questionsSize)
    }

    val context = LocalContext.current
    val exportJson by viewModel.exportJson.collectAsState()
    val exportFileName by viewModel.exportFileName.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current

    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exportErrorMessage) {
        exportErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            exportErrorMessage = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            val json = exportJson ?: return@let
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                exportErrorMessage = context.getString(R.string.library_delete_error, e.message ?: "Unknown error")
            }
            viewModel.clearExportState()
        }
    }

    LaunchedEffect(exportJson) {
        if (exportJson != null && exportFileName.isNotBlank()) {
            exportLauncher.launch(exportFileName)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearExportState()
        }
    }

    if (showEditDialog) {
        EditBankDialog(
            initialTitle = bankTitle,
            initialCategory = bankCategory,
            onDismiss = { viewModel.dismissEditDialog() },
            onConfirm = { title, category -> viewModel.updateBank(title, category) }
        )
    }

    if (showAddQuestionDialog) {
        QuestionEditDialog(
            title = stringResource(R.string.detail_add_dialog_title),
            initialText = "",
            initialOptions = listOf("", "", "", ""),
            initialCorrectIndex = 0,
            onDismiss = { viewModel.dismissAddQuestionDialog() },
            onConfirm = { text, options, correctIndex ->
                viewModel.addQuestion(text, options, correctIndex)
            }
        )
    }

    editingQuestion?.let { q ->
        QuestionEditDialog(
            title = stringResource(R.string.detail_edit_question_dialog_title),
            initialText = q.text,
            initialOptions = viewModel.toOptionList(q.options),
            initialCorrectIndex = q.correctIndex,
            onDismiss = { viewModel.dismissEditQuestionDialog() },
            onConfirm = { text, options, correctIndex ->
                viewModel.updateQuestion(q.id, text, options, correctIndex)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DialogWithIcon(
            onDismiss = { viewModel.dismissDeleteConfirmDialog() },
            icon = Icons.Outlined.DeleteOutline,
            title = stringResource(R.string.detail_delete_question_title),
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteQuestion() }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            content = {
                Text(stringResource(R.string.detail_delete_question_message))
            }
        )
    }

    if (showDeleteBankConfirmDialog) {
        DialogWithIcon(
            onDismiss = { viewModel.dismissDeleteBankDialog() },
            icon = Icons.Outlined.DeleteOutline,
            title = stringResource(R.string.library_delete_title),
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmDeleteBank()
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.library_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteBankDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            content = {
                Text(stringResource(R.string.library_delete_message))
            }
        )
    }

    if (showTestConfigDialog && questions.isNotEmpty()) {
        TestConfigDialog(
            totalQuestions = questions.size,
            onDismiss = { showTestConfigDialog = false },
            onStart = { count, shuffleQuestions, shuffleOptions ->
                showTestConfigDialog = false
                val bankId = questions.firstOrNull()?.questionBankId ?: return@TestConfigDialog
                Log.d(TAG, "Starting Test - bankId: $bankId, questionCount: $count, shuffleQuestions: $shuffleQuestions, shuffleOptions: $shuffleOptions, totalQuestionsAvailable: ${questions.size}")
                onStartTest(bankId, count, shuffleQuestions, shuffleOptions)
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = bankTitle,
                onNavigationClick = onBack,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                actions = {
                    IconButton(onClick = { viewModel.showAddQuestionDialog() }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.detail_add_question)
                        )
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.library_more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_edit)) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showEditDialog()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                                }
                            )
                            if (questions.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.library_export)) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.prepareExport()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.IosShare, null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    viewModel.showDeleteBankDialog()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (questions.isEmpty()) {
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
                        text = stringResource(R.string.detail_no_questions),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.showAddQuestionDialog() },
                        shape = ButtonShapes
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.detail_add_question))
                    }
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
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = bankCategory,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = questionCountText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(filteredQuestions, key = { it.id }) { question ->
                        val originalQuestion = questions.find { it.id == question.id }
                        if (originalQuestion == null) return@items

                        QuestionCard(
                            question = question,
                            optionList = question.options,
                            onEdit = { viewModel.showEditQuestionDialog(originalQuestion) },
                            onDelete = { viewModel.deleteQuestion(originalQuestion) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                Button(
                    onClick = { showTestConfigDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = ButtonShapes
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.detail_start_test))
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: mirujam.nekomemo.ui.model.CachedQuestion,
    optionList: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Row(modifier = Modifier) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.common_edit),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.common_delete),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            optionList.forEachIndexed { index, option ->
                val isCorrect = index == question.correctIndex
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
        }
    }
}

@Composable
private fun TestConfigDialog(
    totalQuestions: Int,
    onDismiss: () -> Unit,
    onStart: (Int, Boolean, Boolean) -> Unit
) {
    var useAllQuestions by remember { mutableStateOf(true) }
    var selectedCount by remember { mutableIntStateOf(totalQuestions) }
    var shuffleQuestions by remember { mutableStateOf(false) }
    var shuffleOptions by remember { mutableStateOf(false) }

    DialogWithIcon(
        onDismiss = onDismiss,
        icon = Icons.Outlined.Quiz,
        title = stringResource(R.string.detail_test_config_title),
        confirmButton = {
            Button(
                onClick = { onStart(selectedCount, shuffleQuestions, shuffleOptions) },
                shape = ButtonShapes
            ) {
                Icon(
                    imageVector = Icons.Outlined.Quiz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.detail_start_test))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.small)
                    .clickable {
                        useAllQuestions = true
                        selectedCount = totalQuestions
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = useAllQuestions,
                    onClick = {
                        useAllQuestions = true
                        selectedCount = totalQuestions
                    }
                )
                Text(text = stringResource(R.string.detail_all_questions, totalQuestions))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.small)
                    .clickable {
                        useAllQuestions = false
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !useAllQuestions,
                    onClick = { useAllQuestions = false }
                )
                Text(text = stringResource(R.string.detail_custom_count))
            }

            if (!useAllQuestions) {
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = selectedCount.toFloat(),
                    onValueChange = { selectedCount = it.toInt().coerceAtLeast(1) },
                    valueRange = 1f..totalQuestions.toFloat(),
                    steps = (totalQuestions - 2).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.detail_selected_questions_count, selectedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.small)
                    .clickable {
                        shuffleQuestions = !shuffleQuestions
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = shuffleQuestions,
                    onCheckedChange = { shuffleQuestions = it }
                )
                Text(text = stringResource(R.string.detail_shuffle_questions))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.small)
                    .clickable {
                        shuffleOptions = !shuffleOptions
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = shuffleOptions,
                    onCheckedChange = { shuffleOptions = it }
                )
                Text(text = stringResource(R.string.detail_shuffle_options))
            }
        }
    )
}

@Composable
private fun QuestionEditDialog(
    title: String,
    initialText: String,
    initialOptions: List<String>,
    initialCorrectIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, Int) -> Unit
) {
    var questionText by remember { mutableStateOf(initialText) }
    val options = remember { mutableStateListOf(*initialOptions.toTypedArray()) }
    var correctIndex by remember { mutableIntStateOf(initialCorrectIndex) }

    DialogWithIcon(
        onDismiss = onDismiss,
        icon = Icons.Outlined.Edit,
        title = title,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(questionText, options.filter { it.isNotBlank() }, correctIndex)
                },
                enabled = questionText.isNotBlank() && options.any { it.isNotBlank() },
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
                value = questionText,
                onValueChange = { questionText = it },
                placeholder = { Text(stringResource(R.string.detail_question_text_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.extraSmall,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        RadioButton(
                            selected = correctIndex == index,
                            onClick = { correctIndex = index },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = option,
                        onValueChange = { options[index] = it },
                        placeholder = { 
                            Text(
                                text = stringResource(R.string.detail_option_label, index + 1),
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.extraSmall,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                }
                if (index < options.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (options.size > 2) {
                    TextButton(onClick = { options.removeAt(options.lastIndex) }) {
                        Text(stringResource(R.string.detail_remove_last_option))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (options.size < 8) {
                    TextButton(onClick = { options.add("") }) {
                        Text(stringResource(R.string.detail_add_option))
                    }
                }
            }
        }
    )
}

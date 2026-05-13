package mirujam.nekomemo.ui.detail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.component.LocalSnackbarHostState
import mirujam.nekomemo.ui.theme.ButtonShapes
import mirujam.nekomemo.ui.theme.DialogShapes

private const val TAG = "BankDetailScreen"

@Composable
fun BankDetailScreen(
    onStartTest: (Long, Int) -> Unit,
    onBack: () -> Unit,
    viewModel: BankDetailViewModel = hiltViewModel()
) {
    // ✅ 优化：使用缓存的 CachedQuestion（已解析JSON），避免重复解析
    val cachedQuestions by viewModel.cachedQuestions.collectAsState()
    val bankTitle by viewModel.bankTitle.collectAsState()
    val bankCategory by viewModel.bankCategory.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val showAddQuestionDialog by viewModel.showAddQuestionDialog.collectAsState()
    val editingQuestionId by viewModel.editingQuestionId.collectAsState()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsState()
    
    // 编辑时需要原始 QuestionEntity，从缓存中查找
    val questions by viewModel.questions.collectAsState()
    val editingQuestion = editingQuestionId?.let { id -> questions.find { it.id == id } }

    var showTestConfigDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // ✅ 优化：对缓存数据过滤（无需再次解析JSON）
    val filteredQuestions = remember(cachedQuestions, searchQuery) {
        if (searchQuery.isBlank()) cachedQuestions
        else cachedQuestions.filter {
            it.text.contains(searchQuery, ignoreCase = true)
        }
    }

    // ✅ 优化：缓存题目数量文本，避免每次重组都重新创建字符串
    val questionCountText = remember(questions.size, filteredQuestions.size, searchQuery.isBlank()) {
        if (searchQuery.isBlank()) "${questions.size} questions"
        else "${filteredQuestions.size} of ${questions.size} questions"
    }

    val exportJson by viewModel.exportJson.collectAsState()
    val exportFileName by viewModel.exportFileName.collectAsState()
    val context = LocalContext.current
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
                exportErrorMessage = "Export failed: ${e.message}"
            }
            viewModel.clearExportState()
        }
    }

    LaunchedEffect(exportJson) {
        if (exportJson != null && exportFileName.isNotBlank()) {
            exportLauncher.launch(exportFileName)
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
            title = "Add Question",
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
            title = "Edit Question",
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
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmDialog() },
            title = { Text(text = "Delete Question?") },
            text = { Text("This question will be permanently removed. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteQuestion() }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmDialog() }) {
                    Text("Cancel")
                }
            },
            shape = DialogShapes
        )
    }

    if (showTestConfigDialog && questions.isNotEmpty()) {
        TestConfigDialog(
            totalQuestions = questions.size,
            onDismiss = { showTestConfigDialog = false },
            onStart = { count ->
                showTestConfigDialog = false
                val bankId = questions.firstOrNull()?.questionBankId ?: return@TestConfigDialog
                Log.d(TAG, "Starting Test - bankId: $bankId, questionCount: $count, totalQuestionsAvailable: ${questions.size}")
                onStartTest(bankId, count)
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = bankTitle,
                onNavigationClick = onBack,
                actions = {
                    IconButton(onClick = { viewModel.showAddQuestionDialog() }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add Question"
                        )
                    }
                    IconButton(onClick = { viewModel.showEditDialog() }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    if (questions.isNotEmpty()) {
                        IconButton(onClick = { viewModel.prepareExport() }) {
                            Icon(
                                imageVector = Icons.Outlined.IosShare,
                                contentDescription = "Export"
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
                        text = "No questions in this bank",
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
                        Text("Add Question")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (questions.size > 3) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search questions") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        singleLine = true
                    )
                }

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
                            shape = MaterialTheme.shapes.large,
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
                        // ✅ 优化：直接使用已解析的 options（List<String>），无需再次解析JSON
                        val originalQuestion = questions.find { it.id == question.id }
                        if (originalQuestion == null) return@items

                        QuestionCard(
                            question = question,
                            optionList = question.options,  // 已是 List<String>，非JSON字符串
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
                    Text("Start Test")
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
        shape = MaterialTheme.shapes.large,
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
                            contentDescription = "Edit",
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
                            contentDescription = "Delete",
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
private fun EditBankDialog(
    initialTitle: String,
    initialCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Bank") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bank Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, category) },
                enabled = title.isNotBlank(),
                shape = ButtonShapes
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = DialogShapes
    )
}

@Composable
private fun TestConfigDialog(
    totalQuestions: Int,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit
) {
    var useAllQuestions by remember { mutableStateOf(true) }
    var selectedCount by remember { mutableIntStateOf(totalQuestions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Test Configuration") },
        text = {
            Column {
                Text(
                    text = "$totalQuestions questions available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = useAllQuestions,
                        onClick = {
                            useAllQuestions = true
                            selectedCount = totalQuestions
                        }
                    )
                    Text("All questions ($totalQuestions)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !useAllQuestions,
                        onClick = { useAllQuestions = false }
                    )
                    Text("Custom count")
                }

                if (!useAllQuestions) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = selectedCount.toFloat(),
                        onValueChange = { selectedCount = it.toInt().coerceAtLeast(1) },
                        valueRange = 1f..totalQuestions.toFloat(),
                        steps = (totalQuestions - 2).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "$selectedCount questions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStart(selectedCount) },
                shape = ButtonShapes
            ) {
                Icon(
                    imageVector = Icons.Outlined.Quiz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Test")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = DialogShapes
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Question Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = correctIndex == index,
                            onClick = { correctIndex = index }
                        )
                        OutlinedTextField(
                            value = option,
                            onValueChange = { options[index] = it },
                            label = { Text("Option ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.extraSmall,
                            singleLine = true
                        )
                    }
                    if (index < options.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (options.size > 2) {
                        TextButton(onClick = { options.removeAt(options.lastIndex) }) {
                            Text("Remove Last")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    if (options.size < 8) {
                        TextButton(onClick = { options.add("") }) {
                            Text("Add Option")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(questionText, options.filter { it.isNotBlank() }, correctIndex)
                },
                enabled = questionText.isNotBlank() && options.any { it.isNotBlank() },
                shape = ButtonShapes
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = DialogShapes
    )
}

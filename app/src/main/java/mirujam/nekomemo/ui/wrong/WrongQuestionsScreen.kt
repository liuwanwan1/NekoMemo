package mirujam.nekomemo.ui.wrong

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mirujam.nekomemo.R
import mirujam.nekomemo.data.repository.WrongQuestionWithQuestion
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.theme.AppShapes

@Composable
fun WrongQuestionsScreen(
    onBack: () -> Unit,
    onStartRedo: (Long, Int) -> Unit = { _, _ -> },
    viewModel: WrongQuestionsViewModel = hiltViewModel()
) {
    val wrongQuestions by viewModel.wrongQuestions.collectAsState()
    val totalCount by viewModel.totalUnresolvedCount.collectAsState()
    val selectedBankId by viewModel.selectedBankId.collectAsState()
    val unresolvedBankIds by viewModel.unresolvedBankIds.collectAsState()
    val banks by viewModel.banks.collectAsState()

    var filterExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.wrong_questions_title),
                onNavigationClick = onBack,
                actions = {
                    if (unresolvedBankIds.size > 1) {
                        Box {
                            IconButton(onClick = { filterExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = filterExpanded,
                                onDismissRequest = { filterExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部题库") },
                                    onClick = {
                                        viewModel.selectBank(null)
                                        filterExpanded = false
                                    },
                                    trailingIcon = {
                                        if (selectedBankId == null) {
                                            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )
                                unresolvedBankIds.forEach { bankId ->
                                    val bank = banks.find { it.id == bankId }
                                    if (bank != null) {
                                        DropdownMenuItem(
                                            text = { Text(bank.title) },
                                            onClick = {
                                                viewModel.selectBank(bankId)
                                                filterExpanded = false
                                            },
                                            trailingIcon = {
                                                if (selectedBankId == bankId) {
                                                    Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (wrongQuestions.isEmpty()) {
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
                        text = stringResource(R.string.wrong_questions_empty),
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
                // Header with count and redo button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = AppShapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.wrong_questions_count, totalCount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val bankId = selectedBankId ?: unresolvedBankIds.firstOrNull()
                                if (bankId != null) {
                                    val count = wrongQuestions.size
                                    onStartRedo(bankId, count)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("重做错题")
                        }
                    }
                }

                // List of wrong questions with answers
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = wrongQuestions,
                        key = { it.wrongQuestion.id }
                    ) { wrongQuestionWithQuestion ->
                        WrongQuestionCard(
                            wrongQuestionWithQuestion = wrongQuestionWithQuestion,
                            onMarkResolved = {
                                viewModel.markResolved(wrongQuestionWithQuestion.wrongQuestion.id)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun WrongQuestionCard(
    wrongQuestionWithQuestion: WrongQuestionWithQuestion,
    onMarkResolved: () -> Unit
) {
    val wrongQuestion = wrongQuestionWithQuestion.wrongQuestion
    val question = wrongQuestionWithQuestion.question

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = AppShapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = question.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.wrong_questions_wrong_count, wrongQuestion.wrongCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.wrong_questions_last_wrong, formatTime(wrongQuestion.lastWrongAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onMarkResolved) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = stringResource(R.string.wrong_questions_mark_resolved),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Show options with correct answer highlighted
            Spacer(modifier = Modifier.height(8.dp))
            question.options.forEachIndexed { index, option ->
                val isCorrect = index in question.correctIndices
                val optionLetter = ('A' + index).toString()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$optionLetter. $option",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> "${diff / (7 * 24 * 60 * 60 * 1000)}周前"
    }
}

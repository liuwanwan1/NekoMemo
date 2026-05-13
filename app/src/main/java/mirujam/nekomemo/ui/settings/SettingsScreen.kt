package mirujam.nekomemo.ui.settings

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mirujam.nekomemo.data.preferences.ThemeMode
import mirujam.nekomemo.navigation.Route
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.component.DialogWithIcon
import mirujam.nekomemo.ui.theme.ButtonShapes

import androidx.compose.ui.res.stringResource
import mirujam.nekomemo.R

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showWebViewClearDialog by remember { mutableStateOf(false) }
    val bankCount by viewModel.bankCount.collectAsState()
    val totalQuestionCount by viewModel.totalQuestionCount.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()
    val directAnswer by viewModel.directAnswer.collectAsState()

    if (showClearDialog) {
        DialogWithIcon(
            onDismiss = { showClearDialog = false },
            icon = Icons.Outlined.DeleteOutline,
            title = stringResource(R.string.settings_clear_db_title),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    shape = ButtonShapes
                ) {
                    Text(stringResource(R.string.settings_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            content = {
                Text(stringResource(R.string.settings_clear_db_message))
            }
        )
    }

    if (showWebViewClearDialog) {
        DialogWithIcon(
            onDismiss = { showWebViewClearDialog = false },
            icon = Icons.Outlined.CleaningServices,
            title = stringResource(R.string.settings_clear_webview_title),
            confirmButton = {
                val context = LocalContext.current
                Button(
                    onClick = {
                        clearWebViewData(context)
                        showWebViewClearDialog = false
                        Toast.makeText(context, context.getString(R.string.settings_clear_webview_success), android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = ButtonShapes
                ) {
                    Text(stringResource(R.string.settings_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebViewClearDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            content = {
                Text(stringResource(R.string.settings_clear_webview_message))
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Route.Settings.titleResId)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(
                title = stringResource(R.string.settings_appearance),
                icon = Icons.Outlined.DarkMode
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = currentTheme == mode
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                        }
                        Button(
                            onClick = { viewModel.setThemeMode(mode) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = if (isSelected) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            border = if (!isSelected) {
                                ButtonDefaults.outlinedButtonBorder(enabled = true)
                            } else {
                                null
                            }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            SettingsCard(
                title = stringResource(R.string.settings_test_settings),
                icon = Icons.Outlined.Quiz
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_direct_answer),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_direct_answer_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = directAnswer,
                        onCheckedChange = { viewModel.setDirectAnswer(it) }
                    )
                }
            }

            SettingsCard(
                title = stringResource(R.string.settings_statistics),
                icon = Icons.Outlined.Storage
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$bankCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_banks),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalQuestionCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.settings_questions),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SettingsCard(
                title = stringResource(R.string.settings_data_management),
                icon = Icons.Outlined.CleaningServices
            ) {
                Column {
                    SettingsActionItem(
                        title = stringResource(R.string.settings_local_db),
                        description = stringResource(R.string.settings_local_db_desc),
                        actionLabel = stringResource(R.string.settings_clear),
                        onClick = { showClearDialog = true },
                        isDestructive = true
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    SettingsActionItem(
                        title = stringResource(R.string.settings_webview_data),
                        description = stringResource(R.string.settings_webview_data_desc),
                        actionLabel = stringResource(R.string.settings_clean),
                        onClick = { showWebViewClearDialog = true }
                    )
                }
            }

            SettingsCard(
                title = stringResource(R.string.settings_about),
                icon = Icons.Outlined.Info
            ) {
                val uriHandler = LocalUriHandler.current
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_about_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_author),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "JamGmilk",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_open_source),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_github),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://github.com/JamGmilk/NekoMemo")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.settings_version, "1.0.0"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            colors = if (isDestructive) {
                ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.textButtonColors()
            }
        ) {
            Text(
                text = actionLabel,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

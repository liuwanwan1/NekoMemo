package mirujam.nekomemo.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mirujam.nekomemo.BuildConfig
import mirujam.nekomemo.R
import mirujam.nekomemo.data.preferences.ThemeMode
import mirujam.nekomemo.navigation.Route
import mirujam.nekomemo.ui.component.AppTopBar
import mirujam.nekomemo.ui.component.DialogWithIcon
import mirujam.nekomemo.ui.theme.AppShapes
import mirujam.nekomemo.ui.theme.ButtonShapes
import androidx.core.net.toUri
import android.net.Uri
import android.provider.Settings

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
    val context = LocalContext.current

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
                Button(
                    onClick = {
                        clearWebViewData(context)
                        showWebViewClearDialog = false
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.settings_clear_webview_success),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
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
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                            shape = AppShapes.medium,
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
                                text = stringResource(mode.labelResId()),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SettingsCard(
                    title = stringResource(R.string.settings_language),
                    icon = Icons.Outlined.Translate
                ) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_language_desc))
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
                icon = Icons.Outlined.QueryStats
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
                icon = Icons.Outlined.Storage
            ) {
                DataManagementCardContent(
                    onClearDatabase = { showClearDialog = true },
                    onClearWebViewData = { showWebViewClearDialog = true }
                )
            }

            SettingsCard(
                title = stringResource(R.string.settings_about),
                icon = Icons.Outlined.Info
            ) {
                AboutCardContent(
                    onOpenSourceClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/JamGmilk/NekoMemo".toUri())
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeMode.labelResId(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@Composable
private fun AboutCardContent(
    onOpenSourceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = AppShapes.extraLarge
                ),
            shape = AppShapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        )
                        Text(
                            text = "JamGmilk",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOpenSourceClick,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.large,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_open_source),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowOutward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DataManagementCardContent(
    onClearDatabase: () -> Unit,
    onClearWebViewData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_local_db),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Button(
            onClick = onClearDatabase,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_clear_database))
        }
        Text(
            text = stringResource(R.string.settings_local_db_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_webview_data),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Button(
            onClick = onClearWebViewData,
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        ) {
            Icon(
                imageVector = Icons.Outlined.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.settings_clear_cache_cookies))
        }
        Text(
            text = stringResource(R.string.settings_webview_data_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        shape = AppShapes.large,
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

package mirujam.nekomemo.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mirujam.nekomemo.ui.theme.ButtonShapes
import mirujam.nekomemo.ui.theme.DialogShapes

@Composable
fun DialogWithIcon(
    onDismiss: () -> Unit,
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    dismissText: String? = null,
    dismissEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShapes,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        colors.primary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
            ) {
                content()
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                shape = ButtonShapes,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (isDestructive) colors.onError else colors.onPrimary
                    )
                } else {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            if (dismissText != null) {
                TextButton(
                    onClick = onDismiss,
                    enabled = dismissEnabled
                ) {
                    Text(dismissText)
                }
            }
        }
    )
}

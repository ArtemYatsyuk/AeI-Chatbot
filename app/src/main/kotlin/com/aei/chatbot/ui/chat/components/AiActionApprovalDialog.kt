package com.aei.chatbot.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aei.chatbot.domain.model.AiAction
import com.aei.chatbot.domain.model.PendingAiAction

@Composable
fun AiActionApprovalDialog(
    pending: PendingAiAction,
    onApprove: (PendingAiAction) -> Unit,
    onReject: (PendingAiAction) -> Unit
) {
    Dialog(
        onDismissRequest = { onReject(pending) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(24.dp)) {

                // Header icon + title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4A30BB), Color(0xFF8B6FFF))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            actionIcon(pending.action),
                            null,
                            Modifier.size(22.dp),
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            "AeI wants to perform an action",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Review before allowing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Action detail card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionTypeChip(pending.action)

                        when (val a = pending.action) {
                            is AiAction.OpenApp -> {
                                DetailRow(Icons.Default.SmartToy, "App", a.appLabel)
                                DetailRow(Icons.Default.Code, "Package", a.packageName)
                            }
                            is AiAction.CreateFile -> {
                                DetailRow(Icons.Default.InsertDriveFile, "File", a.fileName)
                                DetailRow(Icons.Default.Category, "Type", a.mimeType)
                                if (a.content.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Preview:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            a.content.take(200) + if (a.content.length > 200) "\n…" else "",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            is AiAction.OpenUrl -> {
                                DetailRow(Icons.Default.Link, "URL", a.url)
                                if (a.title.isNotBlank() && a.title != a.url) {
                                    DetailRow(Icons.Default.Label, "Title", a.title)
                                }
                            }
                            is AiAction.OpenMap -> {
                                DetailRow(Icons.Default.Place, "Location", a.query)
                                if (a.label.isNotBlank() && a.label != a.query) {
                                    DetailRow(Icons.Default.Label, "Label", a.label)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Warning note
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        "You can enable auto-approve in Beta Features settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReject(pending) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Deny", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onApprove(pending) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Allow", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTypeChip(action: AiAction) {
    val (label, color) = when (action) {
        is AiAction.OpenApp    -> "Open App"    to Color(0xFF4CAF50)
        is AiAction.CreateFile -> "Create File" to Color(0xFF2196F3)
        is AiAction.OpenUrl    -> "Open URL"    to Color(0xFFFF9800)
        is AiAction.OpenMap    -> "Open Map"    to Color(0xFF00BCD4)
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text(
            "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}

private fun actionIcon(action: AiAction): ImageVector = when (action) {
    is AiAction.OpenApp    -> Icons.Default.OpenInNew
    is AiAction.CreateFile -> Icons.Default.NoteAdd
    is AiAction.OpenUrl    -> Icons.Default.Language
    is AiAction.OpenMap    -> Icons.Default.Place
}
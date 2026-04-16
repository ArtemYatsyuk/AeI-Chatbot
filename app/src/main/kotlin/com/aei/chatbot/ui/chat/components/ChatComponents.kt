package com.aei.chatbot.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.aei.chatbot.R
import com.aei.chatbot.domain.model.WebSearchResult

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dots")
    Row(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.aei_is_thinking), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.width(4.dp))
        repeat(3) { idx ->
            val d = idx * 150
            val a by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = d), RepeatMode.Reverse), label = "a$idx")
            val s by inf.animateFloat(0.7f, 1.2f, infiniteRepeatable(tween(600, delayMillis = d), RepeatMode.Reverse), label = "s$idx")
            Box(Modifier.size(6.dp).scale(s).background(MaterialTheme.colorScheme.primary.copy(alpha = a), CircleShape))
        }
    }
}

@Composable
fun SearchingIndicator(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "search")
    val a by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "sa")
    Row(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = a))
        Text("🔍 Searching the web...", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = a))
    }
}

@Composable
fun ChatInputBar(
    value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit, onStop: () -> Unit, onMicClick: () -> Unit,
    onSearchToggle: () -> Unit, isStreaming: Boolean, isSearching: Boolean,
    webSearchActive: Boolean, modifier: Modifier = Modifier
) {
    val sendEnabled = value.text.isNotBlank() && !isStreaming && !isSearching
    val animScale by animateFloatAsState(if (sendEnabled) 1f else 0.9f, spring(Spring.DampingRatioMediumBouncy), label = "sc")

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            AnimatedVisibility(visible = isSearching) { SearchingIndicator(Modifier.padding(start = 16.dp)) }

            // Search mode indicator
            AnimatedVisibility(visible = webSearchActive && !isSearching) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.web_search_mode_on), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                // Mic
                IconButton(onClick = onMicClick, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Mic, stringResource(R.string.cd_voice_input), tint = MaterialTheme.colorScheme.primary)
                }
                // Search toggle
                IconButton(onClick = onSearchToggle, modifier = Modifier.size(44.dp)
                    .then(if (webSearchActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)) {
                    Icon(Icons.Default.Search, stringResource(R.string.cd_web_search),
                        tint = if (webSearchActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                // Input
                OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions(onSend = { if (sendEnabled) onSend() }),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.width(8.dp))
                if (isStreaming || isSearching) {
                    IconButton(onClick = onStop, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.error, CircleShape)) {
                        Icon(Icons.Default.Stop, stringResource(R.string.cd_stop), tint = Color.White)
                    }
                } else {
                    IconButton(onClick = { if (sendEnabled) onSend() }, enabled = sendEnabled,
                        modifier = Modifier.size(48.dp).scale(animScale).background(
                            if (sendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                        Icon(Icons.Default.ArrowUpward, stringResource(R.string.cd_send),
                            tint = if (sendEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatTopBar(sessionName: String, onSessionNameChange: (String) -> Unit, onTranslateClick: () -> Unit,
    onNewChatClick: () -> Unit, onHistoryClick: () -> Unit, onSettingsClick: () -> Unit) {
    var editingName by remember { mutableStateOf(false) }
    var nameValue by remember(sessionName) { mutableStateOf(sessionName) }
    TopAppBar(title = {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("AeI", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            if (editingName) {
                OutlinedTextField(value = nameValue, onValueChange = { if (it.length <= 40) nameValue = it },
                    singleLine = true, modifier = Modifier.weight(1f).height(48.dp),
                    keyboardActions = KeyboardActions(onDone = { onSessionNameChange(nameValue); editingName = false }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
            } else {
                Text(sessionName, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { editingName = true }))
            }
        }
    }, actions = {
        IconButton(onClick = onNewChatClick) { Icon(Icons.Default.AddComment, stringResource(R.string.cd_new_chat)) }
        IconButton(onClick = onHistoryClick) { Icon(Icons.Default.History, stringResource(R.string.cd_history)) }
        IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, stringResource(R.string.cd_settings)) }
    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
}


@Composable
fun SearchResultsCard(
    results: List<WebSearchResult>,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "${results.size} sources found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }

            // Always show first result title as preview
            if (!expanded && results.isNotEmpty()) {
                Text(
                    results.first().title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    results.forEachIndexed { idx, result ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.url)))
                                } catch (_: Exception) {}
                            }
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${idx + 1}.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        result.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                }
                                if (result.snippet.isNotBlank()) {
                                    Text(
                                        result.snippet.take(120) + if (result.snippet.length > 120) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 2.dp, start = 16.dp)
                                    )
                                }
                                Text(
                                    result.url.removePrefix("https://").removePrefix("http://").take(50),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 2.dp, start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
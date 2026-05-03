package com.aei.chatbot.ui.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aei.chatbot.R
import com.aei.chatbot.domain.model.WebSearchResult
import com.aei.chatbot.ui.theme.LocalAeIColors

// ─── Typing indicator ─────────────────────────────────────────────────────────
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dots")
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(3) { idx ->
            val delay = idx * 180
            val a by inf.animateFloat(
                0.3f, 1f,
                infiniteRepeatable(tween(500, delayMillis = delay, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "dot$idx"
            )
            val s by inf.animateFloat(
                0.7f, 1.15f,
                infiniteRepeatable(tween(500, delayMillis = delay, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "ds$idx"
            )
            Box(
                Modifier.size(7.dp).scale(s)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = a), CircleShape)
            )
        }
    }
}

// ─── Searching indicator ──────────────────────────────────────────────────────
@Composable
fun SearchingIndicator(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "search_pulse")
    val a by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse), label = "sa")
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 1.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = a))
        Text(stringResource(R.string.searching_web),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = a))
    }
}

// ─── Chat Input Bar ───────────────────────────────────────────────────────────
@Composable
fun ChatInputBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMicClick: () -> Unit,
    onSearchToggle: () -> Unit,
    isStreaming: Boolean,
    isSearching: Boolean,
    webSearchActive: Boolean,
    modifier: Modifier = Modifier,
    onImageClick: () -> Unit = {},
    pendingImageUri: android.net.Uri? = null,
    showImageButton: Boolean = false,
    onClearImage: () -> Unit = {},
    webSearchEnabled: Boolean = true
) {
    val colors = LocalAeIColors.current
    val sendEnabled = value.text.isNotBlank() && !isStreaming && !isSearching
    val animScale by animateFloatAsState(
        if (sendEnabled || isStreaming) 1f else 0.9f,
        spring(Spring.DampingRatioMediumBouncy), label = "sc"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Image preview
        AnimatedVisibility(visible = pendingImageUri != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Image, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.image_attached_label),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClearImage, Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Web search active pill
        AnimatedVisibility(visible = webSearchActive && webSearchEnabled && !isSearching) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.TravelExplore, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.web_search_mode_on),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Searching row
        AnimatedVisibility(visible = isSearching) {
            SearchingIndicator(Modifier.padding(start = 4.dp))
        }

        // Main input card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            tonalElevation = 0.dp
        ) {
            Column {
                // Text field
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(R.string.chat_input_placeholder),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.bodyMedium)
                    },
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Default,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (sendEnabled) onSend() }),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = 0.dp, bottomEnd = 0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    )
                )

                // Bottom toolbar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showImageButton) {
                        IconButton(onImageClick, Modifier.size(36.dp)) {
                            Icon(Icons.Default.Image, "Attach image", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    if (webSearchEnabled) {
                        IconButton(
                            onClick = onSearchToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                stringResource(R.string.cd_web_search),
                                Modifier.size(18.dp),
                                tint = if (webSearchActive) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                    IconButton(onMicClick, Modifier.size(36.dp)) {
                        Icon(Icons.Default.Mic, stringResource(R.string.cd_voice_input),
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }

                    Spacer(Modifier.weight(1f))

                    Box(
                        Modifier.size(34.dp).scale(animScale)
                            .background(
                                if (sendEnabled || isStreaming || isSearching)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .clickable(enabled = sendEnabled || isStreaming || isSearching) {
                                if (isStreaming || isSearching) onStop() else if (sendEnabled) onSend()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isStreaming || isSearching) Icons.Default.Stop else Icons.Default.ArrowUpward,
                            null,
                            Modifier.size(16.dp),
                            tint = if (sendEnabled || isStreaming || isSearching) Color.White
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Chat Top Bar ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatTopBar(
    sessionName: String,
    onSessionNameChange: (String) -> Unit,
    onTranslateClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    currentModel: String = "",
    onModelClick: () -> Unit = {}
) {
    var editing by remember { mutableStateOf(false) }
    var nameVal by remember(sessionName) { mutableStateOf(sessionName) }

    TopAppBar(
        title = {
            if (editing) {
                OutlinedTextField(
                    value = nameVal,
                    onValueChange = { if (it.length <= 40) nameVal = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    keyboardActions = KeyboardActions(onDone = {
                        onSessionNameChange(nameVal); editing = false
                    }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("AeI", fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp)

                    if (currentModel.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onModelClick() }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                                Text(
                                    currentModel.substringAfterLast("/").take(28),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.UnfoldMore, null, Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onNewChatClick) {
                Icon(Icons.Default.AddComment, stringResource(R.string.cd_new_chat), Modifier.size(20.dp))
            }
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, stringResource(R.string.cd_history), Modifier.size(20.dp))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, stringResource(R.string.cd_settings), Modifier.size(20.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    )
}

// ─── Search Results Card ──────────────────────────────────────────────────────
@Composable
fun SearchResultsCard(
    results: List<WebSearchResult>,
    modifier: Modifier = Modifier,
    autoTriggered: Boolean = false
) {
    if (results.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.TravelExplore, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text("${results.size} ${stringResource(R.string.sources_found)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold)
                    if (autoTriggered) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(stringResource(R.string.search_auto_badge),
                                Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }

            if (!expanded && results.isNotEmpty()) {
                Text(results.first().title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp))
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    results.forEachIndexed { idx, r ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url)))
                                }
                            }
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // ✅ FIXED: was Surface(color, shape, modifier) positionally
                                    // — now uses named parameters
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(r.title, style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                if (r.snippet.isNotBlank()) {
                                    Text(r.snippet.take(130) + if (r.snippet.length > 130) "…" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 2.dp, start = 24.dp))
                                }
                                Text(r.url.removePrefix("https://").removePrefix("http://").take(48),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp, start = 24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
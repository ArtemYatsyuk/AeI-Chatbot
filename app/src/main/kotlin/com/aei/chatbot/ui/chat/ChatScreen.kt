package com.aei.chatbot.ui.chat

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aei.chatbot.R
import com.aei.chatbot.domain.model.ChatMessage
import com.aei.chatbot.domain.model.WebSearchResult
import com.aei.chatbot.ui.chat.components.*
import com.aei.chatbot.ui.theme.fontSizeMultiplier
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(chatId: String?, onNavigateToHistory: () -> Unit, onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var contextMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.handleSpeechResult(it) }

    LaunchedEffect(Unit) { viewModel.speechResult.collect { inputValue = TextFieldValue(inputValue.text + it) } }
    LaunchedEffect(chatId) { if (chatId != null) viewModel.loadChat(chatId) }
    LaunchedEffect(uiState.messages.size, uiState.streamingMessage.length) {
        if (settings.autoScroll) {
            val total = uiState.messages.size + (if (uiState.isStreaming) 1 else 0)
            if (total > 0) try { listState.animateScrollToItem(total - 1) } catch (_: Exception) {}
        }
    }

    if (showModelPicker) {
        AlertDialog(
            onDismissRequest = { showModelPicker = false },
            title = { Text("Select Model") },
            text = {
                Column {
                    Text("Current: ${settings.selectedModel.ifBlank { "Not set" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                    Text("To change models, go to Settings > Model tab.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPicker = false; onNavigateToSettings() }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelPicker = false }) { Text("Close") }
            }
        )
    }

    if (showNewChatDialog) {
        AlertDialog(onDismissRequest = { showNewChatDialog = false },
            title = { Text(stringResource(R.string.chat_new_session_confirm)) },
            confirmButton = { TextButton(onClick = { showNewChatDialog = false; viewModel.createNewSession() }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { showNewChatDialog = false }) { Text(stringResource(R.string.cancel)) } })
    }
    if (showContextMenu && contextMessage != null) {
        val msg = contextMessage!!
        ModalBottomSheet(onDismissRequest = { showContextMenu = false; contextMessage = null }) {
            Column(Modifier.padding(16.dp)) {
                ListItem(headlineContent = { Text(stringResource(R.string.context_copy_text)) }, leadingContent = { Icon(Icons.Default.ContentCopy, null) },
                    modifier = Modifier.clickable { com.aei.chatbot.util.ClipboardUtils.copyToClipboard(context, "message", msg.content); showContextMenu = false })
                ListItem(headlineContent = { Text(stringResource(R.string.context_delete)) }, leadingContent = { Icon(Icons.Default.Delete, null) },
                    modifier = Modifier.clickable { viewModel.deleteMessage(msg.id); showContextMenu = false })
                ListItem(headlineContent = { Text(stringResource(R.string.context_share)) }, leadingContent = { Icon(Icons.Default.Share, null) },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, msg.content) }, null))
                        showContextMenu = false
                    })
            }
        }
    }

    Scaffold(
        topBar = { ChatTopBar(uiState.sessionName, viewModel::updateSessionName, {},
            { if (uiState.messages.isEmpty()) viewModel.createNewSession() else showNewChatDialog = true },
            onNavigateToHistory, onNavigateToSettings) },
        bottomBar = {
            ChatInputBar(value = inputValue, onValueChange = { inputValue = it },
                onSend = { val t = inputValue.text; inputValue = TextFieldValue(""); viewModel.sendMessage(t) },
                onStop = viewModel::stopStreaming,
                onMicClick = {
                    if (micPermission.status.isGranted) {
                        try { speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, settings.voiceInputLanguage)
                        }) } catch (_: Exception) {}
                    } else micPermission.launchPermissionRequest()
                },
                onSearchToggle = viewModel::toggleWebSearch,
                isStreaming = uiState.isStreaming, isSearching = uiState.isSearching,
                webSearchActive = uiState.webSearchActive)
        }
    ) { pv ->
        Box(Modifier.fillMaxSize().padding(pv)) {
            Column(Modifier.fillMaxSize()) {
                AnimatedVisibility(uiState.error != null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            Row { TextButton(onClick = { viewModel.dismissError() }) { Text(stringResource(R.string.ok)) }
                                TextButton(onClick = onNavigateToSettings) { Text(stringResource(R.string.chat_error_go_to_settings)) } }
                        }
                    }
                }
                if (uiState.messages.isEmpty() && !uiState.isStreaming && !uiState.isSearching) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.chat_empty_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.chat_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    val fs = fontSizeMultiplier(settings.fontSize)
                    LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            MessageBubble(msg, settings.showTimestamps, settings.showAvatars, settings.userInitials, settings.avatarColor, fs, settings.bubbleStyle,
                                onLongPress = { contextMessage = it; showContextMenu = true })
                        }
                        if (uiState.isSearching) { item(key = "searching") { SearchingIndicator() } }
                        if (uiState.isStreaming) {
                            item(key = "streaming_response") {
                                if (uiState.streamingMessage.isEmpty()) TypingIndicator()
                                else MessageBubble(ChatMessage("streaming", "", "assistant", uiState.streamingMessage, null, System.currentTimeMillis(), isStreaming = true),
                                    false, settings.showAvatars, settings.userInitials, settings.avatarColor, fs, settings.bubbleStyle, {}, isStreamingBubble = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

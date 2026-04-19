package com.aei.chatbot.ui.chat

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var contextMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    val editingMessageId = uiState.editingMessageId
    val editingContent = uiState.editingMessageContent
    var showModelPicker by remember { mutableStateOf(false) }
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> viewModel.attachImage(uri) }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.handleSpeechResult(it) }

    LaunchedEffect(Unit) {
        viewModel.speechResult.collect { text ->
            if (text.startsWith("Chat exported")) {
                snackbarHostState.showSnackbar(text)
            } else {
                inputValue = TextFieldValue(inputValue.text + text)
            }
        }
    }
    LaunchedEffect(chatId) { if (chatId != null) viewModel.loadChat(chatId) }
    LaunchedEffect(uiState.messages.size, uiState.streamingMessage.length) {
        if (settings.autoScroll) {
            val total = uiState.messages.size + (if (uiState.isStreaming) 1 else 0)
            if (total > 0) try { listState.animateScrollToItem(total - 1) } catch (_: Exception) {}
        }
    }

        if (showModelPicker) {
        val quickModelIds = settings.quickModels
                val allModels = settings.providers.flatMap { it.models }
                val quickModelConfigs = quickModelIds.map { qmId -> allModels.find { it.modelId == qmId } }
                val hasQuickModels = quickModelIds.size >= 2
        ModalBottomSheet(
            onDismissRequest = { showModelPicker = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Model", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showModelPicker = false; onNavigateToSettings() }) {
                        Text("Manage")
                    }
                }
                HorizontalDivider()

                // Quick Models section (shown first if available)
                if (hasQuickModels) {
                    Text("Quick Models",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))

                    quickModelIds.forEach { qmId ->
                        val model = allModels.find { it.modelId == qmId }
                        val displayName = model?.displayName ?: qmId
                        val isSelected = settings.selectedModel == qmId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateSelectedModel(qmId) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FlashOn, null, Modifier.size(20.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(displayName, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                if (displayName != qmId) {
                                    Text(qmId, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                }

                if (allModels.isEmpty() && settings.selectedModel.isBlank()) {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.SmartToy, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        Text("No models configured", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.height(4.dp))
                        Text("Go to Settings > Model to add one", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                } else {
                    // Current model text input for manual entry
                    OutlinedTextField(
                        value = settings.selectedModel,
                        onValueChange = { viewModel.updateSelectedModel(it) },
                        label = { Text("Model ID") },
                        placeholder = { Text("Type or select below") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.SmartToy, null, Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (settings.selectedModel.isNotBlank()) {
                                IconButton(onClick = { viewModel.updateSelectedModel("") }) {
                                    Icon(Icons.Default.Clear, null, Modifier.size(18.dp))
                                }
                            }
                        }
                    )

                    // Model list from providers
                    if (allModels.isNotEmpty()) {
                        Text("Configured Models",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp))

                        allModels.forEach { model ->
                            val isSelected = settings.selectedModel == model.modelId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateSelectedModel(model.modelId)
                                    }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(36.dp).background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                        CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SmartToy, null, Modifier.size(18.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(model.displayName, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                    Text(model.modelId, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Old model picker removed
    if (false) {
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

    // Edit Message Dialog
    if (editingMessageId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEditMessage() },
            title = { Text("Edit Message", fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = editingContent,
                    onValueChange = { viewModel.updateEditingContent(it) },
                    label = { Text("Your message") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmEditMessage() },
                    enabled = editingContent.isNotBlank()
                ) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEditMessage() }) { Text("Cancel") }
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
                ListItem(
                    headlineContent = { Text("Edit Message") },
                    leadingContent = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.clickable {
                        val msg = contextMessage
                        if (msg != null && msg.role == "user") {
                            viewModel.startEditMessage(msg)
                        }
                        showContextMenu = false
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.context_delete)) },
                    leadingContent = { Icon(Icons.Default.Delete, null) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                sessionName = uiState.sessionName,
                onSessionNameChange = viewModel::updateSessionName,
                onTranslateClick = {},
                onNewChatClick = { if (uiState.messages.isEmpty()) viewModel.createNewSession() else showNewChatDialog = true },
                onHistoryClick = onNavigateToHistory,
                onSettingsClick = onNavigateToSettings,
                currentModel = if (settings.quickModels.size >= 2) settings.selectedModel else "",
                onModelClick = { showModelPicker = true },
                onExportClick = { viewModel.exportCurrentChat() }
            )
        },
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
                webSearchActive = uiState.webSearchActive,
                onImageClick = { imagePickerLauncher.launch("image/*") },
                showImageButton = settings.providers.flatMap { it.models }
                    .any { it.modelId == settings.selectedModel && it.capabilities.vision },
                pendingImageUri = uiState.pendingImageUri,
                onClearImage = { viewModel.clearImage() }
            )
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


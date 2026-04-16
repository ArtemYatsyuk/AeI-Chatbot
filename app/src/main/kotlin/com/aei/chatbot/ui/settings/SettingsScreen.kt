package com.aei.chatbot.ui.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aei.chatbot.BuildConfig
import com.aei.chatbot.R
import com.aei.chatbot.ui.theme.avatarColorFromString
import com.aei.chatbot.ui.theme.fontSizeMultiplier
import com.aei.chatbot.util.Constants

data class SettingsTab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        SettingsTab("Provider", Icons.Default.Cloud),
        SettingsTab("Model", Icons.Default.SmartToy),
        SettingsTab("Search", Icons.Default.Search),
        SettingsTab("Chat", Icons.Default.Chat),
        SettingsTab("General", Icons.Default.Settings),
        SettingsTab("About", Icons.Default.Info)
    )

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { snackbarHostState.showSnackbar(it); viewModel.dismissSnackbar() }
    }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSystemPromptInfo by remember { mutableStateOf(false) }

    if (showClearAllDialog) {
        AlertDialog(onDismissRequest = { showClearAllDialog = false },
            title = { Text(stringResource(R.string.settings_clear_all_confirm1)) },
            text = { Text(stringResource(R.string.settings_clear_all_confirm2)) },
            confirmButton = { TextButton(onClick = { showClearAllDialog = false; viewModel.clearAllChats() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { showClearAllDialog = false }) { Text(stringResource(R.string.cancel)) } })
    }
    if (showResetDialog) {
        AlertDialog(onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_settings)) },
            text = { Text(stringResource(R.string.settings_reset_confirm)) },
            confirmButton = { TextButton(onClick = { showResetDialog = false; viewModel.resetSettings() }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) } })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back)) } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label, fontSize = 12.sp) },
                        icon = { Icon(tab.icon, null, Modifier.size(18.dp)) }
                    )
                }
            }

            HorizontalDivider()

            // Tab Content
            when (selectedTab) {
                0 -> ProviderTab(settings, uiState, viewModel, context)
                1 -> ModelTab(settings, uiState, viewModel)
                2 -> WebSearchTab(settings, viewModel)
                3 -> ChatSettingsTab(settings, viewModel, showSystemPromptInfo, { showSystemPromptInfo = it })
                4 -> GeneralSettingsTab(settings, uiState, viewModel, context, { showClearAllDialog = true }, { showResetDialog = true })
                5 -> AboutTab(context)
            }
        }
    }
}

// ── TAB 1: PROVIDER ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderTab(
    settings: com.aei.chatbot.domain.model.AppSettings,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    context: android.content.Context
) {
    var showApiKey by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SettingsSectionHeader("Connection Mode")

        val connModes = listOf(
            "local" to "Local (LM Studio)",
            "ngrok" to "Ngrok Tunnel",
            "cloud" to "Cloud API"
        )
        DropdownSettingRow(label = "Connection Mode", options = connModes,
            selectedKey = settings.connectionMode, onSelect = viewModel::updateConnectionMode)

        AnimatedVisibility(visible = settings.connectionMode == "local") {
            Column {
                var ipError by remember { mutableStateOf("") }
                OutlinedTextField(value = settings.serverIp,
                    onValueChange = { v ->
                        ipError = if (v.isBlank()) "IP cannot be empty" else ""
                        viewModel.updateServerIp(v)
                    },
                    label = { Text(stringResource(R.string.settings_server_ip)) },
                    isError = ipError.isNotEmpty(),
                    supportingText = { if (ipError.isNotEmpty()) Text(ipError) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(12.dp))
                var portError by remember { mutableStateOf("") }
                OutlinedTextField(value = settings.serverPort.toString(),
                    onValueChange = { v ->
                        val port = v.toIntOrNull()
                        portError = if (port == null || port < 1 || port > 65535) "Invalid port" else ""
                        if (port != null) viewModel.updatePort(port)
                    },
                    label = { Text(stringResource(R.string.settings_port)) },
                    isError = portError.isNotEmpty(),
                    supportingText = { if (portError.isNotEmpty()) Text(portError) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp))
            }
        }

        AnimatedVisibility(visible = settings.connectionMode != "local") {
            Column {
                OutlinedTextField(value = settings.remoteUrl, onValueChange = viewModel::updateRemoteUrl,
                    label = { Text(if (settings.connectionMode == "cloud") "API Host" else "Ngrok URL") },
                    placeholder = { Text(if (settings.connectionMode == "cloud") "https://api.example.com" else "https://xxxx.ngrok-free.app") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Link, null) })
                if (settings.connectionMode == "cloud") {
                    val preview = "${settings.remoteUrl.trimEnd('/')}/${settings.apiEndpoint.trimStart('/')}"
                    if (settings.remoteUrl.isNotBlank()) {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Full URL:", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                Text(preview, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        SettingsSectionHeader("API Endpoint")
        OutlinedTextField(value = settings.apiEndpoint, onValueChange = viewModel::updateApiEndpoint,
            label = { Text(stringResource(R.string.settings_api_endpoint)) },
            supportingText = { Text("Default: v1/chat/completions") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp))

        SettingsSectionHeader("Authentication")
        OutlinedTextField(value = settings.apiKey, onValueChange = viewModel::updateApiKey,
            label = { Text(stringResource(R.string.settings_api_key)) },
            placeholder = { Text(if (settings.connectionMode == "cloud") "nvapi-... or sk-..." else "Optional") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Key, null) },
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            })

        SettingsSectionHeader("Connection")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.settings_timeout), style = MaterialTheme.typography.bodyMedium)
                Text("${settings.timeoutSeconds}s", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = settings.timeoutSeconds.toFloat(), onValueChange = { viewModel.updateTimeout(it.toInt()) },
                valueRange = Constants.MIN_TIMEOUT.toFloat()..Constants.MAX_TIMEOUT.toFloat())
        }

        SettingsToggleRow(label = stringResource(R.string.settings_streaming),
            description = stringResource(R.string.settings_streaming_desc),
            checked = settings.streamingEnabled, onCheckedChange = viewModel::updateStreamingEnabled)

        // Test Connection
        Button(onClick = viewModel::testConnection,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
            colors = when (uiState.connectionStatus) {
                ConnectionStatus.SUCCESS -> ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ConnectionStatus.FAILURE -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else -> ButtonDefaults.buttonColors()
            }) {
            when (uiState.connectionStatus) {
                ConnectionStatus.TESTING -> { CircularProgressIndicator(Modifier.size(20.dp), Color.White, 2.dp); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.settings_testing)) }
                ConnectionStatus.SUCCESS -> { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.settings_connected)) }
                ConnectionStatus.FAILURE -> { Icon(Icons.Default.Error, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.settings_connection_failed)) }
                ConnectionStatus.IDLE -> { Icon(Icons.Default.Wifi, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.settings_test_connection)) }
            }
        }
        if (uiState.connectionStatus == ConnectionStatus.FAILURE && uiState.connectionError.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(uiState.connectionError, Modifier.padding(12.dp), MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ── TAB 2: MODEL ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelTab(
    settings: com.aei.chatbot.domain.model.AppSettings,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val allModels = settings.providers.flatMap { it.models }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<com.aei.chatbot.domain.model.ModelConfig?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }
    var testingModel by remember { mutableStateOf<com.aei.chatbot.domain.model.ModelConfig?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Models") },
            text = { Text("Remove all configured models? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { viewModel.resetProviderModels(); showResetConfirm = false },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } })
    }

    if (showAddDialog) {
        ModelEditDialog(model = null,
            onSave = { viewModel.addModel(it); showAddDialog = false },
            onDismiss = { showAddDialog = false })
    }

    editingModel?.let { model ->
        ModelEditDialog(model = model,
            onSave = { viewModel.updateModel(it); editingModel = null },
            onDismiss = { editingModel = null })
    }

    if (showTestDialog && testingModel != null) {
        AlertDialog(onDismissRequest = { showTestDialog = false; testingModel = null },
            title = { Text("Test Model: ${testingModel!!.displayName}") },
            text = {
                Column {
                    if (uiState.isTestingModel) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Testing connection...")
                        }
                    } else {
                        Text(uiState.testModelResult.ifBlank { "Press Test to check connectivity." })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { testingModel?.let { viewModel.testModel(it) } }) { Text("Test") }
            },
            dismissButton = { TextButton(onClick = { showTestDialog = false; viewModel.dismissTestModelDialog(); testingModel = null }) { Text("Close") } })
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Action buttons
        SettingsSectionHeader("Model Management")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("NEW")
            }
            OutlinedButton(onClick = { viewModel.fetchModels() }, modifier = Modifier.weight(1f),
                enabled = !uiState.isLoadingModels) {
                if (uiState.isLoadingModels) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text("FETCH")
            }
            OutlinedButton(onClick = { showResetConfirm = true }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("RESET")
            }
        }

        // Default model selection
        SettingsSectionHeader("Active Model")
        if (settings.connectionMode == "local" && uiState.availableModels.isNotEmpty()) {
            var modelExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = if (uiState.isLoadingModels) "Loading..."
                    else settings.selectedModel.ifBlank { stringResource(R.string.settings_select_model) },
                    onValueChange = {}, readOnly = true,
                    label = { Text("Active Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                    uiState.availableModels.forEach { model ->
                        DropdownMenuItem(text = { Text(model) }, onClick = { viewModel.updateSelectedModel(model); modelExpanded = false })
                    }
                }
            }
        } else {
            OutlinedTextField(value = settings.selectedModel, onValueChange = viewModel::updateSelectedModel,
                label = { Text("Model ID") },
                placeholder = { Text(stringResource(R.string.settings_model_id_hint)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                singleLine = true, shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.SmartToy, null) },
                trailingIcon = { if (settings.selectedModel.isNotBlank()) IconButton(onClick = { viewModel.updateSelectedModel("") }) { Icon(Icons.Default.Clear, null) } })
        }

        // Configured models list
        if (allModels.isNotEmpty()) {
            SettingsSectionHeader("Configured Models (${allModels.size})")
            allModels.forEach { model ->
                ModelCard(model = model,
                    isSelected = settings.selectedModel == model.modelId,
                    onSelect = { viewModel.updateSelectedModel(model.modelId) },
                    onEdit = { editingModel = model },
                    onDelete = { viewModel.deleteModel(model.id) },
                    onTest = { testingModel = model; showTestDialog = true; viewModel.testModel(model) })
            }
        } else if (!uiState.isLoadingModels) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SmartToy, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text("No models configured", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(Modifier.height(4.dp))
                    Text("Tap NEW to add a model or FETCH to load from provider",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ModelCard(
    model: com.aei.chatbot.domain.model.ModelConfig,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, null, Modifier.size(20.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(model.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(model.modelId, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (isSelected) Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            // Capability chips
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (model.capabilities.chat) CapabilityChip("Chat")
                if (model.capabilities.vision) CapabilityChip("Vision")
                if (model.capabilities.tools) CapabilityChip("Tools")
                if (model.capabilities.imageGeneration) CapabilityChip("Images")
                if (model.capabilities.audio) CapabilityChip("Audio")
            }
            // Context info
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text("Context: ${model.contextWindow}t", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.width(8.dp))
                Text("Max out: ${model.maxOutputTokens}t", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            // Action row
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSelect, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Select", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Edit", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onTest, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text("Test", style = MaterialTheme.typography.labelSmall) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CapabilityChip(label: String) {
    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelEditDialog(
    model: com.aei.chatbot.domain.model.ModelConfig?,
    onSave: (com.aei.chatbot.domain.model.ModelConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember { mutableStateOf(model?.displayName ?: "") }
    var modelId by remember { mutableStateOf(model?.modelId ?: "") }
    var contextWindow by remember { mutableStateOf((model?.contextWindow ?: 4096).toString()) }
    var maxOutput by remember { mutableStateOf((model?.maxOutputTokens ?: 2048).toString()) }
    var capChat by remember { mutableStateOf(model?.capabilities?.chat ?: true) }
    var capVision by remember { mutableStateOf(model?.capabilities?.vision ?: false) }
    var capTools by remember { mutableStateOf(model?.capabilities?.tools ?: false) }
    var capImages by remember { mutableStateOf(model?.capabilities?.imageGeneration ?: false) }
    var capAudio by remember { mutableStateOf(model?.capabilities?.audio ?: false) }
    var temperature by remember { mutableStateOf(model?.temperature ?: 0.7f) }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (model == null) "Add Model" else "Edit Model") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it },
                    label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = modelId, onValueChange = { modelId = it },
                    label = { Text("Model ID") }, placeholder = { Text("e.g. gpt-4.1") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contextWindow, onValueChange = { contextWindow = it },
                    label = { Text("Context Window (tokens)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = maxOutput, onValueChange = { maxOutput = it },
                    label = { Text("Max Output Tokens") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Text("Capabilities:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = capChat, onClick = { capChat = !capChat }, label = { Text("Chat") })
                    FilterChip(selected = capVision, onClick = { capVision = !capVision }, label = { Text("Vision") })
                    FilterChip(selected = capTools, onClick = { capTools = !capTools }, label = { Text("Tools") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = capImages, onClick = { capImages = !capImages }, label = { Text("Images") })
                    FilterChip(selected = capAudio, onClick = { capAudio = !capAudio }, label = { Text("Audio") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Temperature: ${"%.1f".format(temperature)}", style = MaterialTheme.typography.bodySmall)
                }
                Slider(value = temperature, onValueChange = { temperature = (it * 10).toInt() / 10f }, valueRange = 0f..2f)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (modelId.isNotBlank()) {
                    onSave(com.aei.chatbot.domain.model.ModelConfig(
                        id = model?.id ?: java.util.UUID.randomUUID().toString(),
                        displayName = displayName.ifBlank { modelId },
                        modelId = modelId,
                        capabilities = com.aei.chatbot.domain.model.ModelCapabilities(capChat, capVision, capImages, capAudio, capTools),
                        contextWindow = contextWindow.toIntOrNull() ?: 4096,
                        maxOutputTokens = maxOutput.toIntOrNull() ?: 2048,
                        temperature = temperature
                    ))
                }
            }, enabled = modelId.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ── TAB 3: WEB SEARCH ────────────────────────────────────────────────────────
@Composable
fun WebSearchTab(
    settings: com.aei.chatbot.domain.model.AppSettings,
    viewModel: SettingsViewModel
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // Master toggle
        SettingsSectionHeader("Web Search")
        SettingsToggleRow(
            label = stringResource(R.string.settings_web_search_enabled),
            description = "Allow AI to search the web for up-to-date information",
            checked = settings.webSearchEnabled,
            onCheckedChange = viewModel::updateWebSearchEnabled
        )

        AnimatedVisibility(visible = settings.webSearchEnabled) {
            Column {

                // Search mode
                SettingsSectionHeader("Search Mode")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(8.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { viewModel.updateWebSearchMode("manual") }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.webSearchMode == "manual", onClick = { viewModel.updateWebSearchMode("manual") })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Manual", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Only when you tap the search button in chat", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 8.dp))
                        Row(Modifier.fillMaxWidth().clickable { viewModel.updateWebSearchMode("auto") }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = settings.webSearchMode == "auto", onClick = { viewModel.updateWebSearchMode("auto") })
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Auto", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("AI decides when to search based on query (coming soon)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                // SearXNG config
                SettingsSectionHeader("SearXNG Instance")
                OutlinedTextField(value = settings.searxngUrl, onValueChange = viewModel::updateSearxngUrl,
                    label = { Text(stringResource(R.string.settings_searxng_url)) },
                    placeholder = { Text("https://your-searxng.example.com") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) })
                Text("Self-hosted or public SearXNG instance. The URL must support ?format=json",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp))

                // Result limits
                SettingsSectionHeader("Result Settings")
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Max Results Per Search", style = MaterialTheme.typography.bodyMedium)
                            Text("How many results to send to the AI", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Text("${settings.webSearchResultCount}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = settings.webSearchResultCount.toFloat(),
                        onValueChange = { viewModel.updateWebSearchResultCount(it.toInt()) },
                        valueRange = 1f..10f, steps = 8)
                }

                // Safe search
                SettingsSectionHeader("Safe Search")
                val safeSearchOptions = listOf("off" to "Off", "moderate" to "Moderate", "strict" to "Strict")
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    safeSearchOptions.forEach { (key, label) ->
                        FilterChip(selected = key == "moderate", onClick = { },
                            label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }

                // How it works
                SettingsSectionHeader("How It Works")
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("1", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                            Text("Tap the 🔍 search toggle button next to the microphone in the chat input bar.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("2", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                            Text("Type your message and send. AeI will search the web first.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                            Text("Search results are injected into the AI context. Sources are shown as a collapsible card.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ── TAB 4: CHAT SETTINGS ─────────────────────────────────────────────────────
@Composable
fun ChatSettingsTab(
    settings: com.aei.chatbot.domain.model.AppSettings,
    viewModel: SettingsViewModel,
    showPromptInfo: Boolean,
    onShowPromptInfo: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (showPromptInfo) {
            AlertDialog(onDismissRequest = { onShowPromptInfo(false) },
                title = { Text("System Prompt") },
                text = { Text("The system prompt defines the AI personality and behavior. It is sent at the start of every conversation as a hidden instruction to the model.") },
                confirmButton = { TextButton(onClick = { onShowPromptInfo(false) }) { Text("OK") } })
        }

        // System Prompt
        SettingsSectionHeader("Persona / System Prompt")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.Top) {
            OutlinedTextField(value = settings.systemPrompt,
                onValueChange = { if (it.length <= Constants.MAX_SYSTEM_PROMPT_LENGTH) viewModel.updateSystemPrompt(it) },
                label = { Text("System Prompt") }, modifier = Modifier.weight(1f),
                minLines = 5, maxLines = 10,
                supportingText = { Text("${settings.systemPrompt.length}/${Constants.MAX_SYSTEM_PROMPT_LENGTH}") },
                shape = RoundedCornerShape(12.dp))
            Column {
                IconButton(onClick = { onShowPromptInfo(true) }) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                IconButton(onClick = {
                    viewModel.updateSystemPrompt("You are AeI, a helpful, friendly, and intelligent AI assistant.")
                }) {
                    Icon(Icons.Default.RestartAlt, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        // Response Parameters
        SettingsSectionHeader("Response Parameters")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Max Output Tokens", style = MaterialTheme.typography.bodyMedium)
                    Text("Maximum length of AI response", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Text("${settings.maxTokens}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(value = settings.maxTokens.toFloat(), onValueChange = {
                val snapped = (it / Constants.TOKEN_STEP).toInt() * Constants.TOKEN_STEP
                viewModel.updateMaxTokens(snapped.coerceIn(Constants.MIN_TOKENS, Constants.MAX_TOKENS))
            }, valueRange = Constants.MIN_TOKENS.toFloat()..Constants.MAX_TOKENS.toFloat())
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Temperature", style = MaterialTheme.typography.bodyMedium)
                    Text("Higher = more creative, Lower = more focused", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Text("%.1f".format(settings.temperature), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(value = settings.temperature, onValueChange = { viewModel.updateTemperature((it * 10).toInt() / 10f) },
                valueRange = Constants.MIN_TEMP..Constants.MAX_TEMP)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Focused", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Text("Creative", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }

        // Streaming
        SettingsSectionHeader("Streaming")
        SettingsToggleRow(label = "Enable Streaming Responses",
            description = "Show AI response token-by-token as it generates",
            checked = settings.streamingEnabled, onCheckedChange = viewModel::updateStreamingEnabled)

        // Conversation behavior
        SettingsSectionHeader("Conversation Behavior")
        SettingsToggleRow(label = "Auto-scroll to Bottom",
            description = "Automatically scroll as new tokens arrive",
            checked = settings.autoScroll, onCheckedChange = viewModel::updateAutoScroll)
        SettingsToggleRow(label = "Clear on New Session",
            description = "Automatically clear context when starting a new chat",
            checked = settings.clearOnNewSession, onCheckedChange = viewModel::updateClearOnNewSession)

        // Message display
        SettingsSectionHeader("Message Display")
        SettingsToggleRow(label = "Show Timestamps",
            description = "Display time next to each message",
            checked = settings.showTimestamps, onCheckedChange = viewModel::updateShowTimestamps)
        SettingsToggleRow(label = "Show Avatars",
            description = "Show user and AI avatar icons in chat",
            checked = settings.showAvatars, onCheckedChange = viewModel::updateShowAvatars)

        // Bubble style
        SettingsSectionHeader("Chat Bubble Style")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            val bubbleStyles = listOf(
                Constants.BUBBLE_ROUNDED to "Rounded",
                Constants.BUBBLE_SHARP to "Sharp",
                Constants.BUBBLE_MINIMAL to "Minimal"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bubbleStyles.forEach { (key, label) ->
                    FilterChip(selected = settings.bubbleStyle == key,
                        onClick = { viewModel.updateBubbleStyle(key) }, label = { Text(label) })
                }
            }
        }

        // Haptics
        SettingsSectionHeader("Feedback")
        SettingsToggleRow(label = "Haptic Feedback",
            description = "Vibrate on send and receive",
            checked = settings.hapticFeedback, onCheckedChange = viewModel::updateHapticFeedback)
        SettingsToggleRow(label = "Sound Effects",
            description = "Play subtle sounds",
            checked = settings.soundEffects, onCheckedChange = viewModel::updateSoundEffects)

        Spacer(Modifier.height(16.dp))
    }
}

// ── TAB 5: GENERAL SETTINGS ──────────────────────────────────────────────────
@Composable
fun GeneralSettingsTab(
    settings: com.aei.chatbot.domain.model.AppSettings,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    context: android.content.Context,
    onClearAll: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // ── Appearance ──
        SettingsSectionHeader("Appearance")
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Theme", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            val themeModes = listOf(
                Constants.THEME_SYSTEM to "System",
                Constants.THEME_LIGHT to "Light",
                Constants.THEME_DARK to "Dark"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themeModes.forEach { (key, label) ->
                    FilterChip(selected = settings.themeMode == key,
                        onClick = { viewModel.updateThemeMode(key) }, label = { Text(label) })
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsToggleRow(label = "Dynamic Color (Material You)",
                description = "Use wallpaper-based colors (Android 12+)",
                checked = settings.dynamicColor, onCheckedChange = viewModel::updateDynamicColor)
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("Font Size", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            val fontSizes = listOf(
                Constants.FONT_SMALL to "S",
                Constants.FONT_MEDIUM to "M",
                Constants.FONT_LARGE to "L",
                Constants.FONT_XL to "XL"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fontSizes.forEach { (key, label) ->
                    FilterChip(selected = settings.fontSize == key,
                        onClick = { viewModel.updateFontSize(key) }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Preview: The quick brown fox jumps over the lazy dog.",
                fontSize = (14 * fontSizeMultiplier(settings.fontSize)).sp,
                color = MaterialTheme.colorScheme.onSurface)
        }

        // ── Avatar ──
        SettingsSectionHeader("Your Avatar")
        OutlinedTextField(value = settings.userInitials,
            onValueChange = { if (it.length <= 2) viewModel.updateUserInitials(it) },
            label = { Text("Initials (max 2 chars)") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true, shape = RoundedCornerShape(12.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Avatar Color", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("violet", "teal", "rose", "amber", "blue", "green").forEach { colorName ->
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(avatarColorFromString(colorName))
                        .border(width = if (settings.avatarColor == colorName) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                        .clickable { viewModel.updateAvatarColor(colorName) })
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Language ──
        SettingsSectionHeader("Language & Translation")
        val languages = listOf(
            "en-US" to "English (US)", "en-GB" to "English (UK)",
            "uk" to "Українська", "cs" to "Čeština", "zh-CN" to "简体中文"
        )
        DropdownSettingRow(label = "App Display Language", options = languages,
            selectedKey = settings.appLanguage,
            onSelect = { key ->
                viewModel.updateAppLanguage(key)
                AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(key))
            })
        val translationOptions = listOf("" to "None (disabled)") + languages
        DropdownSettingRow(label = "Translate AI Responses To",
            options = translationOptions, selectedKey = settings.translationLanguage,
            onSelect = viewModel::updateTranslationLanguage)
        DropdownSettingRow(label = "Voice Input Language",
            options = languages, selectedKey = settings.voiceInputLanguage,
            onSelect = viewModel::updateVoiceInputLanguage)
        SettingsToggleRow(label = "Auto-Detect Input Language",
            description = "Detect language of your messages automatically",
            checked = settings.autoDetectLanguage, onCheckedChange = viewModel::updateAutoDetectLanguage)

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Data & Privacy ──
        SettingsSectionHeader("Data & Storage")
        OutlinedButton(onClick = viewModel::exportChats,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Export All Chats")
        }
        OutlinedButton(onClick = { },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Icon(Icons.Default.FileUpload, null); Spacer(Modifier.width(8.dp)); Text("Import Chats")
        }
        Button(onClick = onClearAll,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Clear All Chats")
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Reset ──
        SettingsSectionHeader("Reset")
        OutlinedButton(onClick = onReset,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Reset All Settings to Default")
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── TAB 6: ABOUT ─────────────────────────────────────────────────────────────
@Composable
fun AboutTab(context: android.content.Context) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // App hero
        Column(modifier = Modifier.fillMaxWidth().background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.surface))).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(96.dp).background(
                androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF03DAC6))),
                CircleShape), contentAlignment = Alignment.Center) {
                Text("AeI", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
            Text("AeI", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text("Your Intelligent Companion", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                Text("v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        // Description
        SettingsSectionHeader("What is AeI?")
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text("AeI is an open-source Android AI chatbot that connects to any OpenAI-compatible API — including LM Studio (local), NVIDIA NIM, and cloud providers. Chat with powerful language models, use real-time web search, and fully customize your experience.",
                modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }

        // Features
        SettingsSectionHeader("Features")
        val features = listOf(
            Icons.Default.Chat to "Multi-turn conversation with streaming",
            Icons.Default.Search to "Web search via SearXNG",
            Icons.Default.Psychology to "Thinking mode for reasoning models",
            Icons.Default.Language to "On-device translation (ML Kit)",
            Icons.Default.Mic to "Voice input (Speech-to-Text)",
            Icons.Default.Cloud to "Cloud & local model support",
            Icons.Default.History to "Persistent chat history"
        )
        features.forEach { (icon, desc) ->
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(desc, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Tech stack
        SettingsSectionHeader("Technology Stack")
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TechRow("Language", "Kotlin 2.0")
                TechRow("UI", "Jetpack Compose + Material 3")
                TechRow("Architecture", "MVVM + Clean Architecture")
                TechRow("DI", "Hilt")
                TechRow("Database", "Room")
                TechRow("Network", "Retrofit2 + OkHttp3 + SSE")
                TechRow("Translation", "Google ML Kit")
            }
        }

        // Links
        SettingsSectionHeader("Links & Support")
        ListItem(headlineContent = { Text("View on GitHub") },
            supportingContent = { Text("Source code & documentation") },
            leadingContent = { Icon(Icons.Default.OpenInBrowser, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable {
                try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(Constants.GITHUB_URL))) } catch (_: Exception) {}
            })
        ListItem(headlineContent = { Text("Privacy Policy") },
            supportingContent = { Text("How we handle your data") },
            leadingContent = { Icon(Icons.Default.PrivacyTip, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable {
                try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(Constants.PRIVACY_POLICY_URL))) } catch (_: Exception) {}
            })
        ListItem(headlineContent = { Text("Report an Issue") },
            supportingContent = { Text("github.com/aei-chatbot/aei/issues") },
            leadingContent = { Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable {
                try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("${Constants.GITHUB_URL}/issues"))) } catch (_: Exception) {}
            })

        // Legal
        SettingsSectionHeader("Legal Disclaimer")
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
            Text("AI-generated content may be inaccurate, incomplete, or misleading. AeI is not responsible for content generated by third-party AI models or APIs. Always verify important information from authoritative sources. Use AeI responsibly.",
                modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }

        // Credits
        SettingsSectionHeader("Credits")
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Built with ❤️ using open source libraries.", style = MaterialTheme.typography.bodySmall)
                Text("LM Studio • NVIDIA NIM • Google ML Kit • Retrofit • Hilt • Room",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun TechRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ── REUSABLE COMPOSABLES ──────────────────────────────────────────────────────
@Composable
fun SettingsSectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
fun SettingsToggleRow(label: String, description: String = "", checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (description.isNotEmpty()) Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSettingRow(label: String, options: List<Pair<String, String>>, selectedKey: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedKey }?.second ?: options.firstOrNull()?.second ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(value = selectedLabel, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(key); expanded = false }) }
        }
    }
}

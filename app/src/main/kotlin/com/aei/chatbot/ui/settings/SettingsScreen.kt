package com.aei.chatbot.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
        SettingsTab(stringResource(R.string.tab_provider), Icons.Default.Cloud),
        SettingsTab(stringResource(R.string.tab_model),    Icons.Default.SmartToy),
        SettingsTab(stringResource(R.string.tab_search),   Icons.Default.Search),
        SettingsTab(stringResource(R.string.tab_chat),     Icons.Default.Chat),
        SettingsTab(stringResource(R.string.tab_general),  Icons.Default.Settings),
        SettingsTab(stringResource(R.string.tab_about),    Icons.Default.Info),
        SettingsTab(stringResource(R.string.tab_beta_features), Icons.Default.AutoAwesome)
    )

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSystemPromptInfo by remember { mutableStateOf(false) }

    if (showClearAllDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_all_confirm1),
            body = stringResource(R.string.settings_clear_all_confirm2),
            confirmLabel = stringResource(R.string.delete),
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = { showClearAllDialog = false; viewModel.clearAllChats() },
            onDismiss = { showClearAllDialog = false }
        )
    }
    if (showResetDialog) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset_settings),
            body = stringResource(R.string.settings_reset_confirm),
            confirmLabel = stringResource(R.string.confirm),
            onConfirm = { showResetDialog = false; viewModel.resetSettings() },
            onDismiss = { showResetDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

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
                        modifier = Modifier.height(56.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(tab.icon, null, Modifier.size(18.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(tab.label, fontSize = 11.sp)
                        }
                    }
                }
            }

            HorizontalDivider()

            when (selectedTab) {
                0 -> ProviderTab(settings, uiState, viewModel, context)
                1 -> ModelTab(settings, uiState, viewModel)
                2 -> WebSearchTab(settings, viewModel)
                3 -> ChatSettingsTab(settings, viewModel, showSystemPromptInfo) { showSystemPromptInfo = it }
                4 -> GeneralSettingsTab(settings, uiState, viewModel, context, { showClearAllDialog = true }, { showResetDialog = true })
                5 -> AboutTab(context)
                6 -> FeaturesTab(settings, viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2 · MODEL (with Search)
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// TAB 1 · PROVIDER
// ─────────────────────────────────────────────────────────────────────────────

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

        SectionHeader("Connection Mode", Icons.Default.Hub)

        val connModes = listOf(
            "local" to "Local (LM Studio)",
            "ngrok" to "Ngrok Tunnel",
            "cloud" to "Cloud API"
        )

        DropdownSettingRow(
            label = "Connection Mode",
            options = connModes,
            selectedKey = settings.connectionMode,
            onSelect = viewModel::updateConnectionMode
        )

        AnimatedVisibility(
            visible = settings.connectionMode == "local",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                var ipError by remember { mutableStateOf("") }
                var portError by remember { mutableStateOf("") }

                ValidatedTextField(
                    value = settings.serverIp,
                    onValueChange = { v ->
                        ipError = if (v.isBlank()) "IP address cannot be empty" else ""
                        viewModel.updateServerIp(v)
                    },
                    label = stringResource(R.string.settings_server_ip),
                    error = ipError,
                    keyboardType = KeyboardType.Uri,
                    leadingIcon = Icons.Default.Dns
                )

                ValidatedTextField(
                    value = settings.serverPort.toString(),
                    onValueChange = { v ->
                        val port = v.toIntOrNull()
                        portError = when {
                            port == null -> "Must be a number"
                            port !in 1..65535 -> "Port must be 1–65535"
                            else -> ""
                        }
                        if (port != null && portError.isEmpty()) {
                            viewModel.updatePort(port)
                        }
                    },
                    label = stringResource(R.string.settings_port),
                    error = portError,
                    keyboardType = KeyboardType.Number,
                    leadingIcon = Icons.Default.SettingsEthernet
                )
            }
        }

        AnimatedVisibility(
            visible = settings.connectionMode != "local",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                OutlinedTextField(
                    value = settings.remoteUrl,
                    onValueChange = viewModel::updateRemoteUrl,
                    label = { Text(if (settings.connectionMode == "cloud") "API Host" else "Ngrok URL") },
                    placeholder = {
                        Text(
                            if (settings.connectionMode == "cloud") "https://api.example.com"
                            else "https://xxxx.ngrok-free.app"
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Link, null) }
                )

                if (settings.connectionMode == "cloud" && settings.remoteUrl.isNotBlank()) {
                    val preview = "${settings.remoteUrl.trimEnd('/')}/${settings.apiEndpoint.trimStart('/')}"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Column {
                                Text(
                                    "Full request URL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                                Text(
                                    preview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        SectionHeader("API Endpoint", Icons.Default.Directions)

        OutlinedTextField(
            value = settings.apiEndpoint,
            onValueChange = viewModel::updateApiEndpoint,
            label = { Text(stringResource(R.string.settings_api_endpoint)) },
            supportingText = { Text("Default: v1/chat/completions") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        )

        SectionHeader("Authentication", Icons.Default.Lock)

        OutlinedTextField(
            value = settings.apiKey,
            onValueChange = viewModel::updateApiKey,
            label = { Text(stringResource(R.string.settings_api_key)) },
            placeholder = { Text(if (settings.connectionMode == "cloud") "nvapi-... or sk-..." else "Optional") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Key, null) },
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        SectionHeader("Timeout", Icons.Default.Timer)

        SliderRow(
            label = stringResource(R.string.settings_timeout),
            value = settings.timeoutSeconds.toFloat(),
            range = Constants.MIN_TIMEOUT.toFloat()..Constants.MAX_TIMEOUT.toFloat(),
            display = "${settings.timeoutSeconds}s",
            onChange = { viewModel.updateTimeout(it.toInt()) }
        )

        SectionHeader("Options", Icons.Default.Tune)

        ToggleRow(
            label = stringResource(R.string.settings_streaming),
            description = stringResource(R.string.settings_streaming_desc),
            checked = settings.streamingEnabled,
            onChange = viewModel::updateStreamingEnabled,
            icon = Icons.Default.Stream
        )

        Spacer(Modifier.height(8.dp))

        val buttonColor = when (uiState.connectionStatus) {
            ConnectionStatus.SUCCESS -> Color(0xFF2E7D32)
            ConnectionStatus.FAILURE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }

        val animatedColor by animateColorAsState(buttonColor, label = "btnColor")

        Button(
            onClick = viewModel::testConnection,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = animatedColor)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (uiState.connectionStatus) {
                    ConnectionStatus.TESTING -> {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Text(stringResource(R.string.settings_testing), color = Color.White)
                    }
                    ConnectionStatus.SUCCESS -> {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                        Text(stringResource(R.string.settings_connected), color = Color.White)
                    }
                    ConnectionStatus.FAILURE -> {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color.White)
                        Text(stringResource(R.string.settings_connection_failed), color = Color.White)
                    }
                    ConnectionStatus.IDLE -> {
                        Icon(Icons.Default.Wifi, null, tint = Color.White)
                        Text(stringResource(R.string.settings_test_connection), color = Color.White)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.connectionStatus == ConnectionStatus.FAILURE && uiState.connectionError.isNotEmpty()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    Text(
                        uiState.connectionError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelTab(
    settings  : com.aei.chatbot.domain.model.AppSettings,
    uiState   : SettingsUiState,
    viewModel : SettingsViewModel
) {
    val allModels = settings.providers.flatMap { it.models }
    var modelSearchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingModel by remember { mutableStateOf<com.aei.chatbot.domain.model.ModelConfig?>(null) }
    var showTestDialog by remember { mutableStateOf(false) }
    var testingModel by remember { mutableStateOf<com.aei.chatbot.domain.model.ModelConfig?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val filteredModels = if (modelSearchQuery.isBlank()) allModels
    else allModels.filter {
        it.displayName.contains(modelSearchQuery, ignoreCase = true) ||
        it.modelId.contains(modelSearchQuery, ignoreCase = true)
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = "Reset Models",
            body = "Remove all configured models? This cannot be undone.",
            confirmLabel = "Reset",
            confirmColor = MaterialTheme.colorScheme.error,
            onConfirm = { viewModel.resetProviderModels(); showResetConfirm = false },
            onDismiss = { showResetConfirm = false }
        )
    }
    if (showAddDialog) {
        ModelEditDialog(
            model = null,
            onSave = { viewModel.addModel(it); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
    editingModel?.let { model ->
        ModelEditDialog(
            model = model,
            onSave = { viewModel.updateModel(it); editingModel = null },
            onDismiss = { editingModel = null }
        )
    }
    if (showTestDialog && testingModel != null) {
        val testSuccess = uiState.testModelResult.startsWith("Connected")
        AlertDialog(
            onDismissRequest = { showTestDialog = false; testingModel = null },
            title = { Text("Test · ${testingModel!!.displayName}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.isTestingModel) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Sending test request…")
                        }
                    } else {
                        Text(uiState.testModelResult.ifBlank { "Tap Test to check connectivity." })
                    }
                    // Auto-apply info card shown after a successful test
                    if (testSuccess && !uiState.isTestingModel) {
                        HorizontalDivider()
                        Text(
                            "Auto-detected settings applied:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val m = testingModel!!
                        Text("• Context: ${if (m.contextWindow > 0) m.contextWindow.toString() + " tokens" else "unknown"}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("• Max Output: ${if (m.maxOutputTokens > 0) m.maxOutputTokens.toString() + " tokens" else "unknown"}",
                            style = MaterialTheme.typography.bodySmall)
                        val caps = buildList {
                            if (m.capabilities.chat)            add("Chat")
                            if (m.capabilities.vision)          add("Vision")
                            if (m.capabilities.tools)           add("Tools")
                            if (m.capabilities.imageGeneration) add("Images")
                            if (m.capabilities.audio)           add("Audio")
                        }
                        Text("• Capabilities: ${if (caps.isEmpty()) "Chat" else caps.joinToString()}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { testingModel?.let { viewModel.testModel(it) } }) { Text("Test Again") }
            },
            dismissButton = {
                TextButton(onClick = { showTestDialog = false; viewModel.dismissTestModelDialog(); testingModel = null }) { Text("Close") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        SectionHeader("Model Management", Icons.Default.ManageAccounts)

        // Action strip
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("NEW")
            }
            OutlinedButton(
                onClick = { viewModel.fetchModels() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoadingModels
            ) {
                if (uiState.isLoadingModels) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("FETCH")
            }
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("RESET")
            }
        }

        // Search bar
        SectionHeader("Search Models", Icons.Default.Search)
        OutlinedTextField(
            value = modelSearchQuery,
            onValueChange = { modelSearchQuery = it },
            placeholder = { Text("Search by name or model ID...") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
            trailingIcon = {
                if (modelSearchQuery.isNotBlank()) {
                    IconButton(onClick = { modelSearchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, Modifier.size(18.dp))
                    }
                }
            }
        )

        // Active model text field
        SectionHeader("Active Model", Icons.Default.RadioButtonChecked)
        OutlinedTextField(
            value = settings.selectedModel,
            onValueChange = viewModel::updateSelectedModel,
            label = { Text("Current Model ID") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.SmartToy, null) },
            trailingIcon = {
                if (settings.selectedModel.isNotBlank()) {
                    IconButton(onClick = { viewModel.updateSelectedModel("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            }
        )

        // Model list with search results
                // ── Quick Models ─────────────────────────────────────────────────
        SectionHeader("Quick Models", Icons.Default.FlashOn)
        Text(
            "Add models here for fast switching in the chat. The quick-select appears when you have 2+ models.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Add to quick models
        var quickModelInput by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = quickModelInput,
                onValueChange = { quickModelInput = it },
                label = { Text("Model ID to add") },
                placeholder = { Text("e.g. gpt-4o or pick from list") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.FlashOn, null, Modifier.size(18.dp)) }
            )
            IconButton(
                onClick = {
                    if (quickModelInput.isNotBlank()) {
                        viewModel.addQuickModel(quickModelInput.trim())
                        quickModelInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, "Add", tint = Color.White)
            }
        }

        // Quick add from configured models
        if (allModels.isNotEmpty()) {
            val nonQuickModels = allModels.filter { it.modelId !in settings.quickModels }
            if (nonQuickModels.isNotEmpty()) {
                Text(
                    "Quick add from configured models:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    nonQuickModels.forEach { model ->
                        SuggestionChip(
                            onClick = { viewModel.addQuickModel(model.modelId) },
                            label = { Text(model.displayName.take(20), fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Add, null, Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }

        // Current quick models list
        if (settings.quickModels.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            settings.quickModels.forEachIndexed { index, modelId ->
                val configuredModel = allModels.find { it.modelId == modelId }
                val displayName = configuredModel?.displayName ?: modelId
                val isActive = settings.selectedModel == modelId

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            null,
                            Modifier.size(18.dp),
                            tint = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(displayName, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                            if (displayName != modelId) {
                                Text(modelId, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                        if (isActive) {
                            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                                Text("ACTIVE", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        IconButton(onClick = { viewModel.removeQuickModel(modelId) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "Remove", Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("Add 2 or more models above to enable quick model switching in chat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (filteredModels.isNotEmpty()) {
            val headerText = when {
                modelSearchQuery.isBlank() -> "Configured Models (${allModels.size})"
                filteredModels.size == allModels.size -> "All Models (${allModels.size})"
                else -> "Found ${filteredModels.size} of ${allModels.size} models"
            }
            SectionHeader(headerText, Icons.Default.ViewList)
            filteredModels.forEach { model ->
                ModelCard(
                    model = model,
                    isSelected = settings.selectedModel == model.modelId,
                    isQuick    = model.modelId in settings.quickModels,
                    onSelect = { viewModel.updateSelectedModel(model.modelId) },
                    onEdit = { editingModel = model },
                    onDelete = { viewModel.deleteModel(model.id) },
                    onTest = { testingModel = model; showTestDialog = true; viewModel.testModel(model) },
                    onQuick = {
                        if (model.modelId in settings.quickModels) viewModel.removeQuickModel(model.modelId)
                        else viewModel.addQuickModel(model.modelId)
                    }
                )
            }
        } else {
            EmptyState(
                icon = Icons.Default.SmartToy,
                title = if (modelSearchQuery.isBlank()) "No models configured" else "No models match \"$modelSearchQuery\"",
                subtitle = if (modelSearchQuery.isBlank()) "Tap NEW to add manually or FETCH to load from provider"
                else "Try a different search term"
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ModelCard(
    model      : com.aei.chatbot.domain.model.ModelConfig,
    isSelected : Boolean,
    isQuick    : Boolean,
    onSelect   : () -> Unit,
    onEdit     : () -> Unit,
    onDelete   : () -> Unit,
    onTest     : () -> Unit,
    onQuick    : () -> Unit
) {
    val cardColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        else null
    ) {
        Column(Modifier.padding(14.dp)) {
            // Header row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SmartToy, null,
                        Modifier.size(20.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(model.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        model.modelId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "ACTIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Capability chips
            if (listOf(model.capabilities.chat, model.capabilities.vision,
                    model.capabilities.tools, model.capabilities.imageGeneration,
                    model.capabilities.audio).any { it }
            ) {
                Row(
                    modifier             = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (model.capabilities.chat)            CapabilityChip("Chat",   Icons.Default.Chat)
                    if (model.capabilities.vision)          CapabilityChip("Vision", Icons.Default.RemoveRedEye)
                    if (model.capabilities.tools)           CapabilityChip("Tools",  Icons.Default.Build)
                    if (model.capabilities.imageGeneration) CapabilityChip("Images", Icons.Default.Image)
                    if (model.capabilities.audio)           CapabilityChip("Audio",  Icons.Default.Headphones)
                }
            }

            // Token info
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TokenBadge("CTX", model.contextWindow)
                TokenBadge("OUT", model.maxOutputTokens)
                if (model.temperature > 0f) {
                    TokenBadge("TEMP", null, "%.1f".format(model.temperature))
                }
            }

            HorizontalDivider(
                Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Actions
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSelect) { Text("Select", fontSize = 12.sp) }
                TextButton(onClick = onEdit)   { Text("Edit",   fontSize = 12.sp) }
                TextButton(onClick = onTest)   { Text("Test",   fontSize = 12.sp) }
                TextButton(
                    onClick = onQuick,
                    colors  = ButtonDefaults.textButtonColors(
                        contentColor = if (isQuick) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        if (isQuick) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        null, Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(if (isQuick) "⚡ Quick" else "+ Quick", fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CapabilityChip(label: String, icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun TokenBadge(label: String, count: Int?, override: String? = null) {
    Text(
        "$label: ${override ?: formatTokenCount(count ?: 0)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    )
}

private fun formatTokenCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000     -> "${n / 1_000}K"
    else           -> n.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelEditDialog(
    model     : com.aei.chatbot.domain.model.ModelConfig?,
    onSave    : (com.aei.chatbot.domain.model.ModelConfig) -> Unit,
    onDismiss : () -> Unit
) {
    var displayName  by remember { mutableStateOf(model?.displayName ?: "") }
    var modelId      by remember { mutableStateOf(model?.modelId ?: "") }
    var contextWindow by remember { mutableStateOf((model?.contextWindow ?: 4096).toString()) }
    var maxOutput    by remember { mutableStateOf((model?.maxOutputTokens ?: 2048).toString()) }
    var capChat      by remember { mutableStateOf(model?.capabilities?.chat ?: true) }
    var capVision    by remember { mutableStateOf(model?.capabilities?.vision ?: false) }
    var capTools     by remember { mutableStateOf(model?.capabilities?.tools ?: false) }
    var capImages    by remember { mutableStateOf(model?.capabilities?.imageGeneration ?: false) }
    var capAudio     by remember { mutableStateOf(model?.capabilities?.audio ?: false) }
    var temperature by remember { mutableFloatStateOf(model?.temperature ?: 0.7f) }
    val isValid      = modelId.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (model == null) "Add Model" else "Edit Model", fontWeight = FontWeight.SemiBold) },
        text  = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value         = displayName,
                    onValueChange = { displayName = it },
                    label         = { Text("Display Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = modelId,
                    onValueChange = { modelId = it },
                    label         = { Text("Model ID *") },
                    placeholder   = { Text("e.g. gpt-4.1, llama-3.2-90b") },
                    isError       = modelId.isBlank(),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = contextWindow,
                        onValueChange = { contextWindow = it },
                        label         = { Text("Context (tokens)") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier      = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value         = maxOutput,
                        onValueChange = { maxOutput = it },
                        label         = { Text("Max Output") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier      = Modifier.weight(1f)
                    )
                }
                Text("Capabilities", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = capChat,   onClick = { capChat = !capChat },     label = { Text("Chat") })
                    FilterChip(selected = capVision, onClick = { capVision = !capVision }, label = { Text("Vision") })
                    FilterChip(selected = capTools,  onClick = { capTools = !capTools },   label = { Text("Tools") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = capImages, onClick = { capImages = !capImages }, label = { Text("Images") })
                    FilterChip(selected = capAudio,  onClick = { capAudio = !capAudio },   label = { Text("Audio") })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Temperature", style = MaterialTheme.typography.bodySmall)
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "%.1f".format(temperature),
                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Slider(
                    value         = temperature,
                    onValueChange = { temperature = (it * 10).toInt() / 10f },
                    valueRange    = 0f..2f
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    onSave(
                        com.aei.chatbot.domain.model.ModelConfig(
                            id              = model?.id ?: java.util.UUID.randomUUID().toString(),
                            displayName     = displayName.ifBlank { modelId },
                            modelId         = modelId,
                            capabilities    = com.aei.chatbot.domain.model.ModelCapabilities(capChat, capVision, capImages, capAudio, capTools),
                            contextWindow   = contextWindow.toIntOrNull() ?: 4096,
                            maxOutputTokens = maxOutput.toIntOrNull() ?: 2048,
                            temperature     = temperature
                        )
                    )
                },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3 · WEB SEARCH
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WebSearchTab(
    settings  : com.aei.chatbot.domain.model.AppSettings,
    viewModel : SettingsViewModel
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        SectionHeader("Web Search", Icons.Default.Language)
        ToggleRow(
            label    = stringResource(R.string.settings_web_search_enabled),
            description = stringResource(R.string.settings_web_search_desc),
            checked  = settings.webSearchEnabled,
            onChange = viewModel::updateWebSearchEnabled,
            icon     = Icons.Default.TravelExplore
        )

        AnimatedVisibility(
            visible = settings.webSearchEnabled,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column {
                SectionHeader("Search Mode", Icons.Default.ToggleOn)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(4.dp)) {
                        SearchModeRow(
                            title       = stringResource(R.string.settings_search_manual),
                            description = stringResource(R.string.settings_search_manual_desc),
                            selected    = settings.webSearchMode == "manual",
                            onClick     = { viewModel.updateWebSearchMode("manual") }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 8.dp))
                        SearchModeRow(
                            title       = stringResource(R.string.settings_search_auto),
                            description = stringResource(R.string.search_auto_desc_full),
                            selected    = settings.webSearchMode == "auto",
                            onClick     = { viewModel.updateWebSearchMode("auto") }
                        )
                    }
                }

                SectionHeader("SearXNG Instance", Icons.Default.ShoppingCart)
                OutlinedTextField(
                    value         = settings.searxngUrl,
                    onValueChange = viewModel::updateSearxngUrl,
                    label         = { Text(stringResource(R.string.settings_searxng_url)) },
                    placeholder   = { Text("https://your-searxng.example.com") },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    supportingText = { Text(stringResource(R.string.settings_searxng_format_note)) }
                )

                SectionHeader("Result Settings", Icons.Default.FilterList)
                SliderRow(
                    label    = "Max Results Per Search",
                    subLabel = "How many results are injected into AI context",
                    value    = settings.webSearchResultCount.toFloat(),
                    range    = 1f..10f,
                    display  = "${settings.webSearchResultCount}",
                    steps    = 8,
                    onChange = { viewModel.updateWebSearchResultCount(it.toInt()) }
                )

                SectionHeader("Safe Search", Icons.Default.Shield)
                val safeSearchOptions = listOf(
                    "off"      to stringResource(R.string.settings_safe_search_off),
                    "moderate" to stringResource(R.string.settings_safe_search_moderate),
                    "strict"   to stringResource(R.string.settings_safe_search_strict)
                )
                Row(
                    modifier             = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    safeSearchOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = settings.safeSearch == key,
                            onClick  = { viewModel.updateSafeSearch(key) },
                            label    = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SectionHeader("How It Works", Icons.Default.HelpOutline)
                InfoStepsCard(
                    steps = listOf(
                        "Tap the 🔍 button next to the mic in the chat input bar.",
                        "Type your message and send — AeI searches the web first.",
                        "Results are injected into context. Sources shown as a collapsible card."
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SearchModeRow(
    title       : String,
    description : String,
    selected    : Boolean,
    onClick     : () -> Unit,
    disabled    : Boolean = false
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (!disabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick  = if (!disabled) onClick else null,
            enabled  = !disabled
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                title,
                style     = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color     = if (disabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (disabled) 0.3f else 0.6f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 4 · CHAT SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChatSettingsTab(
    settings        : com.aei.chatbot.domain.model.AppSettings,
    viewModel       : SettingsViewModel,
    showPromptInfo  : Boolean,
    onShowPromptInfo: (Boolean) -> Unit
) {
    if (showPromptInfo) {
        AlertDialog(
            onDismissRequest = { onShowPromptInfo(false) },
            title = { Text("System Prompt", fontWeight = FontWeight.SemiBold) },
            text  = { Text("The system prompt defines the AI personality and behavior. It is sent at the start of every conversation as a hidden instruction to the model. Keep it clear and concise for best results.") },
            confirmButton = { TextButton(onClick = { onShowPromptInfo(false) }) { Text("Got it") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        SectionHeader("Persona / System Prompt", Icons.Default.Psychology)
        Row(
            modifier         = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value         = settings.systemPrompt,
                onValueChange = { if (it.length <= Constants.MAX_SYSTEM_PROMPT_LENGTH) viewModel.updateSystemPrompt(it) },
                label         = { Text("System Prompt") },
                modifier      = Modifier.weight(1f),
                minLines      = 5,
                maxLines      = 10,
                supportingText = {
                    val pct = (settings.systemPrompt.length * 100) / Constants.MAX_SYSTEM_PROMPT_LENGTH
                    Text(
                        "${settings.systemPrompt.length} / ${Constants.MAX_SYSTEM_PROMPT_LENGTH}",
                        color = when {
                            pct > 90 -> MaterialTheme.colorScheme.error
                            pct > 70 -> MaterialTheme.colorScheme.tertiary
                            else     -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            Column(Modifier.padding(start = 4.dp)) {
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

        SectionHeader("Conversation Behaviour", Icons.Default.Forum)
        ToggleRow(
            label    = "Auto-scroll to Bottom",
            description = "Scroll down automatically as new tokens arrive",
            checked  = settings.autoScroll,
            onChange = viewModel::updateAutoScroll,
            icon     = Icons.Default.KeyboardArrowDown
        )
        ToggleRow(
            label    = "Clear on New Session",
            description = "Reset context when starting a new chat",
            checked  = settings.clearOnNewSession,
            onChange = viewModel::updateClearOnNewSession,
            icon     = Icons.Default.CleaningServices
        )

        SectionHeader("Message Display", Icons.Default.Visibility)
        ToggleRow(
            label    = "Show Timestamps",
            description = "Display time next to each message",
            checked  = settings.showTimestamps,
            onChange = viewModel::updateShowTimestamps,
            icon     = Icons.Default.Schedule
        )
        ToggleRow(
            label    = "Show Avatars",
            description = "Show user and AI avatar icons in chat",
            checked  = settings.showAvatars,
            onChange = viewModel::updateShowAvatars,
            icon     = Icons.Default.AccountCircle
        )

        SectionHeader("Chat Bubble Style", Icons.Default.ChatBubbleOutline)
        val bubbleStyles = listOf(
            Constants.BUBBLE_ROUNDED to "Rounded",
            Constants.BUBBLE_SHARP   to "Sharp",
            Constants.BUBBLE_MINIMAL to "Minimal"
        )
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bubbleStyles.forEach { (key, label) ->
                FilterChip(
                    selected = settings.bubbleStyle == key,
                    onClick  = { viewModel.updateBubbleStyle(key) },
                    label    = { Text(label) }
                )
            }
        }

        SectionHeader("Feedback", Icons.Default.Vibration)
        ToggleRow(
            label    = "Haptic Feedback",
            description = "Vibrate on send and receive",
            checked  = settings.hapticFeedback,
            onChange = viewModel::updateHapticFeedback,
            icon     = Icons.Default.Vibration
        )
        ToggleRow(
            label    = "Sound Effects",
            description = "Play subtle sounds for chat events",
            checked  = settings.soundEffects,
            onChange = viewModel::updateSoundEffects,
            icon     = Icons.Default.VolumeUp
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 5 · GENERAL SETTINGS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GeneralSettingsTab(
    settings  : com.aei.chatbot.domain.model.AppSettings,
    uiState   : SettingsUiState,
    viewModel : SettingsViewModel,
    context   : android.content.Context,
    onClearAll: () -> Unit,
    onReset   : () -> Unit
) {
    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importChats() }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // ── Appearance ────────────────────────────────────────────────────
        SectionHeader("Appearance", Icons.Default.Palette)

        val themeModes = listOf(
            Constants.THEME_SYSTEM to "System",
            Constants.THEME_LIGHT  to "Light",
            Constants.THEME_DARK   to "Dark"
        )
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                "Theme",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themeModes.forEach { (key, label) ->
                    FilterChip(
                        selected = settings.themeMode == key,
                        onClick  = { viewModel.updateThemeMode(key) },
                        label    = { Text(label) }
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ToggleRow(
                label    = "Dynamic Color (Material You)",
                description = "Use wallpaper-based colors (Android 12+)",
                checked  = settings.dynamicColor,
                onChange = viewModel::updateDynamicColor,
                icon     = Icons.Default.ColorLens
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            val fontSizes = listOf(
                Constants.FONT_SMALL  to "S",
                Constants.FONT_MEDIUM to "M",
                Constants.FONT_LARGE  to "L",
                Constants.FONT_XL     to "XL"
            )
            Text("Font Size", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fontSizes.forEach { (key, label) ->
                    FilterChip(
                        selected = settings.fontSize == key,
                        onClick  = { viewModel.updateFontSize(key) },
                        label    = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    "The quick brown fox jumps over the lazy dog.",
                    Modifier.padding(12.dp),
                    fontSize = (14 * fontSizeMultiplier(settings.fontSize)).sp,
                    color    = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Avatar ────────────────────────────────────────────────────────
        SectionHeader("Your Avatar", Icons.Default.AccountBox)
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live preview circle
            Box(
                Modifier
                    .size(52.dp)
                    .background(avatarColorFromString(settings.avatarColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    settings.userInitials.take(2).uppercase().ifBlank { "?" },
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            }
            OutlinedTextField(
                value         = settings.userInitials,
                onValueChange = { if (it.length <= 2) viewModel.updateUserInitials(it) },
                label         = { Text("Initials") },
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(12.dp)
            )
        }
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("violet", "teal", "rose", "amber", "blue", "green").forEach { colorName ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(avatarColorFromString(colorName))
                        .border(
                            width  = if (settings.avatarColor == colorName) 3.dp else 0.dp,
                            color  = MaterialTheme.colorScheme.onSurface,
                            shape  = CircleShape
                        )
                        .clickable { viewModel.updateAvatarColor(colorName) }
                ) {
                    if (settings.avatarColor == colorName) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            Modifier.align(Alignment.Center).size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Language ──────────────────────────────────────────────────────
        SectionHeader("Language & Translation", Icons.Default.Translate)
        val languages = listOf(
            "en-US" to "English (US)",
            "en-GB" to "English (UK)",
            "uk"    to "Українська",
            "cs"    to "Čeština",
            "zh-CN" to "简体中文"
        )
        DropdownSettingRow(
            label       = "App Display Language",
            options     = languages,
            selectedKey = settings.appLanguage,
            onSelect    = { key ->
                viewModel.updateAppLanguage(key)
                AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(key))
            }
        )
        val translationOptions = listOf("" to "None (disabled)") + languages
        DropdownSettingRow(
            label       = "Translate AI Responses To",
            options     = translationOptions,
            selectedKey = settings.translationLanguage,
            onSelect    = viewModel::updateTranslationLanguage
        )
        DropdownSettingRow(
            label       = "Voice Input Language",
            options     = languages,
            selectedKey = settings.voiceInputLanguage,
            onSelect    = viewModel::updateVoiceInputLanguage
        )
        ToggleRow(
            label    = "Auto-Detect Input Language",
            description = "Detect the language of your messages automatically",
            checked  = settings.autoDetectLanguage,
            onChange = viewModel::updateAutoDetectLanguage,
            icon     = Icons.Default.Translate
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Data & Storage ────────────────────────────────────────────────
        SectionHeader("Data & Storage", Icons.Default.Storage)

        OutlinedButton(
            onClick  = viewModel::exportChats,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.FileDownload, null)
            Spacer(Modifier.width(8.dp))
            Text("Export All Chats")
        }

        // FIX: Import now launches a real file picker (JSON)
        OutlinedButton(
            onClick  = { importLauncher.launch("application/json") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.FileUpload, null)
            Spacer(Modifier.width(8.dp))
            Text("Import Chats")
        }

        Button(
            onClick  = onClearAll,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.DeleteForever, null)
            Spacer(Modifier.width(8.dp))
            Text("Clear All Chats")
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        // ── Reset ─────────────────────────────────────────────────────────
        SectionHeader("Reset", Icons.Default.SettingsBackupRestore)
        OutlinedButton(
            onClick  = onReset,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.RestartAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Reset All Settings to Default")
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 6 · FEATURES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FeaturesTab(
    settings  : com.aei.chatbot.domain.model.AppSettings,
    viewModel : SettingsViewModel
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        SectionHeader("✨ Prompt Enhancement", Icons.Default.AutoAwesome)

        // Beta warning
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            ),
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
        ) {
            Row(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.Top
            ) {
                Text("⚠️", fontSize = 18.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.features_beta_title),
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.error
                    )
                    Text(
                        stringResource(R.string.features_beta_body),
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Feature toggle card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors   = CardDefaults.cardColors(
                containerColor = if (settings.promptEnhancementEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            border   = if (settings.promptEnhancementEnabled)
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            else null
        ) {
            Row(
                modifier             = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment    = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                if (settings.promptEnhancementEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("✨", fontSize = 20.sp) }
                    Column {
                        Text("Prompt Enhancement", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (settings.promptEnhancementEnabled) stringResource(R.string.features_active_beta)
                            else "Inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.promptEnhancementEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Switch(
                    checked         = settings.promptEnhancementEnabled,
                    onCheckedChange = viewModel::updatePromptEnhancementEnabled
                )
            }
        }

        SectionHeader("How It Works", Icons.Default.HelpOutline)
        InfoStepsCard(
            steps = listOf(
                "You type a short message, e.g. \"tell me about stars\".",
                "AeI rewrites it: \"Explain the life cycle, classification, and properties of stars, including how they form, evolve, and die.\"",
                "Your original message is displayed in chat; the enhanced version is what the AI receives."
            ),
            highlightIndex = 1
        )

        AnimatedVisibility(
            visible = settings.promptEnhancementEnabled,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Column {
                SectionHeader("Enhancement Instruction", Icons.Default.Edit)

                SectionHeader("Enhancement Model", Icons.Default.SmartToy)
                Text(
                    "Choose which model rewrites your prompts. Leave empty to use the same model as chat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                )
                OutlinedTextField(
                    value = settings.enhancementModel,
                    onValueChange = viewModel::updateEnhancementModel,
                    label = { Text("Enhancement Model ID") },
                    placeholder = { Text("Leave empty = use chat model") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (settings.enhancementModel.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateEnhancementModel("") }) {
                                Icon(Icons.Default.Clear, null, Modifier.size(18.dp))
                            }
                        }
                    }
                )
                // Quick pick from configured models
                val enhanceModels = settings.providers.flatMap { it.models }
                if (enhanceModels.isNotEmpty()) {
                    Text(
                        "Or pick from configured models:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        enhanceModels.forEach { model ->
                            val isActive = settings.enhancementModel == model.modelId
                            FilterChip(
                                selected = isActive,
                                onClick = {
                                    viewModel.updateEnhancementModel(
                                        if (isActive) "" else model.modelId
                                    )
                                },
                                label = { Text(model.displayName.take(20), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text(
                    "Customize how the AI rewrites your prompts.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                )
                OutlinedTextField(
                    value         = settings.promptEnhancementInstruction,
                    onValueChange = viewModel::updatePromptEnhancementInstruction,
                    label         = { Text("Enhancement Instruction") },
                    modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    minLines      = 4,
                    maxLines      = 8,
                    shape         = RoundedCornerShape(12.dp)
                )
                TextButton(
                    onClick  = {
                        viewModel.updatePromptEnhancementInstruction(
                            "You are a master prompt engineer. Analyze and enhance the following prompt for generative AI. " +
                                    "Add context, structure, and specificity. Instruct the AI to use headers, tables for comparisons, " +
                                    "bullet points for lists, bold for key terms. Return only the enhanced version — no preamble."
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset to Default")
                }
            }
        }

        AnimatedVisibility(visible = !settings.promptEnhancementEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        "Enable Prompt Enhancement above to configure this feature. Note: it uses one extra AI call per message.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ── AI Actions ────────────────────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        SectionHeader("🤖 AI Actions", Icons.Default.PlayArrow)

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
            ),
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f))
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Text("⚠️", fontSize = 18.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Beta Feature", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(
                        "AI Actions lets the AI open apps, create files, and open URLs on your device when you ask it to. Each action requires your approval (unless auto-approve is on). Results depend on the model quality.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer, lineHeight = 18.sp
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enable AI Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (settings.aiActionsEnabled) {
                                Surface(color = Color(0xFF4CAF50).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("ACTIVE", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                        Text(
                            if (settings.aiActionsEnabled) "AI can open apps, create files, and open URLs when asked"
                            else "Allow the AI to interact with your device when asked",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    Switch(checked = settings.aiActionsEnabled, onCheckedChange = { viewModel.updateAiActionsEnabled(it) })
                }

                AnimatedVisibility(visible = settings.aiActionsEnabled) {
                    Column {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.FlashOn, null, Modifier.size(15.dp),
                                        tint = if (settings.aiActionsAutoApprove) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    Text("Auto-Approve Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    if (settings.aiActionsAutoApprove) "⚡ Actions run instantly without asking you"
                                    else "You approve each action before it runs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (settings.aiActionsAutoApprove) Color(0xFFFFB300).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = settings.aiActionsAutoApprove,
                                onCheckedChange = { viewModel.updateAiActionsAutoApprove(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFB300), checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }

        // Capability cards
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("What AI can do", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                AiCapRow(Icons.Default.OpenInNew,       Color(0xFF4CAF50), "Open App",     "e.g. 'Open Viber' or 'Launch Spotify'")
                AiCapRow(Icons.Default.Place,           Color(0xFF00BCD4), "Open Map",     "e.g. 'Find nearest pharmacy' or 'Show Eiffel Tower'")
                AiCapRow(Icons.Default.InsertDriveFile, Color(0xFF2196F3), "Create File",  "e.g. 'Save this as notes.txt in Downloads'")
                AiCapRow(Icons.Default.Language,        Color(0xFFFF9800), "Open URL",     "e.g. 'Open google.com' or 'Go to GitHub'")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AiCapRow(icon: ImageVector, color: Color, title: String, example: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(32.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(16.dp), tint = color) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(example, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 7 · ABOUT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AboutTab(context: android.content.Context) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // Hero
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Gradient ring + logo
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(108.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF03DAC6))),
                                CircleShape
                            )
                    )
                    Box(
                        Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "AeI",
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Your Intelligent Companion", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "v${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}",
                        Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        SectionHeader("What is AeI?", Icons.Default.Info)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                "AeI is an open-source Android AI chatbot that connects to any OpenAI-compatible API — including LM Studio (local), NVIDIA NIM, and cloud providers. Chat with powerful language models, use real-time web search, and fully customize your experience.",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }

        SectionHeader("Features", Icons.Default.Star)
        val features = listOf(
            Icons.Default.Chat       to "Multi-turn conversation with streaming",
            Icons.Default.Search     to "Web search via SearXNG",
            Icons.Default.Psychology to "Thinking mode for reasoning models",
            Icons.Default.Language   to "On-device translation (ML Kit)",
            Icons.Default.Mic        to "Voice input (Speech-to-Text)",
            Icons.Default.Cloud      to "Cloud & local model support",
            Icons.Default.History    to "Persistent chat history"
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEachIndexed { i, (icon, desc) ->
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(desc, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (i < features.lastIndex)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                }
            }
        }

        SectionHeader("Technology Stack", Icons.Default.Code)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TechRow("Language",     "Kotlin 2.0")
                TechRow("UI",           "Jetpack Compose + Material 3")
                TechRow("Architecture", "MVVM + Clean Architecture")
                TechRow("DI",           "Hilt")
                TechRow("Database",     "Room")
                TechRow("Network",      "Retrofit2 + OkHttp3 + SSE")
                TechRow("Translation",  "Google ML Kit")
            }
        }

        SectionHeader("Links & Support", Icons.Default.OpenInNew)
        ListItem(
            headlineContent  = { Text("View on GitHub") },
            supportingContent = { Text("Source code & documentation") },
            leadingContent   = { Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.primary) },
            modifier         = Modifier.clickable {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(Constants.GITHUB_URL))
                    )
                }
            }
        )
        ListItem(
            headlineContent  = { Text("Report an Issue") },
            supportingContent = { Text("github.com/aei-chatbot/aei/issues") },
            leadingContent   = { Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.primary) },
            modifier         = Modifier.clickable {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("${Constants.GITHUB_URL}/issues"))
                    )
                }
            }
        )

        SectionHeader("Legal Disclaimer", Icons.Default.Shield)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Text(
                "AI-generated content may be inaccurate, incomplete, or misleading. AeI is not responsible for content generated by third-party AI models or APIs. Always verify important information from authoritative sources. Use AeI responsibly.",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }

        SectionHeader("Credits", Icons.Default.Favorite)
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Built with ❤️ using open source libraries.", style = MaterialTheme.typography.bodySmall)
                Text(
                    "LM Studio · NVIDIA NIM · Google ML Kit · Retrofit · Hilt · Room",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

/** Accent-bar section header with an optional icon. */
@Composable
fun SectionHeader(title: String, icon: ImageVector? = null) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier          = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
            .drawBehind {
                drawLine(
                    color       = primary.copy(alpha = 0.35f),
                    start       = Offset(0f, size.height),
                    end         = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                    cap         = StrokeCap.Round
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon?.let {
            Icon(it, null, Modifier.size(14.dp), tint = primary.copy(alpha = 0.8f))
        }
        Text(
            title,
            style      = MaterialTheme.typography.labelLarge,
            color      = primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Toggle row with optional leading icon. */
@Composable
fun ToggleRow(
    label       : String,
    description : String = "",
    checked     : Boolean,
    onChange    : (Boolean) -> Unit,
    icon        : ImageVector? = null
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                it, null,
                Modifier.size(20.dp).padding(end = 0.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (description.isNotEmpty()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Slider row with a floating value badge. */
@Composable
fun SliderRow(
    label    : String,
    subLabel : String = "",
    value    : Float,
    range    : ClosedFloatingPointRange<Float>,
    display  : String,
    steps    : Int = 0,
    onChange : (Float) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                if (subLabel.isNotEmpty()) {
                    Text(subLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    display,
                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

/** Validated outlined text field with error state. */
@Composable
fun ValidatedTextField(
    value        : String,
    onValueChange: (String) -> Unit,
    label        : String,
    error        : String = "",
    keyboardType : KeyboardType = KeyboardType.Text,
    leadingIcon  : ImageVector? = null
) {
    OutlinedTextField(
        value          = value,
        onValueChange  = onValueChange,
        label          = { Text(label) },
        isError        = error.isNotEmpty(),
        supportingText = { if (error.isNotEmpty()) Text(error) },
        modifier       = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine     = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape          = RoundedCornerShape(12.dp),
        leadingIcon    = leadingIcon?.let { { Icon(it, null) } }
    )
}

/** Reusable confirm/destroy dialog. */
@Composable
fun ConfirmDialog(
    title        : String,
    body         : String,
    confirmLabel : String,
    confirmColor : Color = MaterialTheme.colorScheme.primary,
    onConfirm    : () -> Unit,
    onDismiss    : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title  = { Text(title, fontWeight = FontWeight.SemiBold) },
        text   = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors  = ButtonDefaults.textButtonColors(contentColor = confirmColor)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** Numbered steps info card used in Search and Features tabs. */
@Composable
fun InfoStepsCard(steps: List<String>, highlightIndex: Int = -1) {
    val stepColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF2E7D32)
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment    = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val color = stepColors.getOrElse(index) { MaterialTheme.colorScheme.primary }
                    Box(
                        Modifier.size(26.dp).background(color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp
                        )
                    }
                    Text(
                        step,
                        style    = MaterialTheme.typography.bodySmall,
                        fontWeight = if (index == highlightIndex) FontWeight.Medium else FontWeight.Normal,
                        color    = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (index == highlightIndex) 0.9f else 0.75f
                        ),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/** Empty state with icon, title, subtitle. */
@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
        }
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSettingRow(
    label       : String,
    options     : List<Pair<String, String>>,
    selectedKey : String,
    onSelect    : (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // FIX: if selectedKey doesn't match any option, show a clear placeholder instead of silently
    // showing the first option.
    val selectedLabel = options.find { it.first == selectedKey }?.second
        ?: if (selectedKey.isBlank()) options.firstOrNull()?.second ?: "" else "Unknown"

    ExposedDropdownMenuBox(
        expanded         = expanded,
        onExpandedChange = { expanded = it },
        modifier         = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value        = selectedLabel,
            onValueChange = {},
            readOnly     = true,
            label        = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier     = Modifier.fillMaxWidth().menuAnchor(),
            shape        = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, name) ->
                DropdownMenuItem(
                    text          = { Text(name) },
                    onClick       = { onSelect(key); expanded = false },
                    trailingIcon  = if (key == selectedKey) ({
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }) else null
                )
            }
        }
    }
}
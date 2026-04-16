package com.aei.chatbot.ui.chat.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aei.chatbot.R
import com.aei.chatbot.domain.model.ChatMessage
import com.aei.chatbot.ui.theme.LocalAeIColors
import com.aei.chatbot.ui.theme.avatarColorFromString
import com.aei.chatbot.util.ClipboardUtils
import com.aei.chatbot.util.DateTimeUtils
import com.aei.chatbot.util.MarkdownParser
import com.aei.chatbot.util.MarkdownSegment
import com.aei.chatbot.util.SegmentType

data class ThinkingParsed(val thinkingContent: String, val responseContent: String, val hasThinking: Boolean)

fun parseThinking(text: String): ThinkingParsed {
    val thinkStart = text.indexOf("<think>")
    val thinkEnd = text.indexOf("</think>")
    return when {
        thinkStart != -1 && thinkEnd != -1 && thinkEnd > thinkStart -> {
            val thinking = text.substring(thinkStart + 7, thinkEnd).trim()
            val before = text.substring(0, thinkStart).trim()
            val after = text.substring(thinkEnd + 8).trim()
            val response = "$before $after".trim()
            ThinkingParsed(thinking, response, true)
        }
        thinkStart != -1 && thinkEnd == -1 -> {
            // Still thinking (streaming, no close tag yet)
            val thinking = text.substring(thinkStart + 7).trim()
            val before = text.substring(0, thinkStart).trim()
            ThinkingParsed(thinking, before, true)
        }
        else -> ThinkingParsed("", text, false)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage, showTimestamp: Boolean, showAvatars: Boolean,
    userInitials: String, avatarColorName: String, fontScale: Float,
    bubbleStyle: String, onLongPress: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier, isStreamingBubble: Boolean = false
) {
    val isUser = message.role == "user"
    val aeIColors = LocalAeIColors.current
    val context = LocalContext.current
    var showTranslation by remember { mutableStateOf(true) }

    val bubbleShape = when (bubbleStyle) {
        "sharp" -> RoundedCornerShape(4.dp)
        "minimal" -> RoundedCornerShape(8.dp)
        else -> if (isUser) RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
        else RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
    }

    if (message.isError) {
        Row(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Bottom) {
            Box(Modifier.widthIn(max = 300.dp).clip(bubbleShape).background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(message.content, fontSize = (14f * fontScale).sp, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        return
    }

    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom) {
        if (!isUser && showAvatars) {
            Box(Modifier.size(32.dp).background(Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF03DAC6))), CircleShape),
                contentAlignment = Alignment.Center) { Text("A", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(8.dp))
        }

        Box(Modifier.widthIn(max = 300.dp).clip(bubbleShape)
            .background(if (isUser) aeIColors.userBubble else aeIColors.aiBubble)
            .combinedClickable(onClick = {}, onLongClick = { onLongPress(message) })
            .padding(horizontal = 12.dp, vertical = 8.dp)) {
            Column {
                val displayContent = if (message.translatedContent != null && showTranslation) message.translatedContent else message.content
                val parsed = parseThinking(displayContent)

                // Thinking block
                if (parsed.hasThinking && parsed.thinkingContent.isNotBlank()) {
                    ThinkingBlock(thinkingText = parsed.thinkingContent, fontScale = fontScale, isStreaming = isStreamingBubble && parsed.responseContent.isBlank())
                }

                // Main response
                val mainText = parsed.responseContent
                if (mainText.isNotBlank()) {
                    if (isStreamingBubble) StreamingBubbleText(text = mainText, fontScale = fontScale)
                    else MarkdownText(text = mainText, fontScale = fontScale, context = context)
                } else if (isStreamingBubble && parsed.hasThinking) {
                    // Still in thinking phase, show cursor
                    val inf = rememberInfiniteTransition(label = "tc")
                    val ca by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "tca")
                    Box(Modifier.width(2.dp).height((14f * fontScale).dp).background(MaterialTheme.colorScheme.primary.copy(alpha = ca)))
                }

                if (message.translatedContent != null && !isStreamingBubble) {
                    TextButton(onClick = { showTranslation = !showTranslation }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                        Text(if (showTranslation) stringResource(R.string.show_original) else stringResource(R.string.show_translation),
                            fontSize = (10 * fontScale).sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                }
                if (showTimestamp && !isStreamingBubble) {
                    Text(DateTimeUtils.formatTime(message.timestamp), fontSize = (10 * fontScale).sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start))
                }
            }
        }

        if (isUser && showAvatars) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(32.dp).background(avatarColorFromString(avatarColorName), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(userInitials.take(2).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ThinkingBlock(thinkingText: String, fontScale: Float, isStreaming: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val baseSize = 12f * fontScale

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Psychology, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (isStreaming) "Thinking..." else "Thought process",
                        fontSize = baseSize.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                    Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            AnimatedVisibility(visible = expanded || isStreaming) {
                Column(Modifier.padding(top = 4.dp)) {
                    HorizontalDivider(Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Text(thinkingText, fontSize = baseSize.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic, lineHeight = (baseSize * 1.4f).sp)
                    if (isStreaming) {
                        val inf = rememberInfiniteTransition(label = "thinkCur")
                        val ca by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "thinkCa")
                        Box(Modifier.width(2.dp).height(baseSize.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = ca)))
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingBubbleText(text: String, fontScale: Float, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "cursor")
    val ca by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "ca")
    val baseSize = 14f * fontScale
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(text, fontSize = baseSize.sp, color = MaterialTheme.colorScheme.onSurface)
        Box(Modifier.width(2.dp).height(baseSize.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = ca)))
    }
}

@Composable
fun MarkdownText(text: String, fontScale: Float, context: android.content.Context, modifier: Modifier = Modifier) {
    val segments = remember(text) { MarkdownParser.parse(text) }
    val baseSize = 14f * fontScale
    Column(modifier = modifier) {
        segments.forEach { segment ->
            when (segment.type) {
                SegmentType.CODE_BLOCK -> CodeBlock(segment, context, fontScale)
                SegmentType.BULLET -> Row { Text("\u2022 ", fontSize = baseSize.sp, color = MaterialTheme.colorScheme.onSurface); Text(segment.text, fontSize = baseSize.sp, color = MaterialTheme.colorScheme.onSurface) }
                SegmentType.NUMBERED -> Text(segment.text, fontSize = baseSize.sp, color = MaterialTheme.colorScheme.onSurface)
                SegmentType.HEADER1 -> Text(segment.text, fontSize = (baseSize * 1.4f).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                SegmentType.HEADER2 -> Text(segment.text, fontSize = (baseSize * 1.25f).sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                SegmentType.HEADER3 -> Text(segment.text, fontSize = (baseSize * 1.1f).sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                SegmentType.HORIZONTAL_RULE -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
                else -> { val ann = buildAnnotatedString { appendSegment(segment, baseSize) }; Text(ann, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendSegment(segment: MarkdownSegment, baseSize: Float) {
    when (segment.type) {
        SegmentType.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(segment.text) }
        SegmentType.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(segment.text) }
        SegmentType.BOLD_ITALIC -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append(segment.text) }
        SegmentType.INLINE_CODE -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22FFFFFF))) { append(segment.text) }
        else -> append(segment.text)
    }
}

@Composable
private fun CodeBlock(segment: MarkdownSegment, context: android.content.Context, fontScale: Float) {
    Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
        Column {
            if (segment.language.isNotEmpty()) { Text(segment.language, fontSize = (10 * fontScale).sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium); Spacer(Modifier.height(4.dp)) }
            Text(segment.text, fontFamily = FontFamily.Monospace, fontSize = (12 * fontScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { ClipboardUtils.copyToClipboard(context, "code", segment.text) },
            modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(Icons.Default.ContentCopy, context.getString(R.string.cd_copy_code), Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

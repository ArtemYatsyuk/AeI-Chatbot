package com.aei.chatbot.ui.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
            ThinkingParsed(thinking, "$before $after".trim(), true)
        }
        thinkStart != -1 && thinkEnd == -1 -> {
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

    // Enhanced prompt bubble - special display with Markwon
    if (message.role == "enhanced") {
        Row(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)) {
                        Text("✨", fontSize = 12.sp)
                        Text("Enhanced Prompt",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                    MarkwonText(
                        markdown = message.content,
                        fontSize = 13f * fontScale,
                        textColor = MaterialTheme.colorScheme.onTertiaryContainer.toArgb(),
                        linkColor = MaterialTheme.colorScheme.tertiary.toArgb()
                    )
                }
            }
        }
        return
    }


    if (message.isError) {
        Row(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Bottom) {
            Box(Modifier.widthIn(max = 320.dp).clip(bubbleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                MarkwonText(
                    markdown = message.content,
                    fontSize = 14f * fontScale,
                    textColor = MaterialTheme.colorScheme.onErrorContainer.toArgb()
                )
            }
        }
        return
    }

    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom) {

        if (!isUser && showAvatars) {
            Box(Modifier.size(34.dp).background(
                Brush.linearGradient(listOf(Color(0xFF7B61FF), Color(0xFF9D5CFF))), CircleShape),
                contentAlignment = Alignment.Center) {
                Text("Ae", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(Modifier
            .widthIn(max = if (isUser) 300.dp else 340.dp)
            .clip(bubbleShape)
            .then(
                if (isUser) Modifier.background(
                    Brush.linearGradient(listOf(Color(0xFF4A30BB), Color(0xFF8B6FFF)))
                ) else Modifier.background(aeIColors.aiBubble)
            )
            .combinedClickable(onClick = {}, onLongClick = { onLongPress(message) })
            .padding(horizontal = 14.dp, vertical = 10.dp)) {
            Column {
                val displayContent = if (message.translatedContent != null && showTranslation)
                    message.translatedContent else message.content
                val parsed = parseThinking(displayContent)

                if (parsed.hasThinking && parsed.thinkingContent.isNotBlank()) {
                    ThinkingBlock(thinkingText = parsed.thinkingContent, fontScale = fontScale,
                        isStreaming = isStreamingBubble && parsed.responseContent.isBlank())
                }

                val mainText = parsed.responseContent
                if (mainText.isNotBlank()) {
                    if (isStreamingBubble) {
                        StreamingBubbleText(text = mainText, fontScale = fontScale)
                    } else {
                        MarkwonText(
                            markdown = mainText,
                            fontSize = 14.5f * fontScale,
                            textColor = if (isUser) android.graphics.Color.WHITE
                                        else MaterialTheme.colorScheme.onSurface.toArgb(),
                            linkColor = if (isUser) android.graphics.Color.WHITE
                                        else MaterialTheme.colorScheme.primary.toArgb()
                        )
                    }
                } else if (isStreamingBubble && parsed.hasThinking) {
                    val inf = rememberInfiniteTransition(label = "tc")
                    val ca by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "tca")
                    Box(Modifier.width(2.dp).height((14f * fontScale).dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = ca)))
                }

                if (message.translatedContent != null && !isStreamingBubble) {
                    TextButton(onClick = { showTranslation = !showTranslation },
                        contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                        Text(if (showTranslation) stringResource(R.string.show_original)
                        else stringResource(R.string.show_translation),
                            fontSize = (10 * fontScale).sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    }
                }

                if (showTimestamp && !isStreamingBubble) {
                    Text(DateTimeUtils.formatTime(message.timestamp),
                        fontSize = (10 * fontScale).sp,
                        color = if (isUser) Color.White.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start))
                }
            }
        }

        if (isUser && showAvatars) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(34.dp).background(avatarColorFromString(avatarColorName), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(userInitials.take(2).uppercase(), color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
    }

    // Copy button row — shown below every non-streaming bubble
    if (!isStreamingBubble) {
        val ctx = LocalContext.current
        val copyText = if (message.translatedContent != null && showTranslation)
            message.translatedContent else message.content
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start  = if (isUser) 0.dp else if (showAvatars) 42.dp else 12.dp,
                    end    = if (isUser) if (showAvatars) 42.dp else 12.dp else 0.dp,
                    top    = 1.dp,
                    bottom = 2.dp
                ),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            var copied by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    ClipboardUtils.copyToClipboard(ctx, "message", copyText)
                    copied = true
                },
                modifier = Modifier.size(26.dp)
            ) {
                if (copied) {
                    Text(
                        "✓",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
            // Reset copied state after 2s
            if (copied) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    copied = false
                }
            }
        }
    }
}

// ── Rich inline text (handles bold, italic, code, links, strikethrough) ──────
@Composable
fun RichText(
    text: String,
    fontScale: Float,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val segments = remember(text) { MarkdownParser.parseInline(text) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant

    val annotated = buildAnnotatedString {
        segments.forEach { seg ->
            when (seg.type) {
                SegmentType.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) { append(seg.text) }
                SegmentType.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) { append(seg.text) }
                SegmentType.BOLD_ITALIC -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = textColor)) { append(seg.text) }
                SegmentType.INLINE_CODE -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace,
                    background = codeBackground, color = MaterialTheme.colorScheme.primary,
                    fontSize = (13f * fontScale).sp)) { append(" ${seg.text} ") }
                SegmentType.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough,
                    color = textColor.copy(alpha = 0.6f))) { append(seg.text) }
                SegmentType.LINK -> {
                    pushStringAnnotation("URL", seg.language)
                    withStyle(SpanStyle(color = primaryColor,
                        textDecoration = TextDecoration.Underline)) { append(seg.text) }
                    pop()
                }
                else -> withStyle(SpanStyle(color = textColor)) { append(seg.text) }
            }
        }
    }

    ClickableText(text = annotated,
        style = androidx.compose.ui.text.TextStyle(fontSize = (14f * fontScale).sp),
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ann.item))) }
                catch (_: Exception) {}
            }
        })
}

// ── Full markdown renderer ───────────────────────────────────────────────────
@Composable
fun MarkdownText(text: String, fontScale: Float, context: android.content.Context, modifier: Modifier = Modifier) {
    val segments = remember(text) { MarkdownParser.parse(text) }
    val baseSize = 14f * fontScale
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.forEach { segment ->
            when (segment.type) {
                SegmentType.CODE_BLOCK -> CodeBlock(segment, context, fontScale)

                SegmentType.BLOCKQUOTE -> {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Box(Modifier.width(3.dp).height(20.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(8.dp))
                        if (segment.inlineSegments.isNotEmpty()) {
                            InlineRow(segment.inlineSegments, fontScale,
                                textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        } else {
                            Text(segment.text, fontSize = baseSize.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontStyle = FontStyle.Italic)
                        }
                    }
                }

                SegmentType.HEADER1 -> {
                    Spacer(Modifier.height(4.dp))
                    if (segment.inlineSegments.isNotEmpty()) {
                        InlineRow(segment.inlineSegments, fontScale * 1.5f,
                            defaultWeight = FontWeight.ExtraBold)
                    } else {
                        Text(segment.text, fontSize = (baseSize * 1.5f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                }

                SegmentType.HEADER2 -> {
                    Spacer(Modifier.height(3.dp))
                    if (segment.inlineSegments.isNotEmpty()) {
                        InlineRow(segment.inlineSegments, fontScale * 1.3f,
                            defaultWeight = FontWeight.Bold)
                    } else {
                        Text(segment.text, fontSize = (baseSize * 1.3f).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                SegmentType.HEADER3 -> {
                    Spacer(Modifier.height(2.dp))
                    if (segment.inlineSegments.isNotEmpty()) {
                        InlineRow(segment.inlineSegments, fontScale * 1.15f,
                            defaultWeight = FontWeight.SemiBold)
                    } else {
                        Text(segment.text, fontSize = (baseSize * 1.15f).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                SegmentType.BULLET -> {
                    val indent = (segment.language.toIntOrNull() ?: 0) * 12
                    Row(modifier = Modifier.padding(start = indent.dp, top = 1.dp, bottom = 1.dp)) {
                        Text("• ", fontSize = baseSize.sp,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        if (segment.inlineSegments.isNotEmpty()) {
                            InlineRow(segment.inlineSegments, fontScale)
                        } else {
                            Text(segment.text, fontSize = baseSize.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                SegmentType.NUMBERED -> {
                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                        Text("› ", fontSize = baseSize.sp,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        if (segment.inlineSegments.isNotEmpty()) {
                            InlineRow(segment.inlineSegments, fontScale)
                        } else {
                            Text(segment.text, fontSize = baseSize.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                SegmentType.HORIZONTAL_RULE ->
                    HorizontalDivider(Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

                SegmentType.TABLE_ROW -> {
                    val cells = segment.text.trim().trim('|').split("|").map { it.trim() }
                    Row(Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(0.dp))) {
                        cells.forEach { cell ->
                            Box(Modifier.weight(1f).padding(6.dp)) {
                                Text(cell, fontSize = (baseSize * 0.9f).sp,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                SegmentType.PLAIN -> {
                    if (segment.text.isBlank()) {
                        Spacer(Modifier.height(3.dp))
                    } else {
                        val segsToRender = if (segment.inlineSegments.isNotEmpty())
                            segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        InlineRow(segsToRender, fontScale)
                    }
                }

                else -> {
                    if (segment.text.isNotBlank()) {
                        val segsToRender = if (segment.inlineSegments.isNotEmpty())
                            segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        InlineRow(segsToRender, fontScale)
                    }
                }
            }
        }
    }
}

// ── Inline row - renders a list of inline segments in a flow ─────────────────
@Composable
fun InlineRow(
    segments: List<MarkdownSegment>,
    fontScale: Float,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    defaultWeight: FontWeight = FontWeight.Normal
) {
    val context = LocalContext.current
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    val annotated = buildAnnotatedString {
        segments.forEach { seg ->
            when (seg.type) {
                SegmentType.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) { append(seg.text) }
                SegmentType.ITALIC -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) { append(seg.text) }
                SegmentType.BOLD_ITALIC -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = textColor)) { append(seg.text) }
                SegmentType.INLINE_CODE -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace,
                    background = codeBackground, color = primaryColor,
                    fontSize = (13f * fontScale).sp)) { append(" ${seg.text} ") }
                SegmentType.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough,
                    color = textColor.copy(alpha = 0.6f))) { append(seg.text) }
                SegmentType.LINK -> {
                    pushStringAnnotation("URL", seg.language)
                    withStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline)) { append(seg.text) }
                    pop()
                }
                else -> withStyle(SpanStyle(color = textColor, fontWeight = defaultWeight)) { append(seg.text) }
            }
        }
    }

    ClickableText(
        text = annotated,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = (14f * fontScale).sp,
            lineHeight = (14f * fontScale * 1.5f).sp,
            fontWeight = defaultWeight
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { ann ->
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ann.item))) }
                catch (_: Exception) {}
            }
        }
    )
}

// ── Thinking block ────────────────────────────────────────────────────────────
@Composable
fun ThinkingBlock(thinkingText: String, fontScale: Float, isStreaming: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val baseSize = 12f * fontScale
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
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
                Icon(if (expanded || isStreaming) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                    Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            AnimatedVisibility(visible = expanded || isStreaming) {
                Column(Modifier.padding(top = 4.dp)) {
                    HorizontalDivider(Modifier.padding(bottom = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Text(thinkingText, fontSize = baseSize.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic, lineHeight = (baseSize * 1.4f).sp)
                    if (isStreaming) {
                        val inf = rememberInfiniteTransition(label = "thinkCur")
                        val ca by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "thinkCa")
                        Box(Modifier.width(2.dp).height(baseSize.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = ca)))
                    }
                }
            }
        }
    }
}

// ── Streaming bubble with cursor ──────────────────────────────────────────────
@Composable
fun StreamingBubbleText(text: String, fontScale: Float, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "cursor")
    val ca by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "ca")
    val baseSize = 14f * fontScale

    // Parse and render markdown even during streaming
    val segments = remember(text) { MarkdownParser.parse(text) }

    Column(modifier = modifier) {
        // Render all complete segments except last using full markdown
        if (segments.size > 1) {
            val completeSegments = segments.dropLast(1)
            completeSegments.forEach { segment ->
                when (segment.type) {
                    SegmentType.CODE_BLOCK -> {
                        val ctx = LocalContext.current
                        CodeBlock(segment, ctx, fontScale)
                    }
                    SegmentType.HEADER1 -> {
                        val segs = if (segment.inlineSegments.isNotEmpty()) segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        InlineRow(segs, fontScale * 1.5f, defaultWeight = FontWeight.ExtraBold)
                    }
                    SegmentType.HEADER2 -> {
                        val segs = if (segment.inlineSegments.isNotEmpty()) segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        InlineRow(segs, fontScale * 1.3f, defaultWeight = FontWeight.Bold)
                    }
                    SegmentType.HEADER3 -> {
                        val segs = if (segment.inlineSegments.isNotEmpty()) segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        InlineRow(segs, fontScale * 1.15f, defaultWeight = FontWeight.SemiBold)
                    }
                    SegmentType.BULLET -> Row(modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)) {
                        Text("• ", fontSize = baseSize.sp, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                        val segs = if (segment.inlineSegments.isNotEmpty()) segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        InlineRow(segs, fontScale)
                    }
                    SegmentType.NUMBERED -> Row(modifier = Modifier.padding(top = 1.dp, bottom = 1.dp)) {
                        Text("› ", fontSize = baseSize.sp, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                        val segs = if (segment.inlineSegments.isNotEmpty()) segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        InlineRow(segs, fontScale)
                    }
                    SegmentType.HORIZONTAL_RULE -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    SegmentType.PLAIN -> {
                        if (segment.text.isBlank()) {
                            Spacer(Modifier.height(3.dp))
                        } else {
                            val segs = if (segment.inlineSegments.isNotEmpty()) segment.inlineSegments
                            else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                            InlineRow(segs, fontScale)
                        }
                    }
                    else -> {
                        val segs = if (segment.inlineSegments.isNotEmpty()) segment.inlineSegments
                        else listOf(MarkdownSegment(SegmentType.PLAIN, segment.text))
                        if (segment.text.isNotBlank()) InlineRow(segs, fontScale)
                    }
                }
            }
        }
        // Last segment with blinking cursor
        val lastSeg = segments.lastOrNull()
        if (lastSeg != null) {
            when (lastSeg.type) {
                SegmentType.CODE_BLOCK -> {
                    val ctx = LocalContext.current
                    CodeBlock(lastSeg, ctx, fontScale)
                }
                else -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        val segs = if (lastSeg.inlineSegments.isNotEmpty()) lastSeg.inlineSegments
                        else if (lastSeg.text.isNotBlank()) listOf(MarkdownSegment(SegmentType.PLAIN, lastSeg.text))
                        else emptyList()
                        if (segs.isNotEmpty()) InlineRow(segs, fontScale)
                        Box(Modifier.width(2.dp).height(baseSize.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = ca)))
                    }
                }
            }
        } else {
            Box(Modifier.width(2.dp).height(baseSize.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = ca)))
        }
    }
}

// ── Code block ────────────────────────────────────────────────────────────────
@Composable
fun CodeBlock(segment: com.aei.chatbot.util.MarkdownSegment, context: android.content.Context, fontScale: Float) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF1E1E2E)).padding(0.dp)) {
        // Header bar
        if (segment.language.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().background(Color(0xFF2D2D3F))
                .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(segment.language, fontSize = (11 * fontScale).sp,
                    color = Color(0xFF89B4FA), fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        ClipboardUtils.copyToClipboard(context, "code", segment.text)
                    }) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(12.dp), tint = Color(0xFF89B4FA))
                    Text("Copy", fontSize = (10 * fontScale).sp, color = Color(0xFF89B4FA))
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().background(Color(0xFF2D2D3F))
                .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable {
                        ClipboardUtils.copyToClipboard(context, "code", segment.text)
                    }) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(12.dp), tint = Color(0xFF89B4FA))
                    Text("Copy", fontSize = (10 * fontScale).sp, color = Color(0xFF89B4FA))
                }
            }
        }
        // Code content with horizontal scroll
        Box(Modifier.horizontalScroll(scrollState).padding(12.dp)) {
            Text(segment.text, fontFamily = FontFamily.Monospace,
                fontSize = (12.5f * fontScale).sp, color = Color(0xFFCDD6F4),
                lineHeight = (12.5f * fontScale * 1.6f).sp)
        }
    }
}
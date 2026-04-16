package com.aei.chatbot.util

data class MarkdownSegment(
    val type: SegmentType,
    val text: String,
    val language: String = ""
)

enum class SegmentType {
    PLAIN, BOLD, ITALIC, BOLD_ITALIC, INLINE_CODE, CODE_BLOCK, HEADER1, HEADER2, HEADER3,
    BULLET, NUMBERED, HORIZONTAL_RULE
}

object MarkdownParser {
    fun parse(input: String): List<MarkdownSegment> {
        val segments = mutableListOf<MarkdownSegment>()
        val lines = input.split("\n")
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("```") -> {
                    val lang = line.removePrefix("```").trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    segments.add(MarkdownSegment(SegmentType.CODE_BLOCK, codeLines.joinToString("\n"), lang))
                }
                line.startsWith("### ") -> segments.add(MarkdownSegment(SegmentType.HEADER3, line.removePrefix("### ")))
                line.startsWith("## ") -> segments.add(MarkdownSegment(SegmentType.HEADER2, line.removePrefix("## ")))
                line.startsWith("# ") -> segments.add(MarkdownSegment(SegmentType.HEADER1, line.removePrefix("# ")))
                line.startsWith("- ") || line.startsWith("* ") -> segments.add(MarkdownSegment(SegmentType.BULLET, line.drop(2)))
                line.matches(Regex("^\\d+\\. .+")) -> {
                    val text = line.substringAfter(". ")
                    segments.add(MarkdownSegment(SegmentType.NUMBERED, text))
                }
                line == "---" || line == "***" || line == "___" -> segments.add(MarkdownSegment(SegmentType.HORIZONTAL_RULE, ""))
                else -> {
                    if (line.isNotBlank()) {
                        segments.addAll(parseInline(line))
                    } else if (segments.isNotEmpty()) {
                        segments.add(MarkdownSegment(SegmentType.PLAIN, "\n"))
                    }
                }
            }
            i++
        }
        return segments
    }

    private fun parseInline(text: String): List<MarkdownSegment> {
        val segments = mutableListOf<MarkdownSegment>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            when {
                remaining.startsWith("***") || remaining.startsWith("___") -> {
                    val end = remaining.indexOf(remaining.take(3), 3)
                    if (end != -1) {
                        segments.add(MarkdownSegment(SegmentType.BOLD_ITALIC, remaining.substring(3, end)))
                        remaining = remaining.substring(end + 3)
                    } else {
                        segments.add(MarkdownSegment(SegmentType.PLAIN, remaining.take(1)))
                        remaining = remaining.drop(1)
                    }
                }
                remaining.startsWith("**") || remaining.startsWith("__") -> {
                    val marker = remaining.take(2)
                    val end = remaining.indexOf(marker, 2)
                    if (end != -1) {
                        segments.add(MarkdownSegment(SegmentType.BOLD, remaining.substring(2, end)))
                        remaining = remaining.substring(end + 2)
                    } else {
                        segments.add(MarkdownSegment(SegmentType.PLAIN, remaining.take(1)))
                        remaining = remaining.drop(1)
                    }
                }
                remaining.startsWith("*") || remaining.startsWith("_") -> {
                    val marker = remaining.take(1)
                    val end = remaining.indexOf(marker, 1)
                    if (end != -1) {
                        segments.add(MarkdownSegment(SegmentType.ITALIC, remaining.substring(1, end)))
                        remaining = remaining.substring(end + 1)
                    } else {
                        segments.add(MarkdownSegment(SegmentType.PLAIN, remaining.take(1)))
                        remaining = remaining.drop(1)
                    }
                }
                remaining.startsWith("`") -> {
                    val end = remaining.indexOf("`", 1)
                    if (end != -1) {
                        segments.add(MarkdownSegment(SegmentType.INLINE_CODE, remaining.substring(1, end)))
                        remaining = remaining.substring(end + 1)
                    } else {
                        segments.add(MarkdownSegment(SegmentType.PLAIN, remaining.take(1)))
                        remaining = remaining.drop(1)
                    }
                }
                else -> {
                    val nextSpecial = remaining.indexOfFirst { it in listOf('*', '_', '`') }
                    if (nextSpecial == -1) {
                        segments.add(MarkdownSegment(SegmentType.PLAIN, remaining))
                        remaining = ""
                    } else {
                        segments.add(MarkdownSegment(SegmentType.PLAIN, remaining.take(nextSpecial)))
                        remaining = remaining.drop(nextSpecial)
                    }
                }
            }
        }
        return segments
    }
}

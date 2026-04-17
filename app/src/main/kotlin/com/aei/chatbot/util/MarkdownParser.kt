package com.aei.chatbot.util

data class MarkdownSegment(
    val type: SegmentType,
    val text: String,
    val language: String = "",
    val inlineSegments: List<MarkdownSegment> = emptyList(),
    val cells: List<List<MarkdownSegment>> = emptyList()
)

enum class SegmentType {
    PLAIN, BOLD, ITALIC, BOLD_ITALIC, INLINE_CODE, CODE_BLOCK,
    HEADER1, HEADER2, HEADER3, BULLET, NUMBERED, HORIZONTAL_RULE,
    STRIKETHROUGH, LINK, BLOCKQUOTE, TABLE_HEADER, TABLE_ROW
}

object MarkdownParser {

    fun parse(input: String): List<MarkdownSegment> {
        val segments = mutableListOf<MarkdownSegment>()
        val lines = input.split("\n")
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            when {
                // Code block
                trimmed.startsWith("```") -> {
                    val lang = trimmed.removePrefix("```").trim()
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    segments.add(MarkdownSegment(SegmentType.CODE_BLOCK, codeLines.joinToString("\n"), lang))
                }
                // Blockquote
                trimmed.startsWith("> ") -> {
                    val text = trimmed.removePrefix("> ")
                    segments.add(MarkdownSegment(SegmentType.BLOCKQUOTE, text, inlineSegments = parseInline(text)))
                }
                // Headers
                trimmed.startsWith("#### ") || trimmed.startsWith("### ") -> {
                    val text = if (trimmed.startsWith("#### ")) trimmed.removePrefix("#### ") else trimmed.removePrefix("### ")
                    segments.add(MarkdownSegment(SegmentType.HEADER3, text, inlineSegments = parseInline(text)))
                }
                trimmed.startsWith("## ") -> {
                    val text = trimmed.removePrefix("## ")
                    segments.add(MarkdownSegment(SegmentType.HEADER2, text, inlineSegments = parseInline(text)))
                }
                trimmed.startsWith("# ") -> {
                    val text = trimmed.removePrefix("# ")
                    segments.add(MarkdownSegment(SegmentType.HEADER1, text, inlineSegments = parseInline(text)))
                }
                // Bullet list
                line.trimStart().let { t -> t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ") } -> {
                    val t = line.trimStart()
                    val text = t.drop(2)
                    val indent = line.length - t.length
                    segments.add(MarkdownSegment(SegmentType.BULLET, text,
                        language = indent.toString(), inlineSegments = parseInline(text)))
                }
                // Numbered list
                line.trimStart().matches(Regex("^\\d+[.)\\s].+")) -> {
                    val text = line.trimStart().replace(Regex("^\\d+[.)\\s]\\s*"), "")
                    segments.add(MarkdownSegment(SegmentType.NUMBERED, text, inlineSegments = parseInline(text)))
                }
                // Horizontal rule
                trimmed == "---" || trimmed == "***" || trimmed == "___" || trimmed.matches(Regex("-{3,}")) ->
                    segments.add(MarkdownSegment(SegmentType.HORIZONTAL_RULE, ""))
                // Table separator row |---|---| - skip
                trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.matches(Regex("\\|[\\s\\-:|]+\\|([\\s\\-:|]+\\|)*")) -> {
                    // skip separator
                }
                // Table row |cell|cell|
                trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                    val rawCells = splitTableRow(trimmed)
                    val parsedCells = rawCells.map { cell -> parseInline(cell.trim()) }
                    // Check if next non-empty line is a separator => this is a header
                    val nextLine = lines.drop(i + 1).firstOrNull { it.trim().isNotEmpty() }?.trim() ?: ""
                    val isHeader = nextLine.startsWith("|") && nextLine.matches(Regex("\\|[\\s\\-:|]+\\|([\\s\\-:|]+\\|)*"))
                    segments.add(MarkdownSegment(
                        type = if (isHeader) SegmentType.TABLE_HEADER else SegmentType.TABLE_ROW,
                        text = trimmed,
                        cells = parsedCells
                    ))
                }
                // Blank line
                trimmed.isBlank() -> {
                    if (segments.isNotEmpty()) segments.add(MarkdownSegment(SegmentType.PLAIN, ""))
                }
                // Plain text
                else -> {
                    val inline = parseInline(line)
                    segments.add(MarkdownSegment(SegmentType.PLAIN, line, inlineSegments = inline))
                }
            }
            i++
        }
        return segments
    }

    // Split table row respecting inline content (don't split on | inside **)
    private fun splitTableRow(row: String): List<String> {
        val content = row.trim().removeSurrounding("|", "|").trim()
        val cells = mutableListOf<String>()
        var current = StringBuilder()
        var insideBold = false
        var j = 0
        while (j < content.length) {
            when {
                j + 1 < content.length && content[j] == '*' && content[j+1] == '*' -> {
                    insideBold = !insideBold
                    current.append("**")
                    j += 2
                }
                content[j] == '|' && !insideBold -> {
                    cells.add(current.toString().trim())
                    current = StringBuilder()
                    j++
                }
                else -> { current.append(content[j]); j++ }
            }
        }
        if (current.isNotEmpty()) cells.add(current.toString().trim())
        return cells
    }

    fun parseInline(text: String): List<MarkdownSegment> {
        val result = mutableListOf<MarkdownSegment>()
        var i = 0
        val sb = StringBuilder()

        fun flushPlain() {
            if (sb.isNotEmpty()) { result.add(MarkdownSegment(SegmentType.PLAIN, sb.toString())); sb.clear() }
        }

        while (i < text.length) {
            val ch = text[i]
            when {
                // Bold italic ***
                ch == '*' && i + 2 < text.length && text[i+1] == '*' && text[i+2] == '*' -> {
                    val end = text.indexOf("***", i + 3)
                    if (end != -1) { flushPlain(); result.add(MarkdownSegment(SegmentType.BOLD_ITALIC, text.substring(i+3, end).trim())); i = end + 3 }
                    else { sb.append(ch); i++ }
                }
                // Bold **
                ch == '*' && i + 1 < text.length && text[i+1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) { flushPlain(); result.add(MarkdownSegment(SegmentType.BOLD, text.substring(i+2, end).trim())); i = end + 2 }
                    else { sb.append(ch); i++ }
                }
                // Bold __
                ch == '_' && i + 1 < text.length && text[i+1] == '_' -> {
                    val end = text.indexOf("__", i + 2)
                    if (end != -1) { flushPlain(); result.add(MarkdownSegment(SegmentType.BOLD, text.substring(i+2, end).trim())); i = end + 2 }
                    else { sb.append(ch); i++ }
                }
                // Strikethrough ~~
                ch == '~' && i + 1 < text.length && text[i+1] == '~' -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) { flushPlain(); result.add(MarkdownSegment(SegmentType.STRIKETHROUGH, text.substring(i+2, end))); i = end + 2 }
                    else { sb.append(ch); i++ }
                }
                // Italic * (not **)
                ch == '*' && (i + 1 >= text.length || text[i+1] != '*') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1 && end > i + 1) { flushPlain(); result.add(MarkdownSegment(SegmentType.ITALIC, text.substring(i+1, end))); i = end + 1 }
                    else { sb.append(ch); i++ }
                }
                // Italic _ (not __)
                ch == '_' && (i + 1 >= text.length || text[i+1] != '_') -> {
                    val end = text.indexOf('_', i + 1)
                    if (end != -1 && end > i + 1) { flushPlain(); result.add(MarkdownSegment(SegmentType.ITALIC, text.substring(i+1, end))); i = end + 1 }
                    else { sb.append(ch); i++ }
                }
                // Inline code `
                ch == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end != -1) { flushPlain(); result.add(MarkdownSegment(SegmentType.INLINE_CODE, text.substring(i+1, end))); i = end + 1 }
                    else { sb.append(ch); i++ }
                }
                // Link [text](url)
                ch == '[' -> {
                    val closeBracket = text.indexOf(']', i)
                    if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket+1] == '(') {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            flushPlain()
                            result.add(MarkdownSegment(SegmentType.LINK,
                                text.substring(i+1, closeBracket),
                                language = text.substring(closeBracket+2, closeParen)))
                            i = closeParen + 1
                        } else { sb.append(ch); i++ }
                    } else { sb.append(ch); i++ }
                }
                else -> { sb.append(ch); i++ }
            }
        }
        flushPlain()
        return result
    }
}

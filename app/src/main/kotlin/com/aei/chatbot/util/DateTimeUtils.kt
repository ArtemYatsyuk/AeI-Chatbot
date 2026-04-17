package com.aei.chatbot.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private fun getFormatter(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

    fun formatTime(timestamp: Long): String = getFormatter("HH:mm").format(Date(timestamp))
    fun formatDate(timestamp: Long): String = getFormatter("MMM d").format(Date(timestamp))
    fun formatFull(timestamp: Long): String = getFormatter("MMM d, HH:mm").format(Date(timestamp))
    fun formatExportTimestamp(): String = getFormatter("yyyyMMdd_HHmmss").format(Date())
}

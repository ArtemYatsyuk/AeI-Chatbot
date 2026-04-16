package com.aei.chatbot.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullFormatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    private val exportFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun formatTime(epochMillis: Long): String = timeFormatter.format(Date(epochMillis))

    fun formatDate(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - epochMillis
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> timeFormatter.format(Date(epochMillis))
            diff < 604_800_000 -> dateFormatter.format(Date(epochMillis))
            else -> fullFormatter.format(Date(epochMillis))
        }
    }

    fun formatExportTimestamp(): String = exportFormatter.format(Date())
}

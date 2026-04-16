package com.aei.chatbot.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardUtils {
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}

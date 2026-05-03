package com.aei.chatbot.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.aei.chatbot.domain.model.AiAction
import java.io.File

object AiActionExecutor {

    sealed class Result {
        data class Success(val message: String) : Result()
        data class Failure(val reason: String)  : Result()
    }

    fun execute(context: Context, action: AiAction): Result = when (action) {

        // ── Open App ──────────────────────────────────────────────────────────
        is AiAction.OpenApp -> launchApp(context, action.packageName, action.appLabel)

        // ── Open Map ──────────────────────────────────────────────────────────
        is AiAction.OpenMap -> {
            val label = action.label.ifBlank { action.query }
            try {
                val encodedQuery = Uri.encode(action.query)
                // geo: URI — opens any installed map app (Google Maps, Waze, etc.)
                val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedQuery")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(geoIntent)
                Result.Success("Opened map: $label")
            } catch (_: Exception) {
                // Fallback: Google Maps web URL
                try {
                    val webQuery = Uri.encode(action.query)
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$webQuery")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(webIntent)
                    Result.Success("Opened Google Maps for: $label")
                } catch (e: Exception) {
                    Result.Failure("Could not open map: ${e.message}")
                }
            }
        }

        // ── Create File ───────────────────────────────────────────────────────
        is AiAction.CreateFile -> {
            try {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, action.fileName)
                file.writeText(action.content)
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                Result.Success("Created '${action.fileName}' in Downloads")
            } catch (e: Exception) {
                Result.Failure("Failed to create file: ${e.message}")
            }
        }

        // ── Open URL ──────────────────────────────────────────────────────────
        is AiAction.OpenUrl -> {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(action.url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                Result.Success("Opened ${action.title.ifBlank { action.url }}")
            } catch (e: Exception) {
                Result.Failure("Could not open URL: ${e.message}")
            }
        }
    }

    // ── App launching with 4-level fallback chain ─────────────────────────────

    private fun launchApp(context: Context, rawPackage: String, label: String): Result {
        val pm = context.packageManager

        // Step 1 — resolve package name through lookup table + corrections
        val resolvedPkg = AiActionParser.resolvePackage(rawPackage, label)

        // All candidates to try (resolved first, then original if different)
        val candidates = listOfNotNull(
            resolvedPkg,
            rawPackage.takeIf { it != resolvedPkg && it.contains(".") }
        ).distinct()

        // Step 2 — try each candidate package directly
        for (pkg in candidates) {
            val result = tryLaunchPackage(context, pm, pkg)
            if (result != null) return Result.Success("Opened $label")
        }

        // Step 3 — fuzzy search among ALL installed apps by label
        val fuzzyPkg = findByLabel(pm, label) ?: findByLabel(pm, rawPackage)
        if (fuzzyPkg != null) {
            val result = tryLaunchPackage(context, pm, fuzzyPkg)
            if (result != null) return Result.Success("Opened $label")
        }

        // Step 4 — try a generic ACTION_VIEW with a custom scheme that many apps register
        // (e.g. youtube://, spotify://) — only for known schemes
        val scheme = knownScheme(label, resolvedPkg)
        if (scheme != null) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(scheme)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                return Result.Success("Opened $label")
            } catch (_: Exception) {}
        }

        // Step 5 — Play Store as true last resort
        return openPlayStore(context, resolvedPkg, label)
    }

    private fun tryLaunchPackage(context: Context, pm: PackageManager, pkg: String): Unit? {
        return try {
            // Try getLaunchIntentForPackage first
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return Unit
            }
            // Also try querying for LAUNCHER activities explicitly — more reliable on AOSP
            val queryIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                `package` = pkg
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PackageManager.MATCH_ALL else 0
            val activities = pm.queryIntentActivities(queryIntent, flags)
            if (activities.isNotEmpty()) {
                val actInfo = activities.first().activityInfo
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(actInfo.packageName, actInfo.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(intent)
                Unit
            } else null
        } catch (_: Exception) { null }
    }

    private fun findByLabel(pm: PackageManager, name: String): String? {
        if (name.isBlank()) return null
        val needle = name.lowercase().trim()
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                PackageManager.MATCH_UNINSTALLED_PACKAGES.inv() and 0 else 0
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.enabled }
                .firstOrNull { appInfo ->
                    val lbl = pm.getApplicationLabel(appInfo).toString().lowercase()
                    lbl == needle || lbl.contains(needle) || needle.contains(lbl)
                }?.packageName
        } catch (_: Exception) { null }
    }

    private fun knownScheme(label: String, pkg: String): String? {
        val key = "${label.lowercase()}|${pkg.lowercase()}"
        return APP_SCHEMES.entries.firstOrNull { (k, _) ->
            key.contains(k) || k.contains(label.lowercase())
        }?.value
    }

    private fun openPlayStore(context: Context, pkg: String, label: String): Result {
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            Result.Success("'$label' not installed — opening Play Store")
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                Result.Success("'$label' not installed — opening Play Store")
            } catch (e: Exception) {
                Result.Failure("App '$label' not found and Play Store unavailable: ${e.message}")
            }
        }
    }

    /** Custom URI schemes that bypass package name issues for some apps */
    private val APP_SCHEMES = mapOf(
        "youtube"  to "vnd.youtube://",
        "spotify"  to "spotify://",
        "viber"    to "viber://",
        "whatsapp" to "whatsapp://",
        "telegram" to "tg://",
        "twitter"  to "twitter://",
        "discord"  to "discord://",
        "slack"    to "slack://",
        "zoom"     to "zoomus://",
        "instagram" to "instagram://",
        "snapchat" to "snapchat://",
        "tiktok"   to "snssdk1233://",
        "reddit"   to "reddit://",
        "netflix"  to "nflx://",
    )
}
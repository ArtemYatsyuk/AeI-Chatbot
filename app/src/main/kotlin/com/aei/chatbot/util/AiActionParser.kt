package com.aei.chatbot.util

import com.aei.chatbot.domain.model.AiAction
import com.aei.chatbot.domain.model.PendingAiAction

/**
 * Parses AI responses for special action blocks.
 *
 * Syntax the AI emits (at the END of its response):
 *   %%ACTION:OPEN_APP|packageName|Human Label%%
 *   %%ACTION:CREATE_FILE|fileName|mimeType|file content%%
 *   %%ACTION:OPEN_URL|https://...|Page Title%%
 *   %%ACTION:OPEN_MAP|query|Display Label%%
 *
 * All tags are stripped from the visible text.
 *
 * Bug fixes vs v1:
 *  - Regex now uses [\s\S] so it matches across line breaks and handles %20 / special chars in URLs
 *  - OPEN_MAP action added — uses geo: URI which opens Google Maps / any map app natively
 *  - App name → package resolution is now done here so the executor receives the right package
 */
object AiActionParser {

    // Match %%ACTION:TYPE|...%% — non-greedy, handles encoded URLs (%20 etc.)
    private val ACTION_REGEX = Regex("""%%ACTION:([A-Z_]+)\|(.+?)%%""", RegexOption.DOT_MATCHES_ALL)

    data class ParseResult(
        val cleanText: String,
        val actions: List<PendingAiAction>
    )

    fun parse(text: String): ParseResult {
        val found = mutableListOf<PendingAiAction>()

        val clean = ACTION_REGEX.replace(text) { match ->
            val type  = match.groupValues[1].trim()
            val rest  = match.groupValues[2].trim()
            val parts = rest.split("|")

            val action: AiAction? = when (type) {
                "OPEN_APP" -> {
                    val rawPkg  = parts.getOrNull(0)?.trim() ?: return@replace ""
                    val label   = parts.getOrNull(1)?.trim() ?: rawPkg
                    // Resolve by app name if the AI gave a name instead of a package
                    val pkg = resolvePackage(rawPkg, label)
                    AiAction.OpenApp(pkg, label)
                }
                "CREATE_FILE" -> {
                    val name    = parts.getOrNull(0)?.trim() ?: return@replace ""
                    val mime    = parts.getOrNull(1)?.trim() ?: "text/plain"
                    val content = parts.drop(2).joinToString("|")
                    AiAction.CreateFile(name, content, mime)
                }
                "OPEN_URL" -> {
                    val url   = parts.getOrNull(0)?.trim() ?: return@replace ""
                    val title = parts.getOrNull(1)?.trim() ?: url
                    // If it looks like a Maps search redirect it to a proper map action
                    if (isMapsUrl(url)) {
                        val query = extractMapsQuery(url)
                        AiAction.OpenMap(query, title)
                    } else {
                        AiAction.OpenUrl(url, title)
                    }
                }
                "OPEN_MAP" -> {
                    val query = parts.getOrNull(0)?.trim() ?: return@replace ""
                    val label = parts.getOrNull(1)?.trim() ?: query
                    AiAction.OpenMap(query, label)
                }
                else -> null
            }

            if (action != null) found += PendingAiAction(action, describeAction(action))
            "" // strip from visible text
        }.trim()

        return ParseResult(clean, found)
    }

    fun hasActions(text: String) = ACTION_REGEX.containsMatchIn(text)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isMapsUrl(url: String): Boolean {
        val l = url.lowercase()
        return (l.contains("google.com/maps") || l.contains("maps.google") ||
                l.contains("maps.app.goo") || l.contains("goo.gl/maps"))
    }

    private fun extractMapsQuery(url: String): String {
        // Extract the search query from a Google Maps URL
        // e.g. https://www.google.com/maps/search/nearest%20toilet -> "nearest toilet"
        return try {
            val decoded = java.net.URLDecoder.decode(url, "UTF-8")
            val patterns = listOf(
                Regex("""maps/search/([^?&#]+)"""),
                Regex("""[?&]q=([^&#]+)"""),
                Regex("""[?&]query=([^&#]+)"""),
                Regex("""place/([^/@?]+)""")
            )
            patterns.firstNotNullOfOrNull { it.find(decoded)?.groupValues?.get(1) }
                ?.replace("+", " ")?.trim()
                ?: decoded
        } catch (_: Exception) {
            url
        }
    }

    /**
     * Resolve a package name from what the AI gave us.
     * Many models output app names instead of package names, or use wrong packages.
     * This lookup covers the most common apps and falls back to the raw string.
     */
    fun resolvePackage(rawPkg: String, label: String): String {
        val key = rawPkg.lowercase().trim()
        val lbl = label.lowercase().trim()

        // If it already looks like a real package (contains dot, not a plain word), keep it
        // but still run through the correction table in case the AI got it wrong
        val fromTable = KNOWN_PACKAGES[key] ?: KNOWN_PACKAGES[lbl]
            ?: KNOWN_PACKAGES.entries.firstOrNull { (k, _) ->
                key.contains(k) || lbl.contains(k) || k.contains(lbl)
            }?.value

        return fromTable ?: rawPkg
    }

    private fun describeAction(action: AiAction): String = when (action) {
        is AiAction.OpenApp    -> "Open app: ${action.appLabel}"
        is AiAction.CreateFile -> "Create file: ${action.fileName}"
        is AiAction.OpenUrl    -> "Open URL: ${action.url}"
        is AiAction.OpenMap    -> "Open map: ${action.query}"
    }

    // ── Known app name → package table ───────────────────────────────────────
    // Covers ~100 popular apps. Keys are lowercase partial names.
    val KNOWN_PACKAGES: Map<String, String> = mapOf(
        // Communication
        "viber"                      to "com.viber.voip",
        "whatsapp"                   to "com.whatsapp",
        "telegram"                   to "org.telegram.messenger",
        "telegram x"                 to "org.thunderdog.challegram",
        "signal"                     to "org.thoughtcrime.securesms",
        "messenger"                  to "com.facebook.orca",
        "facebook messenger"         to "com.facebook.orca",
        "instagram"                  to "com.instagram.android",
        "facebook"                   to "com.facebook.katana",
        "twitter"                    to "com.twitter.android",
        "x (twitter)"                to "com.twitter.android",
        "discord"                    to "com.discord",
        "skype"                      to "com.skype.raider",
        "teams"                      to "com.microsoft.teams",
        "microsoft teams"            to "com.microsoft.teams",
        "slack"                      to "com.slack",
        "zoom"                       to "us.zoom.videomeetings",
        "google meet"                to "com.google.android.apps.meetings",
        "meet"                       to "com.google.android.apps.meetings",
        "snapchat"                   to "com.snapchat.android",
        "tiktok"                     to "com.zhiliaoapp.musically",
        "linkedin"                   to "com.linkedin.android",
        "reddit"                     to "com.reddit.frontpage",
        "pinterest"                  to "com.pinterest",
        "tumblr"                     to "com.tumblr",
        "line"                       to "jp.naver.line.android",
        "wechat"                     to "com.tencent.mm",
        "kik"                        to "kik.android",
        "hangouts"                   to "com.google.android.talk",
        "google chat"                to "com.google.android.apps.dynamite",
        "threema"                    to "ch.threema.app",
        "imo"                        to "com.imo.android.imoim",

        // Productivity & Office
        "gmail"                      to "com.google.android.gm",
        "google mail"                to "com.google.android.gm",
        "outlook"                    to "com.microsoft.office.outlook",
        "microsoft outlook"          to "com.microsoft.office.outlook",
        "calendar"                   to "com.google.android.calendar",
        "google calendar"            to "com.google.android.calendar",
        "google docs"                to "com.google.android.apps.docs.editors.docs",
        "docs"                       to "com.google.android.apps.docs.editors.docs",
        "google sheets"              to "com.google.android.apps.docs.editors.sheets",
        "sheets"                     to "com.google.android.apps.docs.editors.sheets",
        "google slides"              to "com.google.android.apps.docs.editors.slides",
        "slides"                     to "com.google.android.apps.docs.editors.slides",
        "google drive"               to "com.google.android.apps.docs",
        "drive"                      to "com.google.android.apps.docs",
        "microsoft word"             to "com.microsoft.office.word",
        "word"                       to "com.microsoft.office.word",
        "microsoft excel"            to "com.microsoft.office.excel",
        "excel"                      to "com.microsoft.office.excel",
        "microsoft powerpoint"       to "com.microsoft.office.powerpoint",
        "powerpoint"                 to "com.microsoft.office.powerpoint",
        "one drive"                  to "com.microsoft.skydrive",
        "onedrive"                   to "com.microsoft.skydrive",
        "notion"                     to "notion.id",
        "evernote"                   to "com.evernote",
        "keep"                       to "com.google.android.keep",
        "google keep"                to "com.google.android.keep",
        "tasks"                      to "com.google.android.apps.tasks",
        "todoist"                    to "com.todoist",
        "trello"                     to "com.trello",
        "asana"                      to "com.asana.app",
        "dropbox"                    to "com.dropbox.android",
        "box"                        to "com.box.android",

        // Navigation & Maps
        "maps"                       to "com.google.android.apps.maps",
        "google maps"                to "com.google.android.apps.maps",
        "waze"                       to "com.waze",
        "here maps"                  to "com.here.app.maps",
        "citymapper"                 to "com.citymapper.commuter",
        "moovit"                     to "com.tranzmate",
        "uber"                       to "com.ubercab",
        "lyft"                       to "me.lyft.android",
        "bolt"                       to "ee.mtakso.client",

        // Media & Entertainment
        "youtube"                    to "com.google.android.youtube",
        "youtube music"              to "com.google.android.apps.youtube.music",
        "spotify"                    to "com.spotify.music",
        "netflix"                    to "com.netflix.mediaclient",
        "prime video"                to "com.amazon.avod.thirdpartyclient",
        "amazon prime"               to "com.amazon.avod.thirdpartyclient",
        "disney+"                    to "com.disney.disneyplus",
        "hbo"                        to "com.hbo.hbonow",
        "twitch"                     to "tv.twitch.android.app",
        "hulu"                       to "com.hulu.plus",
        "apple music"                to "com.apple.android.music",
        "deezer"                     to "deezer.android.app",
        "tidal"                      to "com.aspiro.tidal",
        "soundcloud"                 to "com.soundcloud.android",
        "podcast"                    to "com.google.android.apps.podcasts",
        "google podcast"             to "com.google.android.apps.podcasts",
        "pocket casts"               to "au.com.shiftyjelly.pocketcasts",
        "vlc"                        to "org.videolan.vlc",
        "mx player"                  to "com.mxtech.videoplayer.ad",
        "plex"                       to "com.plexapp.android",

        // Shopping
        "amazon"                     to "com.amazon.mShop.android.shopping",
        "ebay"                       to "com.ebay.mobile",
        "aliexpress"                 to "com.alibaba.aliexpresshd",
        "wish"                       to "com.contextlogic.wish",
        "etsy"                       to "com.etsy.android",

        // Finance & Banking
        "paypal"                     to "com.paypal.android.p2pmobile",
        "revolut"                    to "com.revolut.revolut",
        "wise"                       to "com.transferwise.android",
        "cashapp"                    to "com.squareup.cash",
        "cash app"                   to "com.squareup.cash",
        "venmo"                      to "com.venmo",
        "coinbase"                   to "com.coinbase.android",
        "binance"                    to "com.binance.dev",

        // Google Apps
        "google"                     to "com.google.android.googlequicksearchbox",
        "google search"              to "com.google.android.googlequicksearchbox",
        "google play"                to "com.android.vending",
        "play store"                 to "com.android.vending",
        "chrome"                     to "com.android.chrome",
        "google chrome"              to "com.android.chrome",
        "google photos"              to "com.google.android.apps.photos",
        "photos"                     to "com.google.android.apps.photos",
        "google translate"           to "com.google.android.apps.translate",
        "translate"                  to "com.google.android.apps.translate",
        "google lens"                to "com.google.ar.lens",
        "lens"                       to "com.google.ar.lens",
        "google pay"                 to "com.google.android.apps.nbu.paisa.user",
        "gpay"                       to "com.google.android.apps.nbu.paisa.user",
        "google assistant"           to "com.google.android.apps.googleassistant",
        "google fit"                 to "com.google.android.apps.fitness",
        "google news"                to "com.google.android.apps.magazines",
        "google one"                 to "com.google.android.apps.subscriptions.red",
        "google classroom"           to "com.google.android.apps.classroom",
        "google earth"               to "com.google.earth",

        // Browsers
        "firefox"                    to "org.mozilla.firefox",
        "brave"                      to "com.brave.browser",
        "opera"                      to "com.opera.browser",
        "edge"                       to "com.microsoft.emmx",
        "microsoft edge"             to "com.microsoft.emmx",
        "samsung browser"            to "com.sec.android.app.sbrowser",
        "duckduckgo"                 to "com.duckduckgo.mobile.android",

        // Health & Fitness
        "strava"                     to "com.strava",
        "myfitnesspal"               to "com.myfitnesspal.android",
        "fitbit"                     to "com.fitbit.FitbitMobile",
        "headspace"                  to "com.getsomeheadspace.android",
        "calm"                       to "com.calm.android",
        "duolingo"                   to "com.duolingo",

        // News
        "bbc"                        to "bbc.mobile.news.ww",
        "cnn"                        to "com.cnn.mobile.android.phone",

        // Utilities
        "shazam"                     to "com.shazam.android",
        "1password"                  to "com.agilebits.onepassword",
        "lastpass"                   to "com.lastpass.lpandroid",
        "bitwarden"                  to "com.x8bit.bitwarden",
        "qr scanner"                 to "me.scan.android.client",
        "files"                      to "com.google.android.apps.nbu.files",
        "google files"               to "com.google.android.apps.nbu.files",

        // Package name corrections (when AI outputs wrong package)
        "com.viber"                  to "com.viber.voip",
        "viber.voip"                 to "com.viber.voip",
        "com.telegram"               to "org.telegram.messenger",
        "com.whatsapp.whatsapp"      to "com.whatsapp",
        "com.google.youtube"         to "com.google.android.youtube",
        "youtube.android"            to "com.google.android.youtube",
        "com.spotify"                to "com.spotify.music",
    )

    /** System prompt injected when AI Actions is enabled */
    val SYSTEM_PROMPT_INJECTION = """

You have the ability to perform device actions. Use these ONLY when the user explicitly asks you to open an app, find/open a location, create a file, or open a URL.

Emit ONE action at the very END of your response using this exact syntax:

Open an app:        %%ACTION:OPEN_APP|com.package.name|App Name%%
Open a map/place:   %%ACTION:OPEN_MAP|search query|Display Label%%
Create a file:      %%ACTION:CREATE_FILE|filename.txt|text/plain|file content%%
Open a URL:         %%ACTION:OPEN_URL|https://example.com|Page Title%%

Important rules:
- ALWAYS use OPEN_MAP (not OPEN_URL) for any location, place, address, or navigation request
- For OPEN_MAP the query can be a place name, address, or type ("nearest pharmacy", "Eiffel Tower", "pizza near me")
- For OPEN_APP use the exact Android package name. Common apps: YouTube=com.google.android.youtube, Viber=com.viber.voip, WhatsApp=com.whatsapp, Spotify=com.spotify.music, Maps=com.google.android.apps.maps, Chrome=com.android.chrome
- Only emit an action when the user EXPLICITLY asks for it
- Never emit an action the user did not request
- The action tag is automatically stripped from your visible response
"""
}
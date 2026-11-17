package com.reelscounter.local

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Heuristic only: app layouts vary; no screen content is persisted. */
class ReelsAccessibilityService : AccessibilityService() {
    private lateinit var store: SecureStore
    private var activePackage = ""
    private var lastCountAt = 0L
    private var lastSeenShortAt = 0L
    private var lastSignature = ""
    override fun onServiceConnected() { super.onServiceConnected(); store = SecureStore(this) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        val platform = when (pkg) {
            "com.instagram.android" -> "instagram"
            "com.google.android.youtube" -> "youtube"
            "com.facebook.katana", "com.facebook.lite" -> "facebook"
            else -> return
        }
        val now = System.currentTimeMillis()
        if (pkg != activePackage) { activePackage = pkg; lastSeenShortAt = 0L; lastSignature = "" }
        val root = rootInActiveWindow ?: return
        val text = collectVisibleText(root, 0, StringBuilder()).toString().lowercase(Locale.ROOT)
        val shortScreen = when (platform) {
            "youtube" -> text.contains("shorts") || nodeIdContains(root, "shorts", 0)
            "instagram" -> text.contains("reels") || text.contains("reel") || nodeIdContains(root, "clips", 0)
            else -> text.contains("reels") || text.contains("reel")
        }
        if (!shortScreen) { lastSeenShortAt = 0L; return }
        val scroll = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        val screenChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        // Count the first short-form screen, then meaningful scrolls. Debounce UI noise.
        val initial = lastSeenShortAt == 0L && (screenChanged || now - lastCountAt > 2000L)
        val signature = "${event.fromIndex}:${event.toIndex}:${event.scrollY}:${event.maxScrollY}"
        val meaningfulScroll = scroll && (signature != lastSignature || now - lastCountAt > 2500L)
        if ((initial || meaningfulScroll) && now - lastCountAt > 1100L) {
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            store.increment(day, platform)
            lastCountAt = now
        }
        if (scroll) lastSignature = signature
        lastSeenShortAt = now
    }
    private fun collectVisibleText(node: AccessibilityNodeInfo, depth: Int, out: StringBuilder): StringBuilder {
        if (depth > 8 || out.length > 4500) return out
        node.text?.let { out.append(' ').append(it.take(120)) }
        node.contentDescription?.let { out.append(' ').append(it.take(120)) }
        for (i in 0 until minOf(node.childCount, 35)) node.getChild(i)?.let { collectVisibleText(it, depth + 1, out) }
        return out
    }
    private fun nodeIdContains(node: AccessibilityNodeInfo, term: String, depth: Int): Boolean {
        if (depth > 7) return false
        if (node.viewIdResourceName?.contains(term, true) == true) return true
        for (i in 0 until minOf(node.childCount, 35)) if (node.getChild(i)?.let { nodeIdContains(it, term, depth + 1) } == true) return true
        return false
    }
    override fun onInterrupt() {}
}

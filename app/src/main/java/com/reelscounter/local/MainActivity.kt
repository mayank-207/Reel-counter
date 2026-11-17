package com.reelscounter.local

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private val store by lazy { SecureStore(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(17, 24, 39)
        render()
    }
    override fun onResume() { super.onResume(); render() }
    private fun label(text: String, size: Float = 18f, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(Color.rgb(28, 35, 48))
        setPadding(0, 12, 0, 12)
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
    }
    private fun addButton(title: String, click: () -> Unit) {
        content.addView(Button(this).apply { text = title; setOnClickListener { click() } })
    }
    private fun render() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(248, 250, 252)) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 40, 36, 48)
            gravity = Gravity.START
        }
        scroll.addView(content)
        setContentView(scroll)
        content.addView(label("Reels Counter", 29f, true))
        content.addView(label("Private, on-device scrolling analytics", 14f))
        val data = store.read()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val day = data.optJSONObject(today)
        val instagram = day?.optInt("instagram", 0) ?: 0
        val youtube = day?.optInt("youtube", 0) ?: 0
        val facebook = day?.optInt("facebook", 0) ?: 0
        content.addView(label("TODAY · $today", 14f, true))
        content.addView(label("${instagram + youtube + facebook} short videos detected", 26f, true))
        content.addView(label("Instagram Reels    $instagram"))
        content.addView(label("YouTube Shorts       $youtube"))
        content.addView(label("Facebook Reels       $facebook"))
        val calendar = Calendar.getInstance()
        var weekly = 0
        repeat(7) { i ->
            calendar.time = java.util.Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            val item = data.optJSONObject(date)
            weekly += (item?.optInt("instagram", 0) ?: 0) + (item?.optInt("youtube", 0) ?: 0) + (item?.optInt("facebook", 0) ?: 0)
        }
        content.addView(label("Last 7 days: $weekly", 20f, true))
        content.addView(label("Tracking requires Accessibility permission. Detection is approximate and may vary by app version.", 14f))
        addButton("Enable / manage tracking") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        addButton("Refresh counts") { render() }
        addButton("Erase all local data") {
            android.app.AlertDialog.Builder(this).setTitle("Delete encrypted history?")
                .setMessage("This permanently removes all recorded counters on this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ -> store.clear(); render() }.show()
        }
        content.addView(label("No login • No internet permission • Android Keystore encryption • Backup disabled", 13f))
    }
}

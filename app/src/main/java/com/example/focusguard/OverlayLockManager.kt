package com.example.focusguard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class OverlayLockManager(private val context: Context, private val onHome: () -> Unit, private val onSettingsPass: () -> Unit) {
    private val manager = context.getSystemService(WindowManager::class.java)
    private var view: View? = null
    private var shownFor = ""
    val isShowing: Boolean get() = view != null

    fun showApp(app: ManagedApp, demo: Boolean = false) {
        val key = "${app.packageName}:$demo"
        if (shownFor == key && view != null) return
        val detail = if (demo) "DEMO · ${app.name} đang được khóa để trình diễn. Không thay đổi thời gian sử dụng thật."
            else "${app.name} đã đạt giới hạn ${app.limitMinutes} phút hôm nay."
        show(key, "Đến lúc nghỉ một chút", "$detail\n\nHoàn thành một thử thách để mở thêm 5 phút.") { body ->
            button(body, "Vận động · Lắc 30 lần") { challenge(app.packageName, "shake") }
            button(body, "Học từ vựng · Đúng 5 câu") { challenge(app.packageName, "quiz") }
            button(body, "Về màn hình chính", onHome)
        }
    }

    fun showSettings() {
        if (shownFor == "settings" && view != null) return
        show("settings", "Chế độ nghiêm ngặt", "Bạn đang bảo vệ thời gian tập trung.\n\nNếu cần thay đổi quyền hoặc gỡ ứng dụng, bạn vẫn có thể tạm mở Cài đặt.") { body ->
            button(body, "Tiếp tục tập trung", onHome)
            button(body, "Tạm mở Cài đặt 60 giây", onSettingsPass)
            button(body, "Tắt chế độ nghiêm ngặt") {
                FocusRepository(context).strict = false
                hide()
            }
        }
    }

    private fun challenge(pkg: String, mode: String) {
        runCatching {
            context.startActivity(Intent(context, ChallengeActivity::class.java)
                .putExtra("package", pkg).putExtra("mode", mode)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }.onFailure { android.widget.Toast.makeText(context, "Không mở được thử thách. Hãy thử lại.", android.widget.Toast.LENGTH_SHORT).show() }
    }

    private fun show(key: String, title: String, subtitle: String, content: (LinearLayout) -> Unit) {
        hide()
        if (!Settings.canDrawOverlays(context)) return
        val scroll = ScrollView(context).apply { setBackgroundColor(Color.rgb(243, 247, 245)); isFillViewport = true }
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(64), dp(28), dp(40))
        }
        scroll.addView(body)
        text(body, "F O C U S G U A R D", 14, Color.rgb(29, 107, 80))
        text(body, "◷", 72, Color.rgb(29, 107, 80))
        text(body, title, 28, Color.rgb(20, 40, 33))
        text(body, subtitle, 17, Color.rgb(87, 105, 96))
        content(body)
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            type, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        runCatching { manager.addView(scroll, params) }.onSuccess { view = scroll; shownFor = key }
    }

    private fun text(body: LinearLayout, value: String, size: Int, color: Int) {
        body.addView(TextView(context).apply {
            text = value; textSize = size.toFloat(); setTextColor(color); gravity = Gravity.CENTER
            if (size >= 28) setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(16))
        })
    }

    private fun button(body: LinearLayout, title: String, action: () -> Unit) {
        body.addView(Button(context).apply {
            text = title; isAllCaps = false; textSize = 16f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.rgb(29, 107, 80)); cornerRadius = dp(18).toFloat() }
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(-1, dp(60)).apply { topMargin = dp(12) })
    }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun hide() {
        view?.let { runCatching { manager.removeViewImmediate(it) } }
        view = null; shownFor = ""
    }
}

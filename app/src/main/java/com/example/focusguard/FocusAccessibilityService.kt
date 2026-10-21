package com.example.focusguard

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.HandlerThread
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repo: FocusRepository
    private lateinit var overlay: OverlayLockManager
    private var currentPackage = ""
    private var currentUsage = 0L
    private var usageAt = 0L
    private var currentDay = 0L
    private var settingsPassUntil = 0L
    private val workerThread = HandlerThread("FocusGuardUsage").apply { start() }
    private val worker = Handler(workerThread.looper)
    private var reading = false
    private var destroyed = false
    private val loop = object : Runnable {
        override fun run() {
            checkCurrent()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onServiceConnected() {
        repo = FocusRepository(this)
        overlay = OverlayLockManager(this,
            onHome = { performGlobalAction(GLOBAL_ACTION_HOME) },
            onSettingsPass = { settingsPassUntil = System.currentTimeMillis() + 60_000; overlay.hide() })
        connected = true
        handler.post(loop)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::overlay.isInitialized || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // System UI (volume/notifications) must not erase the app being monitored.
        if (pkg == "com.android.systemui" || pkg == "android") return
        if (pkg == packageName && overlay.isShowing) return
        if (pkg != currentPackage) {
            currentPackage = pkg
            usageAt = 0L
            overlay.hide()
        }
        checkCurrent()
    }

    private fun checkCurrent() {
        if (!::overlay.isInitialized) return
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        val now = System.currentTimeMillis()
        if (!power.isInteractive || keyguard.isKeyguardLocked || !repo.enabled || challengeActive || currentPackage == packageName) {
            overlay.hide(); return
        }
        if (repo.strict && currentPackage == "com.android.settings" && now >= settingsPassUntil) {
            overlay.showSettings(); return
        }
        val app = repo.apps().find { it.packageName == currentPackage }
        if (app == null || !UsageReader.permitted(this)) { overlay.hide(); return }
        val day = FocusRepository.dayStart(now)
        // Refresh from Android while the same application remains open, including after midnight.
        if (now - usageAt >= 2000L || day != currentDay) {
            if (!reading) {
                reading = true
                val queriedPackage = app.packageName
                worker.post {
                    val value = runCatching { UsageReader.read(this)[queriedPackage] ?: 0L }.getOrDefault(0L)
                    handler.post {
                        reading = false
                        if (!destroyed && currentPackage == queriedPackage) {
                            currentUsage = value
                            usageAt = System.currentTimeMillis()
                            currentDay = day
                            checkCurrent()
                        }
                    }
                }
            }
            // Don't make a decision using the previous application's cached usage.
            if (usageAt == 0L || day != currentDay) return
        }
        val demo = app.packageName in repo.demoPackages()
        if (FocusPolicy.blocked(repo.enabled, currentUsage, app.limitMinutes, repo.rewardUntil(app.packageName), now, demo)) {
            overlay.showApp(app, demo)
        } else overlay.hide()
    }

    override fun onInterrupt() { if (::overlay.isInitialized) overlay.hide() }
    override fun onDestroy() {
        connected = false
        destroyed = true
        workerThread.quitSafely()
        handler.removeCallbacksAndMessages(null)
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
    }

    companion object {
        @Volatile var connected = false
        @Volatile var challengeActive = false
    }
}

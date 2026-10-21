package com.example.focusguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class FocusRepository(context: Context) {
    private val prefs = context.getSharedPreferences("focusguard", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }
    var strict: Boolean
        get() = prefs.getBoolean("strict", false)
        set(value) { prefs.edit().putBoolean("strict", value).apply() }

    fun apps(): List<ManagedApp> = runCatching {
        val data = JSONArray(prefs.getString("apps", "[]"))
        List(data.length()) { i ->
            val o = data.getJSONObject(i)
            ManagedApp(o.getString("package"), o.getString("name"), o.getInt("limit").coerceIn(5, 180))
        }
    }.getOrDefault(emptyList())

    fun saveApps(apps: List<ManagedApp>) {
        val data = JSONArray()
        apps.distinctBy { it.packageName }.forEach {
            data.put(JSONObject().put("package", it.packageName).put("name", it.name).put("limit", it.limitMinutes))
        }
        prefs.edit().putString("apps", data.toString()).apply()
    }

    fun initialize(context: Context) {
        if (prefs.getBoolean("initialized", false)) return
        val defaults = listOf("com.facebook.katana", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        val apps = defaults.mapNotNull { pkg -> runCatching {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            ManagedApp(pkg, context.packageManager.getApplicationLabel(info).toString())
        }.getOrNull() }
        saveApps(apps)
        prefs.edit().putBoolean("initialized", true).apply()
    }

    fun rewardUntil(pkg: String): Long {
        if (prefs.getLong("rewardDay:$pkg", 0L) != dayStart()) return 0L
        return prefs.getLong("reward:$pkg", 0L)
    }

    fun demoPackages(): Set<String> = if (prefs.getLong("demoDay", 0L) == dayStart())
        prefs.getStringSet("demoPackages", emptySet())?.toSet().orEmpty() else emptySet()

    fun startDemo(targets: List<ManagedApp>) {
        if (targets.isEmpty()) return
        val merged = apps().associateBy { it.packageName }.toMutableMap()
        targets.forEach { if (it.packageName !in merged) merged[it.packageName] = it }
        saveApps(merged.values.toList())
        val edit = prefs.edit()
        if (!prefs.contains("demoPreviousEnabled")) edit.putBoolean("demoPreviousEnabled", enabled)
        edit.putBoolean("enabled", true).putLong("demoDay", dayStart())
            .putStringSet("demoPackages", targets.map { it.packageName }.toSet())
        targets.forEach { edit.remove("reward:${it.packageName}").remove("rewardDay:${it.packageName}") }
        edit.apply()
    }

    fun stopDemo() {
        val edit = prefs.edit()
        if (prefs.contains("demoPreviousEnabled")) edit.putBoolean("enabled", prefs.getBoolean("demoPreviousEnabled", false))
        edit.remove("demoPackages").remove("demoDay").remove("demoPreviousEnabled").apply()
    }

    fun installedSocialApps(context: Context): List<ManagedApp> = SOCIAL_PACKAGES.mapNotNull { pkg ->
        runCatching {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            ManagedApp(pkg, context.packageManager.getApplicationLabel(info).toString())
        }.getOrNull()
    }

    fun grantReward(pkg: String) {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("reward:$pkg", FocusPolicy.rewardExpiry(now, nextMidnight()))
            .putLong("rewardDay:$pkg", dayStart(now))
            .putInt("completed:${dayStart(now)}", completedToday() + 1).apply()
    }

    fun completedToday(): Int = prefs.getInt("completed:${dayStart()}", 0)

    companion object {
        val SOCIAL_PACKAGES = setOf("com.facebook.katana", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        fun dayStart(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        fun nextMidnight(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
            timeInMillis = dayStart(now); add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}

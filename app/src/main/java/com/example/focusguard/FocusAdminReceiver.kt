package com.example.focusguard

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class FocusAdminReceiver : DeviceAdminReceiver() {
    override fun onDisabled(context: Context, intent: Intent) { FocusRepository(context).strict = false }
}

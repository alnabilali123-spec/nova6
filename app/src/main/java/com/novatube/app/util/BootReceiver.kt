package com.novatube.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // WorkManager persists scheduled work; we just wake the application
        // context so the DB is ready and channels exist when the user
        // opens the app after a reboot.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launch?.let { /* reference retained to avoid unused-warning */ }
    }
}

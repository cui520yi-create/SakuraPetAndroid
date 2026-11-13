package com.example.sakurapet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (Settings.canDrawOverlays(context)) {
                val service = Intent(context, PetOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service)
                else context.startService(service)
            }
        }
    }
}

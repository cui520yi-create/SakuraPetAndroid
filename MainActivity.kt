package com.example.sakurapet

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 36, 36, 36)
        }
        root.addView(TextView(this).apply {
            text = "🌸 银发樱花桌宠\n\n开启悬浮窗权限后，她就能陪伴你使用手机。"
            textSize = 20f
            gravity = Gravity.CENTER
        })
        root.addView(Button(this).apply {
            text = "① 开启悬浮窗权限"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else Toast.makeText(this@MainActivity, "悬浮窗权限已开启", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(Button(this).apply {
            text = "② 启动桌宠"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    Toast.makeText(this@MainActivity, "请先开启悬浮窗权限", Toast.LENGTH_LONG).show()
                } else {
                    val i = Intent(this@MainActivity, PetOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
                    Toast.makeText(this@MainActivity, "桌宠已启动，点击角色互动，长按打开菜单", Toast.LENGTH_LONG).show()
                }
            }
        })
        root.addView(Button(this).apply {
            text = "③ 停止桌宠"
            setOnClickListener { stopService(Intent(this@MainActivity, PetOverlayService::class.java)) }
        })
        root.addView(TextView(this).apply {
            text = "\n提示：首次启动需手动允许悬浮窗。部分手机还需在系统设置中允许自启动及后台运行。"
            textSize = 14f
        })
        setContentView(root)
    }
}

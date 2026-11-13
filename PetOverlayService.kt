package com.example.sakurapet

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.*
import android.widget.*
import kotlin.math.max

class PetOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var pet: ImageView? = null
    private var menu: LinearLayout? = null
    private lateinit var params: WindowManager.LayoutParams
    private var actionIndex = 0
    private var affection = 0
    private var fullness = 60
    private val actions = listOf("帅气拔刀！", "樱花剑气！", "结印施法！", "潇洒转身！", "傲娇抱臂！")

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPet()
    }

    private fun startAsForeground() {
        val channel = "sakura_pet_service"
        if (Build.VERSION.SDK_INT >= 26) {
            val c = NotificationChannel(channel, "桌宠后台运行", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, if (Build.VERSION.SDK_INT >= 26) channel else "")
            .setContentTitle("银发樱花桌宠正在陪伴你")
            .setContentText("点击通知打开桌宠控制页")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(open).setOngoing(true).build()
        startForeground(7, n)
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun showPet() {
        val image = ImageView(this).apply {
            setImageResource(R.drawable.pet)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = "银发樱花桌宠"
        }
        val width = (resources.displayMetrics.widthPixels * 0.34f).toInt().coerceIn(170, 340)
        params = WindowManager.LayoutParams(width, width * 1.5f, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - width
            y = resources.displayMetrics.heightPixels / 3
        }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        var moved = false
        image.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 12) moved = true
                    params.x = startX + dx; params.y = startY + dy
                    wm.updateViewLayout(image, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) playAction()
                    true
                }
                else -> true
            }
        }
        image.setOnLongClickListener { toggleMenu(); true }
        pet = image
        wm.addView(image, params)
        idleAnimation()
    }

    private fun idleAnimation() {
        val v = pet ?: return
        val a = ScaleAnimation(1f, 1.035f, 1f, 1.035f,
            Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .8f).apply {
            duration = 1100; repeatMode = Animation.REVERSE; repeatCount = Animation.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        v.startAnimation(a)
    }

    private fun playAction() {
        val v = pet ?: return
        val name = actions[actionIndex++ % actions.size]
        Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
        v.clearAnimation()
        val rotate = when (actionIndex % 5) {
            1 -> RotateAnimation(0f, -18f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .7f)
            2 -> TranslateAnimation(0f, 38f, 0f, -24f)
            3 -> RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f)
            4 -> TranslateAnimation(0f, -26f, 0f, 0f)
            else -> ScaleAnimation(1f, 1.18f, 1f, .88f, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .8f)
        }.apply { duration = 650; repeatCount = 1; repeatMode = Animation.REVERSE; interpolator = OvershootInterpolator() }
        v.startAnimation(rotate)
        v.postDelayed({ if (pet === v) idleAnimation() }, 1400)
    }

    private fun toggleMenu() {
        if (menu != null) { removeMenu(); return }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.argb(238, 35, 35, 48))
        }
        fun addAction(label: String, run: () -> Unit) {
            panel.addView(Button(this).apply {
                text = label
                setOnClickListener { run(); removeMenu() }
            })
        }
        panel.addView(TextView(this).apply {
            text = "🌸 银发小侍卫\n好感度：$affection   饱食度：$fullness"
            setTextColor(Color.WHITE); textSize = 15f; gravity = Gravity.CENTER
        })
        addAction("⚔️ 帅气动作", ::playAction)
        addAction("🍡 投喂团子") { fullness = (fullness + 20).coerceAtMost(100); affection += 2; Toast.makeText(this, "她开心地接过团子！", Toast.LENGTH_SHORT).show() }
        addAction("🌸 摸摸头") { affection += 3; Toast.makeText(this, "她眯起眼睛，轻轻蹭了蹭你的手。", Toast.LENGTH_SHORT).show(); pet?.animate()?.translationYBy(-18f)?.setDuration(180)?.withEndAction { pet?.animate()?.translationY(0f)?.setDuration(180)?.start() }?.start() }
        addAction("💬 夸夸她") { affection += 1; Toast.makeText(this, "哼……这次就接受你的夸奖。", Toast.LENGTH_SHORT).show() }
        addAction("收起菜单") { }
        menu = panel
        val p = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * .62f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 120 }
        try { wm.addView(panel, p) } catch (_: Exception) { menu = null }
    }

    private fun removeMenu() {
        menu?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        menu = null
    }

    override fun onDestroy() {
        removeMenu()
        pet?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        pet = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

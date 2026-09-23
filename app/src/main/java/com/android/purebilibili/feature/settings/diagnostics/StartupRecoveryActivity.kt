package com.android.purebilibili.feature.settings.diagnostics

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.MainActivity
import com.android.purebilibili.app.PureApplication
import com.android.purebilibili.core.util.LogCollector

/**
 * Emergency UI deliberately uses platform widgets, not the normal Compose/theme/player stack.
 * This Activity is private; no incoming Intent extras or external destinations are forwarded.
 */
class StartupRecoveryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        setTheme(
            if (dark) android.R.style.Theme_Material_NoActionBar
            else android.R.style.Theme_Material_Light_NoActionBar
        )
        super.onCreate(savedInstanceState)
        title = "启动诊断"

        val root = FrameLayout(this)
        // Handles enforced edge-to-edge too, without invoking the app's window/blur helpers.
        @Suppress("DEPRECATION")
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom,
            )
            insets
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }
        fun text(value: String, size: Float) {
            column.addView(TextView(this).apply {
                text = value
                textSize = size
                setPadding(0, 0, 0, dp(20))
                setTextIsSelectable(true)
            })
        }
        fun button(label: String, action: () -> Unit) {
            column.addView(Button(this).apply {
                text = label
                minimumHeight = dp(56)
                isAllCaps = false
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
        text("应用未能正常启动", 24f)
        text(
            "检测到连续启动未完成，已暂停首页、播放器和插件初始化。" +
                "无需开启增强日志，即可导出已保存的基础错误与崩溃快照。",
            17f,
        )
        text(
            "版本：${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                "系统：Android ${android.os.Build.VERSION.RELEASE} / API ${android.os.Build.VERSION.SDK_INT}\n" +
                "设备：${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            15f,
        )
        button("导出并分享诊断日志") {
            // Avoid loading the API 37 profiling path while diagnosing OS compatibility.
            LogCollector.exportAndShare(this, includeSystemDiagnostics = false)
        }
        button("重试正常启动") {
            (application as PureApplication).retryStartupFromRecovery()
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
        }
        button("暂时退出") { finishAndRemoveTask() }
        text("日志仅在你主动导出时分享。重试不会清除账号、设置或崩溃快照。", 14f)
        scroll.addView(column)
        val width = if (resources.configuration.screenWidthDp > 640) dp(640) else -1
        root.addView(scroll, FrameLayout.LayoutParams(width, -1, Gravity.CENTER_HORIZONTAL))
        setContentView(root)
        root.requestApplyInsets()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}

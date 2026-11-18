package com.example.eduprofile

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// [THÊM]: Hàm tiện ích ẩn thanh trạng thái hệ thống (Status Bar) theo yêu cầu giao diện ("không hiện phần này")
fun Activity.hideSystemStatusBar() {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
}

package com.soundryn.music.core.design.blur

import android.app.ActivityManager
import android.content.Context

fun isSlowDevice(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    return am?.isLowRamDevice ?: false
}

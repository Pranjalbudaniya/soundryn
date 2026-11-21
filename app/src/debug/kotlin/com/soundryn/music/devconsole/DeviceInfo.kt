package com.soundryn.music.devconsole

import android.app.Activity
import android.os.Build
import kotlin.math.roundToInt

data class DeviceInfo(
    val androidRelease: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val refreshRateHz: Float
) {
    companion object {
        fun from(activity: Activity): DeviceInfo {
            val refreshRate = activity.display?.refreshRate ?: 60.0f
            return DeviceInfo(
                androidRelease = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                refreshRateHz = refreshRate
            )
        }
    }
}

fun deviceInfoLines(info: DeviceInfo): List<String> {
    val roundedHz = info.refreshRateHz.roundToInt()
    return listOf(
        "Android ${info.androidRelease} (API ${info.apiLevel})",
        "Model: ${info.manufacturer} ${info.model}",
        "Refresh rate: $roundedHz Hz"
    )
}

package com.soundryn.music.devconsole

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceInfoLinesTest {

    @Test
    fun androidLine_showsReleaseAndApi() {
        val info = DeviceInfo(
            androidRelease = "16",
            apiLevel = 36,
            manufacturer = "Google",
            model = "Pixel 9",
            refreshRateHz = 120.0f
        )
        val lines = deviceInfoLines(info)
        assertEquals("Android 16 (API 36)", lines[0])
    }

    @Test
    fun modelLine_joinsManufacturerAndModel() {
        val info = DeviceInfo(
            androidRelease = "16",
            apiLevel = 36,
            manufacturer = "Google",
            model = "Pixel 9",
            refreshRateHz = 120.0f
        )
        val lines = deviceInfoLines(info)
        assertEquals("Model: Google Pixel 9", lines[1])
    }

    @Test
    fun refreshRateLine_roundsToWholeHz() {
        val info1 = DeviceInfo(
            androidRelease = "16",
            apiLevel = 36,
            manufacturer = "Google",
            model = "Pixel 9",
            refreshRateHz = 119.99f
        )
        val lines1 = deviceInfoLines(info1)
        assertEquals("Refresh rate: 120 Hz", lines1[2])

        val info2 = DeviceInfo(
            androidRelease = "16",
            apiLevel = 36,
            manufacturer = "Google",
            model = "Pixel 9",
            refreshRateHz = 60.0f
        )
        val lines2 = deviceInfoLines(info2)
        assertEquals("Refresh rate: 60 Hz", lines2[2])
    }

    @Test
    fun deviceInfoLines_returnsExactlyThreeLines() {
        val info = DeviceInfo(
            androidRelease = "16",
            apiLevel = 36,
            manufacturer = "Google",
            model = "Pixel 9",
            refreshRateHz = 90.0f
        )
        val lines = deviceInfoLines(info)
        assertEquals(3, lines.size)
    }
}

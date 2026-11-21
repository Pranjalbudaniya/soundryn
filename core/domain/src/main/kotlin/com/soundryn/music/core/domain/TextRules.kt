package com.soundryn.music.core.domain

import java.text.Normalizer
import java.util.Locale

private val WHITESPACE_REGEX = Regex("\\s+")
private val DIACRITICS_REGEX = Regex("\\p{M}+")

fun sortKeyOf(text: String): String {
    val collapsed = text.trim().replace(WHITESPACE_REGEX, " ")
    val lower = collapsed.lowercase(Locale.ROOT)
    val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
    val withoutDiacritics = DIACRITICS_REGEX.replace(normalized, "")
    return when {
        withoutDiacritics.startsWith("the ") && withoutDiacritics.length > 4 -> withoutDiacritics.substring(4)
        withoutDiacritics.startsWith("a ") && withoutDiacritics.length > 2 -> withoutDiacritics.substring(2)
        withoutDiacritics.startsWith("an ") && withoutDiacritics.length > 3 -> withoutDiacritics.substring(3)
        else -> withoutDiacritics
    }
}

fun formatDurationMs(ms: Long?): String? {
    if (ms == null || ms < 0L) return null
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val secondsStr = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        val minutesStr = minutes.toString().padStart(2, '0')
        "$hours:$minutesStr:$secondsStr"
    } else {
        "$minutes:$secondsStr"
    }
}

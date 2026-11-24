package com.soundryn.music.devconsole

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window

data class FrameStats(
    val frames: Int,
    val overDeadline: Int,
    val worstMs: Float,
    val p95Ms: Float,
)

fun frameStats(totalMs: List<Float>, deadlineMs: List<Float>): FrameStats {
    if (totalMs.isEmpty()) {
        return FrameStats(frames = 0, overDeadline = 0, worstMs = 0f, p95Ms = 0f)
    }
    var overDeadline = 0
    var worstMs = 0f
    for (i in totalMs.indices) {
        val total = totalMs[i]
        if (total > worstMs) {
            worstMs = total
        }
        val deadline = deadlineMs.getOrNull(i)
        if (deadline != null && deadline > 0f && total >= deadline) {
            overDeadline++
        }
    }
    val sorted = totalMs.sorted()
    val n = sorted.size
    val rank = ((95L * n + 99L) / 100L).toInt().coerceIn(1, n)
    return FrameStats(
        frames = n,
        overDeadline = overDeadline,
        worstMs = worstMs,
        p95Ms = sorted[rank - 1],
    )
}

class FrameMeter(private val window: Window) {
    private val lock = Any()
    private val totalsMs = mutableListOf<Float>()
    private val deadlinesMs = mutableListOf<Float>()
    private var lastDeadlineMs = 0f
    private var handlerThread: HandlerThread? = null
    private var started = false

    private val listener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
        if (frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) {
            return@OnFrameMetricsAvailableListener
        }
        val totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
        if (totalNs < 0L) {
            return@OnFrameMetricsAvailableListener
        }
        val deadlineNs = frameMetrics.getMetric(FrameMetrics.DEADLINE)
        val total = totalNs / 1_000_000f
        val deadline = if (deadlineNs > 0L) deadlineNs / 1_000_000f else -1f
        synchronized(lock) {
            totalsMs.add(total)
            deadlinesMs.add(deadline)
            if (deadline > 0f) {
                lastDeadlineMs = deadline
            }
        }
    }

    fun start() {
        if (started) return
        val thread = HandlerThread("FrameMeter").apply { start() }
        handlerThread = thread
        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        started = true
    }

    fun stop() {
        if (!started) return
        started = false
        window.removeOnFrameMetricsAvailableListener(listener)
        handlerThread?.quitSafely()
        handlerThread = null
    }

    fun reset() {
        synchronized(lock) {
            totalsMs.clear()
            deadlinesMs.clear()
            lastDeadlineMs = 0f
        }
    }

    fun snapshot(): Pair<FrameStats, Float> {
        return synchronized(lock) {
            frameStats(totalsMs.toList(), deadlinesMs.toList()) to lastDeadlineMs
        }
    }
}

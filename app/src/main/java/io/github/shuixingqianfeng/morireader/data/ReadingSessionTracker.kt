package io.github.shuixingqianfeng.morireader.data

import android.os.SystemClock

interface SessionClock {
    fun wallTime(): Long
    fun elapsedTime(): Long
}

object AndroidSessionClock : SessionClock {
    override fun wallTime(): Long = System.currentTimeMillis()
    override fun elapsedTime(): Long = SystemClock.elapsedRealtime()
}

class ReadingSessionTracker(private val clock: SessionClock = AndroidSessionClock) {
    private var bookId: String? = null
    private var startedWall: Long = 0
    private var startedElapsed: Long = 0

    fun start(id: String) {
        if (bookId == id) return
        bookId = id
        startedWall = clock.wallTime()
        startedElapsed = clock.elapsedTime()
    }

    fun stop(): Segment? {
        val id = bookId ?: return null
        val endedWall = clock.wallTime()
        val duration = (clock.elapsedTime() - startedElapsed).coerceAtLeast(0)
        bookId = null
        return Segment(id, startedWall, endedWall, duration)
    }

    data class Segment(val bookId: String, val startedAt: Long, val endedAt: Long, val durationMs: Long)
}

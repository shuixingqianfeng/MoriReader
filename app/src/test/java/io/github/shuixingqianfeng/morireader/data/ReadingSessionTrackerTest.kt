package io.github.shuixingqianfeng.morireader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingSessionTrackerTest {
    @Test
    fun recordsOnlyElapsedTimeAndStopsOnce() {
        val clock = FakeClock(wall = 1_000, elapsed = 50)
        val tracker = ReadingSessionTracker(clock)
        tracker.start("book")
        clock.wall = 11_000
        clock.elapsed = 5_050

        val segment = tracker.stop()

        assertEquals("book", segment?.bookId)
        assertEquals(5_000L, segment?.durationMs)
        assertNull(tracker.stop())
    }

    private class FakeClock(var wall: Long, var elapsed: Long) : SessionClock {
        override fun wallTime(): Long = wall
        override fun elapsedTime(): Long = elapsed
    }
}

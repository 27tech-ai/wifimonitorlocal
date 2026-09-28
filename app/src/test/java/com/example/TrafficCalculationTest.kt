package com.example

import com.example.traffic.TrafficCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrafficCalculationTest {

    @Test
    fun testSpeedCalculationStandardDelta() {
        val previousRx = 100_000_000L
        val currentRx = 102_400_000L // 2,400,000 bytes delta
        val elapsed = 1000L // 1 second

        val speed = TrafficCalculator.calculateSpeed(currentRx, previousRx, elapsed)
        assertEquals(2_400_000L, speed)
    }

    @Test
    fun testSpeedCalculationOverTwoSeconds() {
        val previousRx = 10_000_000L
        val currentRx = 20_000_000L // 10,000,000 bytes delta
        val elapsed = 2000L // 2 seconds

        val speed = TrafficCalculator.calculateSpeed(currentRx, previousRx, elapsed)
        assertEquals(5_000_000L, speed)
    }

    @Test
    fun testSpeedCalculationInitialBaselineNull() {
        val currentRx = 100_000_000L
        val speed = TrafficCalculator.calculateSpeed(currentRx, null, 1000L)
        assertEquals(0L, speed)
    }

    @Test
    fun testSpeedCalculationCounterResetGuardsAgainstNegative() {
        // Device rebooted or 32-bit counter wrapped
        val previousRx = 4_000_000_000L
        val currentRx = 500_000L // smaller than previous!

        val speed = TrafficCalculator.calculateSpeed(currentRx, previousRx, 1000L)
        assertEquals("Speed must be 0 and non-negative on counter wrap", 0L, speed)
    }

    @Test
    fun testSpeedCalculationZeroOrNegativeElapsed() {
        val speedZero = TrafficCalculator.calculateSpeed(200L, 100L, 0L)
        assertEquals(0L, speedZero)

        val speedNeg = TrafficCalculator.calculateSpeed(200L, 100L, -500L)
        assertEquals(0L, speedNeg)
    }

    @Test
    fun testFormatSpeed() {
        assertEquals("0 B/s", TrafficCalculator.formatSpeed(0L))
        assertEquals("500 B/s", TrafficCalculator.formatSpeed(500L))
        assertEquals("120.0 KB/s", TrafficCalculator.formatSpeed(120 * 1024L))
        assertEquals("2.4 MB/s", TrafficCalculator.formatSpeed((2.4 * 1024 * 1024).toLong()))
        assertEquals("1.2 GB/s", TrafficCalculator.formatSpeed((1.2 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", TrafficCalculator.formatBytes(0L))
        assertEquals("512 B", TrafficCalculator.formatBytes(512L))
        assertEquals("1.0 KB", TrafficCalculator.formatBytes(1024L))
        assertEquals("1.0 MB", TrafficCalculator.formatBytes(1024L * 1024L))
        assertEquals("2.50 GB", TrafficCalculator.formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", TrafficCalculator.formatDuration(0L))
        assertEquals("00:45", TrafficCalculator.formatDuration(45_000L))
        assertEquals("02:15", TrafficCalculator.formatDuration(135_000L))
        assertEquals("01:05:20", TrafficCalculator.formatDuration((3600 + 300 + 20) * 1000L))
    }
}

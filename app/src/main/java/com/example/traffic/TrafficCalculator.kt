package com.example.traffic

import java.util.Locale

object TrafficCalculator {

    /**
     * Calculates speed in bytes per second.
     * Prevents negative values in case of counter wrap-around or device reboot.
     * If previousBytes is null or 0 (initial baseline), returns 0.
     */
    fun calculateSpeed(
        currentBytes: Long,
        previousBytes: Long?,
        elapsedMillis: Long
    ): Long {
        if (previousBytes == null || elapsedMillis <= 0) {
            return 0L
        }

        val deltaBytes = currentBytes - previousBytes

        // Counter reset / wrap-around guard
        if (deltaBytes < 0) {
            return 0L
        }

        val seconds = elapsedMillis.toDouble() / 1000.0
        if (seconds <= 0.0) return 0L

        val speed = (deltaBytes.toDouble() / seconds).toLong()
        return if (speed < 0L) 0L else speed
    }

    /**
     * Formats bytes per second into human-readable representation:
     * e.g. "0 B/s", "120 KB/s", "2.4 MB/s", "1.1 GB/s"
     */
    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "0 B/s"
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB/s", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB/s", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB/s", kb)
            else -> "$bytesPerSec B/s"
        }
    }

    /**
     * Formats cumulative byte count into human-readable string:
     * e.g. "512 B", "45.2 KB", "128.5 MB", "12.4 GB"
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Formats milliseconds duration into mm:ss or hh:mm:ss
     */
    fun formatDuration(durationMillis: Long): String {
        if (durationMillis <= 0) return "00:00"
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}

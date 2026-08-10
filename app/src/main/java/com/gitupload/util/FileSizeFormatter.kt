package com.gitupload.util

import java.util.Locale

/**
 * Formats a byte count as a human-readable file size string (e.g.
 * `"512 B"`, `"1.5 KB"`, `"2.00 MB"`).
 *
 * Uses [Locale.US] so the decimal separator is always a period regardless
 * of the device's locale (some locales use a comma, which breaks the
 * string if it's later parsed or compared).
 *
 * Extracted as a single shared helper to replace four copy-pasted
 * implementations that existed in [StagedFile], [UploadScreen] and
 * [HistoryScreen].
 */
fun Long.formatFileSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", this / 1024.0)
        else -> String.format(Locale.US, "%.2f MB", this / (1024.0 * 1024.0))
    }
}

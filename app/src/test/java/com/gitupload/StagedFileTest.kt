package com.gitupload

import com.gitupload.data.models.StagedFile
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Unit tests for [StagedFile.formattedSize].
 *
 * Runs under Robolectric because [StagedFile] references [android.net.Uri].
 * The locale is forced to US so the decimal separator in KB/MB formatting
 * is deterministic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StagedFileTest {

    private fun stagedFile(sizeBytes: Long): StagedFile {
        return StagedFile(
            id = "test-id",
            uri = null,
            fileName = "test.txt",
            relativePath = "test.txt",
            sizeBytes = sizeBytes,
            isText = true,
            mimeType = "text/plain",
            contentBytes = ByteArray(0),
            textPreview = null,
            selected = true
        )
    }

    @Test
    fun `formattedSize shows bytes below 1 KB`() {
        Locale.setDefault(Locale.US)
        assertEquals("0 B", stagedFile(0).formattedSize)
        assertEquals("1 B", stagedFile(1).formattedSize)
        assertEquals("512 B", stagedFile(512).formattedSize)
        assertEquals("1023 B", stagedFile(1023).formattedSize)
    }

    @Test
    fun `formattedSize shows KB at and above 1024 bytes`() {
        Locale.setDefault(Locale.US)
        assertEquals("1.0 KB", stagedFile(1024).formattedSize)
        assertEquals("1.5 KB", stagedFile(1536).formattedSize)
        assertEquals("10.2 KB", stagedFile(10240 + 204).formattedSize)
    }

    @Test
    fun `formattedSize shows MB at and above 1 MiB`() {
        Locale.setDefault(Locale.US)
        val oneMiB = 1024L * 1024
        assertEquals("1.00 MB", stagedFile(oneMiB).formattedSize)
        assertEquals("1.50 MB", stagedFile(oneMiB + oneMiB / 2).formattedSize)
        assertEquals("2.00 MB", stagedFile(oneMiB * 2).formattedSize)
    }

    @Test
    fun `formattedSize boundary between KB and MB`() {
        Locale.setDefault(Locale.US)
        val justBelow1MiB = 1024L * 1024 - 1
        assertEquals("1024.0 KB", stagedFile(justBelow1MiB).formattedSize)
        val exactly1MiB = 1024L * 1024
        assertEquals("1.00 MB", stagedFile(exactly1MiB).formattedSize)
    }

    @Test
    fun `StagedFile equals and hashCode use id and relativePath`() {
        val file1 = stagedFile(100)
        val file2 = file1.copy(sizeBytes = 999, contentBytes = ByteArray(99))
        // Same id + relativePath → equal despite different content/size
        assertEquals(file1, file2)
        assertEquals(file1.hashCode(), file2.hashCode())

        val file3 = file1.copy(id = "different-id")
        // Different id → not equal
        assert(!file1.equals(file3))
    }
}

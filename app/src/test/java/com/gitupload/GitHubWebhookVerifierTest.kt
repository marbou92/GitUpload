package com.gitupload

import com.gitupload.util.GitHubWebhookVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Unit tests for [GitHubWebhookVerifier].
 *
 * These tests run on the JVM (no Android dependencies) because the verifier
 * only uses [javax.crypto.Mac] and [java.security.MessageDigest], which are
 * part of the standard JDK.
 */
class GitHubWebhookVerifierTest {

    private val verifier = GitHubWebhookVerifier

    private val secret = "my_webhook_secret"
    private val payload = """{"action":"opened","number":42}"""

    /** Computes the expected HMAC-SHA256 hex digest using the JDK directly. */
    private fun expectedHmac(data: String, key: String): String {
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        val hashBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `computeHmacSha256 returns correct lowercase hex digest`() {
        val expected = expectedHmac(payload, secret)
        val actual = verifier.computeHmacSha256(payload, secret)
        assertNotNull("computeHmacSha256 should not return null", actual)
        assertEquals(expected, actual)
        // Verify it's lowercase hex
        assertTrue("Digest should be lowercase hex", actual!!.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun `computeHmacSha256 produces different digests for different payloads`() {
        val digest1 = verifier.computeHmacSha256("payload_one", secret)
        val digest2 = verifier.computeHmacSha256("payload_two", secret)
        assertNotNull(digest1)
        assertNotNull(digest2)
        assertFalse("Different payloads should produce different digests", digest1 == digest2)
    }

    @Test
    fun `computeHmacSha256 produces different digests for different secrets`() {
        val digest1 = verifier.computeHmacSha256(payload, "secret_one")
        val digest2 = verifier.computeHmacSha256(payload, "secret_two")
        assertNotNull(digest1)
        assertNotNull(digest2)
        assertFalse("Different secrets should produce different digests", digest1 == digest2)
    }

    @Test
    fun `verifySignature accepts a valid signature header`() {
        val hash = verifier.computeHmacSha256(payload, secret)!!
        val header = "sha256=$hash"
        assertTrue("Valid signature should be accepted", verifier.verifySignature(payload, secret, header))
    }

    @Test
    fun `verifySignature rejects a tampered signature`() {
        val hash = verifier.computeHmacSha256(payload, secret)!!
        // Flip one character in the hash to tamper it
        val tamperedHash = if (hash[0] == 'a') hash.replaceFirst('a', 'b') else hash.replaceFirst(hash[0], 'a')
        val header = "sha256=$tamperedHash"
        assertFalse("Tampered signature should be rejected", verifier.verifySignature(payload, secret, header))
    }

    @Test
    fun `verifySignature rejects a signature computed with the wrong secret`() {
        val wrongHash = verifier.computeHmacSha256(payload, "wrong_secret")!!
        val header = "sha256=$wrongHash"
        assertFalse("Wrong-secret signature should be rejected", verifier.verifySignature(payload, secret, header))
    }

    @Test
    fun `verifySignature rejects a signature for a different payload`() {
        val hash = verifier.computeHmacSha256("different_payload", secret)!!
        val header = "sha256=$hash"
        assertFalse("Different-payload signature should be rejected", verifier.verifySignature(payload, secret, header))
    }

    @Test
    fun `verifySignature returns false for null header`() {
        assertFalse(verifier.verifySignature(payload, secret, null))
    }

    @Test
    fun `verifySignature returns false for blank header`() {
        assertFalse(verifier.verifySignature(payload, secret, ""))
        assertFalse(verifier.verifySignature(payload, secret, "   "))
    }

    @Test
    fun `verifySignature returns false for header without sha256 prefix`() {
        val hash = verifier.computeHmacSha256(payload, secret)!!
        val header = "sha1=$hash"
        assertFalse("Header without sha256= prefix should be rejected", verifier.verifySignature(payload, secret, header))
    }

    @Test
    fun `verifySignature returns false for blank secret`() {
        val hash = verifier.computeHmacSha256(payload, secret)!!
        val header = "sha256=$hash"
        assertFalse("Blank secret should cause rejection", verifier.verifySignature(payload, "", header))
    }

    @Test
    fun `verifySignature returns false for empty payload`() {
        val hash = verifier.computeHmacSha256(payload, secret)!!
        val header = "sha256=$hash"
        assertFalse("Empty payload should cause rejection", verifier.verifySignature("", secret, header))
    }

    @Test
    fun `verifySignature is case-insensitive on the sha256 prefix`() {
        val hash = verifier.computeHmacSha256(payload, secret)!!
        val header = "SHA256=$hash"
        assertTrue("Uppercase SHA256= prefix should be accepted", verifier.verifySignature(payload, secret, header))
    }
}

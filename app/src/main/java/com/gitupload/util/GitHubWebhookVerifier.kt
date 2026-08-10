package com.gitupload.util

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class to handle GitHub webhook verification and HMAC-SHA256 signature validation.
 * Ensures that repository updates pushed from other devices or webhooks are securely authenticated.
 */
object GitHubWebhookVerifier {

    /**
     * Verifies the GitHub HMAC SHA-256 signature from the 'X-Hub-Signature-256' HTTP header.
     *
     * @param payloadBody The raw request payload body received from GitHub webhook.
     * @param secret The webhook secret key configured on GitHub repository settings.
     * @param signatureHeader The header value from 'X-Hub-Signature-256' (e.g., "sha256=abcdef...").
     * @return True if signature is valid and authentic, false otherwise.
     */
    fun verifySignature(payloadBody: String, secret: String, signatureHeader: String?): Boolean {
        if (signatureHeader.isNullOrBlank() || secret.isBlank() || payloadBody.isEmpty()) return false
        val prefix = "sha256="
        if (!signatureHeader.startsWith(prefix, ignoreCase = true)) return false

        val expectedHash = signatureHeader.substring(prefix.length).trim()
        val calculatedHash = computeHmacSha256(payloadBody, secret) ?: return false

        return constantTimeAreEqual(expectedHash.lowercase(), calculatedHash.lowercase())
    }

    /**
     * Computes HMAC SHA-256 digest hex string for given data and secret.
     */
    fun computeHmacSha256(data: String, secret: String): String? {
        return try {
            val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            val hashBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Constant time comparison to prevent timing attacks on hash verification.
     */
    private fun constantTimeAreEqual(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }
}

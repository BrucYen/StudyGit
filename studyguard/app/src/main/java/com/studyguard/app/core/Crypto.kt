package com.studyguard.app.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Crypto {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(value: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(value.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashed = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hashed)
    }

    fun verify(value: String, saltBase64: String, expectedHash: String): Boolean {
        val actual = hash(value, saltBase64)
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }
}

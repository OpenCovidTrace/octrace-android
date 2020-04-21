package org.opencovidtrace.octrace

import java.security.SecureRandom

object SecurityUtil {

    private val random = SecureRandom()

    fun generateKey(): ByteArray {
        val bytes = ByteArray(32)

        random.nextBytes(bytes)

        return bytes
    }

}
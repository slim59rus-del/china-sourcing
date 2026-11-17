package com.leantech.finder.data.net

import java.security.MessageDigest

/** Подпись запросов mtop: sign = md5(token & t & appKey & data). */
object Sign {

    fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        val builder = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val value = byte.toInt() and 0xFF
            if (value < 0x10) builder.append('0')
            builder.append(Integer.toHexString(value))
        }
        return builder.toString()
    }

    fun mtop(token: String, timestamp: String, appKey: String, data: String): String =
        md5("$token&$timestamp&$appKey&$data")
}

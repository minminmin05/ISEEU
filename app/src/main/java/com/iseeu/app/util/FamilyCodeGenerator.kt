package com.iseeu.app.util

import kotlin.random.Random

/** 6-character codes from the Crockford Base32 alphabet — no I/L/O/U, so nothing looks ambiguous read aloud or typed on a phone. */
object FamilyCodeGenerator {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val LENGTH = 6

    fun next(): String = buildString {
        repeat(LENGTH) { append(ALPHABET[Random.nextInt(ALPHABET.length)]) }
    }

    /** "ABC123" -> "ABC-123" for display/sharing. */
    fun format(code: String): String {
        val clean = normalize(code)
        return if (clean.length == LENGTH) "${clean.take(3)}-${clean.takeLast(3)}" else clean
    }

    /** Strips whitespace/hyphens and uppercases — call before any lookup so "abc-123" and "ABC123" resolve the same. */
    fun normalize(input: String): String = input.uppercase().filter { it.isLetterOrDigit() }
}

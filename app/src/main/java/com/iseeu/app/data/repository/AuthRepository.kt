package com.iseeu.app.data.repository

interface AuthRepository {
    /** Returns the current anonymous uid, signing in first if this is a fresh install. */
    suspend fun ensureSignedIn(): String

    fun currentUid(): String?
}

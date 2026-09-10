package com.iseeu.app.location

import android.location.Location
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gates which delivered location updates are actually worth writing to Firestore.
 * Two triggers: moved far enough, or enough time passed that a stale "last updated" would read as alarming.
 */
@Singleton
class AdaptiveLocationStrategy @Inject constructor() {

    private var lastWrittenLocation: Location? = null
    private var lastWrittenAtMillis: Long = 0L

    fun shouldWrite(newLocation: Location, nowMillis: Long): Boolean {
        val last = lastWrittenLocation ?: return true
        if (newLocation.distanceTo(last) >= MIN_DISPLACEMENT_METERS) return true
        return (nowMillis - lastWrittenAtMillis) >= HEARTBEAT_INTERVAL_MS
    }

    fun markWritten(location: Location, nowMillis: Long) {
        lastWrittenLocation = location
        lastWrittenAtMillis = nowMillis
    }

    companion object {
        private const val MIN_DISPLACEMENT_METERS = 50f
        private const val HEARTBEAT_INTERVAL_MS = 15 * 60 * 1000L
    }
}

package com.iseeu.app.util

import android.location.Location
import com.iseeu.app.domain.model.HistoryPoint
import com.iseeu.app.domain.model.Trip

/**
 * Groups a member's raw history points (each 50m+ moved or 15min+ elapsed, per the adaptive
 * write-gate) into discrete "trips" — runs of consecutive points that are meaningfully far apart,
 * separated by gaps where the points cluster in one spot. There's no ground-truth "trip started/
 * ended" signal to key off, so this is a geometric heuristic, not a GPS-track classifier.
 */
object TripSegmenter {
    private const val MOVEMENT_THRESHOLD_METERS = 100f
    private const val MIN_TRIP_DISTANCE_METERS = 200.0
    private const val DRIVING_SPEED_KMH_THRESHOLD = 15.0

    fun segmentTrips(points: List<HistoryPoint>): List<Trip> {
        val sorted = points.sortedBy { it.timestampMillis }
        if (sorted.size < 2) return emptyList()

        val trips = mutableListOf<Trip>()
        var currentSegment = mutableListOf(sorted[0])

        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            if (distanceBetween(prev, curr) >= MOVEMENT_THRESHOLD_METERS) {
                currentSegment.add(curr)
            } else {
                buildTripOrNull(currentSegment)?.let(trips::add)
                currentSegment = mutableListOf(curr)
            }
        }
        buildTripOrNull(currentSegment)?.let(trips::add)

        return trips.sortedByDescending { it.endTimeMillis }
    }

    private fun buildTripOrNull(segment: List<HistoryPoint>): Trip? {
        if (segment.size < 2) return null

        var totalDistance = 0.0
        for (i in 1 until segment.size) {
            totalDistance += distanceBetween(segment[i - 1], segment[i])
        }
        if (totalDistance < MIN_TRIP_DISTANCE_METERS) return null

        val startTime = segment.first().timestampMillis
        val endTime = segment.last().timestampMillis
        val durationHours = (endTime - startTime) / 3_600_000.0
        val avgSpeedKmh = if (durationHours > 0) (totalDistance / 1000.0) / durationHours else 0.0

        return Trip(
            points = segment,
            distanceMeters = totalDistance,
            startTimeMillis = startTime,
            endTimeMillis = endTime,
            isDriving = avgSpeedKmh >= DRIVING_SPEED_KMH_THRESHOLD,
        )
    }

    private fun distanceBetween(a: HistoryPoint, b: HistoryPoint): Float {
        val results = FloatArray(1)
        Location.distanceBetween(a.lat, a.lng, b.lat, b.lng, results)
        return results[0]
    }
}

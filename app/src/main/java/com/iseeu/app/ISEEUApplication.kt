package com.iseeu.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class ISEEUApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // OSM tile-usage policy requires a real user agent — the default (blank) gets tiles blocked.
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        createLocationNotificationChannel()
        createPlaceAlertsNotificationChannel()
    }

    private fun createLocationNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            LOCATION_CHANNEL_ID,
            "Location sharing",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows while ISEEU is sharing your location with your family"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createPlaceAlertsNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            PLACE_ALERTS_CHANNEL_ID,
            "Place arrivals and departures",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when a family member arrives at or leaves a saved place"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val LOCATION_CHANNEL_ID = "location_sharing"
        const val PLACE_ALERTS_CHANNEL_ID = "place_alerts"
    }
}

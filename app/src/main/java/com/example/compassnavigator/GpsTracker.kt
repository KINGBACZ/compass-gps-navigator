package com.example.compassnavigator

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat

class GpsTracker(
    private val context: Context,
    private val onLocation: (Location) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var started = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onStatus("GPS работает, точность ${location.accuracy.toInt()} м")
            onLocation(location)
        }

        override fun onProviderEnabled(provider: String) {
            onStatus("Геолокация включена, ищем сигнал...")
        }

        override fun onProviderDisabled(provider: String) {
            onStatus("Геолокация выключена в настройках телефона")
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return

        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            onStatus("Нет разрешения на геолокацию. Включите его в настройках приложения")
            return
        }

        val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val netOn = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsOn && !netOn) {
            onStatus("Включите геолокацию в настройках телефона (шторка → Местоположение)")
            return
        }

        started = true

        try {
            // Сразу показываем последнюю известную позицию, если она есть
            val last = when {
                fineGranted && gpsOn -> lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                else -> lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (last != null) onLocation(last)

            if (fineGranted && gpsOn) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
            }
            if (netOn) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, listener)
            }
            onStatus("Ищем сигнал GPS... выйдите на улицу, это может занять 1–3 минуты")
        } catch (e: SecurityException) {
            started = false
            onStatus("Нет доступа к геолокации: ${e.message}")
        }
    }

    fun stop() {
        if (!started) return
        lm.removeUpdates(listener)
        started = false
    }
}
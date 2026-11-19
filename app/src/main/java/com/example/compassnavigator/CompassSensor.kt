package com.example.compassnavigator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Компас на основе датчика ориентации TYPE_ROTATION_VECTOR.
 * Работает без интернета и без сторонних библиотек.
 */
class CompassSensor(
    context: Context,
    private val onHeadingChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var smoothedHeading = Float.NaN

    val isAvailable: Boolean
        get() = rotationSensor != null

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val rawHeading = (azimuthDeg + 360f) % 360f

        smoothedHeading = if (smoothedHeading.isNaN()) {
            rawHeading
        } else {
            smoothAngle(smoothedHeading, rawHeading)
        }

        onHeadingChanged(smoothedHeading)
    }

    /** Сглаживание угла с учётом перехода через 0/360 градусов */
    private fun smoothAngle(current: Float, target: Float): Float {
        var diff = target - current
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return (current + diff * 0.2f + 360f) % 360f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
package com.example.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SensorOrientation(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val yaw: Float = 0f
)

class SensorTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _orientation = MutableStateFlow(SensorOrientation())
    val orientation: StateFlow<SensorOrientation> = _orientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var isRunning = false

    fun start() {
        if (isRunning || sensorManager == null) return
        try {
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
                isRunning = true
            } else if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI)
                isRunning = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        if (!isRunning || sensorManager == null) return
        try {
            sensorManager.unregisterListener(this)
            isRunning = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        try {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                _orientation.value = SensorOrientation(
                    yaw = orientationAngles[0],
                    pitch = orientationAngles[1],
                    roll = orientationAngles[2]
                )
            } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val ax = event.values[0] / SensorManager.GRAVITY_EARTH
                val ay = event.values[1] / SensorManager.GRAVITY_EARTH
                val az = event.values[2] / SensorManager.GRAVITY_EARTH
                _orientation.value = SensorOrientation(
                    pitch = -ay * 0.8f,
                    roll = ax * 0.8f,
                    yaw = 0f
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

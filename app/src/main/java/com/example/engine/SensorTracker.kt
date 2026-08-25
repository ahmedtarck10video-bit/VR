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
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _orientation = MutableStateFlow(SensorOrientation())
    val orientation: StateFlow<SensorOrientation> = _orientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val gravityValues = FloatArray(3)
    private val geoMagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false

    private var baselinePitch = 0f
    private var baselineRoll = 0f
    private var baselineYaw = 0f

    private var isRunning = false

    fun start() {
        if (isRunning || sensorManager == null) return
        try {
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
                isRunning = true
            } else {
                if (accelerometerSensor != null) {
                    sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
                    isRunning = true
                }
                if (magneticSensor != null) {
                    sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_GAME)
                }
                if (gyroSensor != null) {
                    sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
                }
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

    fun calibrate() {
        val current = _orientation.value
        baselinePitch = current.pitch + baselinePitch
        baselineRoll = current.roll + baselineRoll
        baselineYaw = current.yaw + baselineYaw
    }

    private var filteredPitch = 0f
    private var filteredRoll = 0f
    private var filteredYaw = 0f
    private val filterAlpha = 0.22f // Exponential Smoothing Factor for stable AR Anchoring

    private fun updateSmoothedOrientation(rawPitch: Float, rawRoll: Float, rawYaw: Float) {
        filteredPitch = filteredPitch + filterAlpha * (rawPitch - filteredPitch)
        filteredRoll = filteredRoll + filterAlpha * (rawRoll - filteredRoll)
        filteredYaw = filteredYaw + filterAlpha * (rawYaw - filteredYaw)

        _orientation.value = SensorOrientation(
            pitch = filteredPitch,
            roll = filteredRoll,
            yaw = filteredYaw
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        try {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    updateSmoothedOrientation(
                        rawPitch = orientationAngles[1] - baselinePitch,
                        rawRoll = orientationAngles[2] - baselineRoll,
                        rawYaw = orientationAngles[0] - baselineYaw
                    )
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravityValues, 0, 3)
                    hasGravity = true
                    if (hasGravity && hasMagnetic) {
                        computeOrientationFromGravityAndMagnetic()
                    } else {
                        val ax = event.values[0] / SensorManager.GRAVITY_EARTH
                        val ay = event.values[1] / SensorManager.GRAVITY_EARTH
                        updateSmoothedOrientation(
                            rawPitch = (-ay * 1.2f) - baselinePitch,
                            rawRoll = (ax * 1.2f) - baselineRoll,
                            rawYaw = -baselineYaw
                        )
                    }
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, geoMagneticValues, 0, 3)
                    hasMagnetic = true
                    if (hasGravity) {
                        computeOrientationFromGravityAndMagnetic()
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if (rotationVectorSensor == null && !hasMagnetic) {
                        val cur = _orientation.value
                        updateSmoothedOrientation(
                            rawPitch = cur.pitch + event.values[0] * 0.05f,
                            rawRoll = cur.roll + event.values[1] * 0.05f,
                            rawYaw = cur.yaw + event.values[2] * 0.05f
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun computeOrientationFromGravityAndMagnetic() {
        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, gravityValues, geoMagneticValues)) {
            val angles = FloatArray(3)
            SensorManager.getOrientation(r, angles)
            updateSmoothedOrientation(
                rawPitch = angles[1] - baselinePitch,
                rawRoll = angles[2] - baselineRoll,
                rawYaw = angles[0] - baselineYaw
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}


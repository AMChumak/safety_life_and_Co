package com.example.safetylife

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlin.math.round

class LocatyService: Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    companion object {
        val KEY_ANGLE = "angle"
        val KEY_ON_SENSOR_CHANGED_ACTION = "com.example.safetylife.ON_SENSOR_CHANGED"
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
        updateOrientationAngles()
    }

    private fun updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

        val orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val degrees = (Math.toDegrees(orientation.get(0).toDouble()) + 360.0) % 360.0
        val angle = round(degrees * 100) / 100

        val intent = Intent()
        intent.putExtra(KEY_ANGLE, angle)
        intent.action = KEY_ON_SENSOR_CHANGED_ACTION

        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //do nothing
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

}
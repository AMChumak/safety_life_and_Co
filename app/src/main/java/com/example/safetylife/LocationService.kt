package com.example.safetylife

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.PI
import kotlin.math.cos

class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        Log.d("HEY", "Service Started")
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Safety Life работает")
            .setContentText("Будьте внимательны на дороге")
            .setSmallIcon(R.drawable.logo)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(1000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                //val lat = location.latitude.toString().takeLast(3)
                //val long = location.longitude.toString().takeLast(3)

                val lat = location.latitude.toDouble()
                val long = location.longitude.toDouble()
                val intent = Intent()
                intent.putExtra(KEY_LATITUDE, lat)
                intent.putExtra(KEY_LONGITUDE, long)
                intent.action = KEY_ON_LOCATION_CHANGED_ACTION

                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
            .launchIn(serviceScope)

        startForeground(2, notification.build())
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        val KEY_LATITUDE = "LATITUDE"
        val KEY_LONGITUDE = "LONGITUDE"
        val KEY_ON_LOCATION_CHANGED_ACTION = "LOCATION_CHANGED"
    }
}
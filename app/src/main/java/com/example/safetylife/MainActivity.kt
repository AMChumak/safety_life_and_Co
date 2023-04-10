package com.example.safetylife

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.navigationandmap.ui.NavigatorViewModel
import com.example.navigationandmap.ui.models.PointsOnRoard
import com.example.navigationandmap.ui.models.QuadrantsOnMap
import com.example.safetylife.data.Constants
import com.example.safetylife.data.nightSetting
import com.example.safetylife.data.pushSetting
import com.example.safetylife.data.soundSetting
import com.google.android.gms.location.*
import com.google.gson.Gson
import org.json.JSONException
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    var pref : SharedPreferences? = null
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val TAG = "MainActivity"
    var isActive = false

    private lateinit var client: ActivityRecognitionClient

    private val navigatorViewModel: NavigatorViewModel by viewModels()
    private val locationRequest: LocationRequest = createLocationRequest()
    private lateinit var locationCallback: LocationCallback
    private var angle: Double = 0.2

    fun Double.format(digits: Int) = "%.${digits}f".format(this)

    lateinit var notificationManager: NotificationManager
    lateinit var audioManager: AudioManager
    var previousVolume: Int = 0

    @SuppressLint("ResourceAsColor", "MissingInflatedId", "UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {

        }

        setContentView(R.layout.activity_main)
        val prefs1 = getSharedPreferences("push", MODE_PRIVATE)
        val prefs2 = getSharedPreferences("night", MODE_PRIVATE)
        val prefs3 = getSharedPreferences("sound", MODE_PRIVATE)
        pushSetting = prefs1.getBoolean("switchState", true)
        nightSetting = prefs2.getBoolean("switchState", true)
        soundSetting = prefs3.getBoolean("switchState", true)


        getSupportActionBar()?.setTitle("Safety Life")
        // sd
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.getDefault())
        getLoaction()
        
        createChannel("userRiskInteraction", "Опасные ситуации")
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        audioManager =
            getSystemService(AUDIO_SERVICE) as AudioManager
        previousVolume = audioManager.mediaCurrentVolume

        client = ActivityRecognition.getClient(this)

        val a = findViewById<Button>(R.id.button2)
        val buttobState = findViewById<Button>(R.id.button2)
        val backgroundImage = findViewById<ImageView>(R.id.imageView2)

        fun active(){
            if(isActive){
                // Stop foreground service
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
                    startService(this)
                }

                isActive = !isActive
                buttobState.setBackgroundResource(R.drawable.circle_onoff_button)
                backgroundImage.setImageResource(R.drawable.turnon)
            } else{
                // Start foreground service
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
                    startService(this)
                }

                buttobState.setBackgroundResource(R.drawable.circle_onoff_button_active)
                backgroundImage.setImageResource(R.drawable.turnon_active)
                isActive = !isActive
            }
        }
        a.setOnClickListener {
            active()
        }

        //для компаса
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,  IntentFilter(LocatyService.KEY_ON_SENSOR_CHANGED_ACTION))

        //для геолокации
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastLocationReceiver,  IntentFilter(LocationService.KEY_ON_LOCATION_CHANGED_ACTION))

        // подключение измерения скорости
        LocalBroadcastManager.getInstance(this).registerReceiver(activityTransitionReceiver,  IntentFilter(ActivityTransitionReceiver.KEY_ON_ACTIVITY_TYPE_CHANGED_ACTION))

        // поделючение JSON
        try{
            val jsonPointsString  = getJSONFromAssets("log_300.json")
            val points = Gson().fromJson(jsonPointsString, PointsOnRoard::class.java)
            val jsonQuadrantsString  = getJSONFromAssets("chunk_log_302.json")
            val quadrants = Gson().fromJson(jsonQuadrantsString, QuadrantsOnMap::class.java)
            navigatorViewModel.loadMapPointsAndQuadrants(points.points, quadrants.quadrants)
        } catch (e: JSONException){
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            // check for permission
            && !ActivityTransitionsUtil.hasActivityTransitionPermissions(this)
        ) {
            // request for permission
            requestActivityTransitionPermission()
        } else {
            // when permission is already allowed
            requestForUpdates()
        }
     }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        requestForUpdates()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestActivityTransitionPermission()
        }
    }

    private fun requestForUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        client
            .requestActivityTransitionUpdates(
                ActivityTransitionsUtil.getActivityTransitionRequest(),
                getPendingIntent()
            )
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            Constants.REQUEST_CODE_INTENT_ACTIVITY_TRANSITION,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun requestActivityTransitionPermission() {
        EasyPermissions.requestPermissions(
            this,
            "You need to allow Activity Transition Permissions in order to recognize your activities",
            Constants.REQUEST_CODE_ACTIVITY_TRANSITION,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    }



    var isSoundFaded = false;
    var soundFadedTime = 0L;
    val broadcastLocationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val latitude = intent.getDoubleExtra(LocationService.KEY_LATITUDE,0.0)
            val longitude = intent.getDoubleExtra(LocationService.KEY_LONGITUDE,0.0)
            val notification = NotificationCompat.Builder(this@MainActivity, "userRiskInteraction")
                .setContentText("Отвлекитесь от смартфона")
                .setContentTitle("Переходите дорогу?")
                .setSmallIcon(R.drawable.logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setOngoing(true)
            val currentTime = Calendar.getInstance().time
            val hour = currentTime.hours
            navigatorViewModel.updateCoordinates(latitude, longitude, angle)
            // Ответная реакция
            if(navigatorViewModel.uiState.inDangerous){
                if((nightSetting) or !( hour in 0..6 )){
                    if(pushSetting) notificationManager.notify(1, notification.build())
                    if(soundSetting) {
                        if(audioManager.mediaCurrentVolume >= 5 && ((System.currentTimeMillis() - soundFadedTime) > 20000)) {
                            previousVolume = audioManager.mediaCurrentVolume
                            audioManager.setMediaVolume(5)
                            isSoundFaded = true;
                            soundFadedTime = System.currentTimeMillis();
                        }
                    }
                }

            } else if (isSoundFaded && ((System.currentTimeMillis() - soundFadedTime) > 20000)) {
                if(nightSetting){
                    if(pushSetting) audioManager.setMediaVolume(previousVolume)
                    if(soundSetting) notificationManager.cancel(1)
                    isSoundFaded = false;
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startForegroundServiceForSensors()
    }

    private fun startForegroundServiceForSensors() {
        val locatyIntent = Intent(this, LocatyService::class.java)
        startService(locatyIntent)
    }

    fun openAct(v : View) {
        val intent = Intent(this, settings::class.java)
        startActivity(intent)
    }


    private fun getLoaction(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACTIVITY_RECOGNITION), 10101)
        }

        val lastLocation = fusedLocationProviderClient.lastLocation

        lastLocation.addOnSuccessListener {
            val address = geocoder.getFromLocation(it.latitude, it.longitude, 1)
            val text = findViewById<TextView>(R.id.sityText)
            text.text = address?.get(0)?.locality ?: "0"
        }
    }



    fun createChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name,
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }

    fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        return locationRequest
    }

    // функция считывания JSON
    private fun getJSONFromAssets(fileName: String): String? {
        var json: String? = null
        val charset: Charset = Charsets.UTF_8
        try{
            val myJsonFile = assets.open(fileName)
            val size = myJsonFile.available()
            val buffer = ByteArray(size)
            myJsonFile.read(buffer)
            myJsonFile.close()
            json = String(buffer, charset)
            Log.d(TAG,"size $size, fileName $fileName")
        }catch(ex: IOException){
            ex.printStackTrace()
            return null
        }
        return json
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            angle = intent.getDoubleExtra(LocatyService.KEY_ANGLE,angle)
        }
    }



    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    fun AudioManager.setMediaVolume(volumeIndex:Int) {
        // Set media volume level
        this.setStreamVolume(
            AudioManager.STREAM_MUSIC, // Stream type
            volumeIndex, // Volume index
            AudioManager.FLAG_SHOW_UI
        )
    }

    override fun onPause() {
        super.onPause()

        // пишем нужное в SharedPreferences

        val ed1 = getSharedPreferences("push", MODE_PRIVATE).edit()
        val ed2 = getSharedPreferences("night", MODE_PRIVATE).edit()
        val ed3 = getSharedPreferences("sound", MODE_PRIVATE).edit()


        ed1.putBoolean("switchState", pushSetting)
        ed1.commit()
        ed2.putBoolean("switchState", nightSetting)
        ed2.commit()
        ed3.putBoolean("switchState", soundSetting)
        ed3.commit()
        var prefs: MutableList<SharedPreferences.Editor> = mutableListOf(ed1, ed2, ed3)
    }

    // Extension property to get media maximum volume index
    val AudioManager.mediaMaxVolume:Int
        get() = this.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    // Extension property to get media/music current volume index
    val AudioManager.mediaCurrentVolume:Int
        get() = this.getStreamVolume(AudioManager.STREAM_MUSIC)

    private val activityTransitionReceiver : BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newActivityType = intent.getStringExtra(ActivityTransitionReceiver.ACTIVITY_TYPE) ?: "WALKING"
            navigatorViewModel.updateActivityType(newActivityType)
        }
    }
}
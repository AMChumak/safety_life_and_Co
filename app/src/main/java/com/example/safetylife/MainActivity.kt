package com.example.safetylife

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.location.Geocoder
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.navigationandmap.ui.NavigatorViewModel
import com.example.navigationandmap.ui.models.PointsOnRoard
import com.example.navigationandmap.ui.models.QuadrantsOnMap
import com.example.safetylife.data.nightSetting
import com.example.safetylife.data.pushSetting
import com.example.safetylife.data.soundSetting
import com.google.android.gms.location.*
import com.google.gson.Gson
import org.json.JSONException
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity() {
    var pref : SharedPreferences? = null
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val TAG = "MainActivity"
    var isActive = false


    private val navigatorViewModel: NavigatorViewModel by viewModels()
    private val locationRequest: LocationRequest = createLocationRequest()
    private lateinit var locationCallback: LocationCallback
    private var angle: Double = 0.2

    fun Double.format(digits: Int) = "%.${digits}f".format(this)

    @SuppressLint("ResourceAsColor", "MissingInflatedId", "UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {

        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        setContentView(R.layout.activity_main)


        getSupportActionBar()?.setTitle("Safety Life")
        // sd
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.getDefault())
        getLoaction()

        val audioManager: AudioManager =
            getSystemService(AUDIO_SERVICE) as AudioManager
        var previousVolume: Int = audioManager.mediaCurrentVolume




        createChannel("userRiskInteraction", "Опасные ситуации")
        val notification = NotificationCompat.Builder(this, "userRiskInteraction")
            .setContentText("Оглянись! Рядом дорога.")
            .setContentTitle("Safety Live")
            .setSmallIcon(R.drawable.notify_icon_foreground)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setOngoing(true)

        val a = findViewById<Button>(R.id.button2)
        val buttobState = findViewById<Button>(R.id.button2)
        val backgroundImage = findViewById<ImageView>(R.id.imageView2)

        fun active(){


            if(isActive){
                isActive = !isActive
                buttobState.setBackgroundResource(R.drawable.circle_onoff_button)
                backgroundImage.setImageResource(R.drawable.turnon)
                //notificationManager.cancel(1);
            } else{
                buttobState.setBackgroundResource(R.drawable.circle_onoff_button_active)
                backgroundImage.setImageResource(R.drawable.turnon_active)

                /*val text = findViewById<TextView>(R.id.textViewConsole)
                text.text = "Distance:${navigatorViewModel.uiState.dist.format(4)}\n" +
                        "latitude: ${navigatorViewModel.uiState.latitude.format(3)}   longitude: ${navigatorViewModel.uiState.longitude.format(3)}\n" +
                        "direction: ${navigatorViewModel.uiState.direction.format(3)}\n" +
                        "userX: ${navigatorViewModel.uiState.userX.format(3)}   userY: ${navigatorViewModel.uiState.userY.format(3)}\n" +
                        "movedX: ${navigatorViewModel.uiState.userMovedX.format(3)}   movedY: ${navigatorViewModel.uiState.userMovedY.format(3)}\n" +
                        "point: ${navigatorViewModel.uiState.closestPoint}   size: ${navigatorViewModel.uiState.sizeRoad} type: ${navigatorViewModel.uiState.type}\n" +
                        "pointX: ${navigatorViewModel.uiState.pointX}   pointY: ${navigatorViewModel.uiState.pointY}"

                Toast.makeText(this,"Distance:${navigatorViewModel.uiState.dist.format(4)}\n" +
                        "latitude: ${navigatorViewModel.uiState.inDangerous}",Toast.LENGTH_SHORT).show()*/

                isActive = !isActive
            }
            //notificationManager.notify(1, notification.build()) // !1
        }
        a.setOnClickListener {
            active()
        }



        pref = getSharedPreferences("Pass", MODE_PRIVATE)
        var test = pref?.getString("pass", "hi")!!

        //для компаса
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,  IntentFilter(LocatyService.KEY_ON_SENSOR_CHANGED_ACTION))

        // поделючение JSON
        try{
            val jsonPointsString  = getJSONFromAssets("log_298.json")
            val points = Gson().fromJson(jsonPointsString, PointsOnRoard::class.java)
            val jsonQuadrantsString  = getJSONFromAssets("chunk_log_298.json")
            val quadrants = Gson().fromJson(jsonQuadrantsString, QuadrantsOnMap::class.java)
            navigatorViewModel.loadMapPointsAndQuadrants(points.points, quadrants.quadrants)
        } catch (e: JSONException){
            e.printStackTrace()
        }


        // создаем locationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0?: return
                previousVolume = audioManager.mediaCurrentVolume
                val currentTime = Calendar.getInstance().time
                val hour = currentTime.hours
                for (location in p0.locations){
                    navigatorViewModel.updateCoordinates(location.latitude, location.longitude, angle)
                    if(navigatorViewModel.uiState.inDangerous){
                        if((nightSetting) or !( hour in 0..6 )){
                            if(pushSetting) notificationManager.notify(1, notification.build())
                            if(soundSetting) {
                                if(audioManager.mediaCurrentVolume >5)audioManager.setMediaVolume(5)
                            }
                        }

                        //zvuk
                        //val previousVolume: Int = audioManager.mediaCurrentVolume
                        //audioManager.setMediaVolume(3)
                        //Thread.sleep(3000)
                        //if (audioManager.mediaCurrentVolume ==3) audioManager.setMediaVolume(previousVolume)
                    } else{
                        if(nightSetting){
                            if(pushSetting) audioManager.setMediaVolume(previousVolume)
                            if(soundSetting) notificationManager.cancel(1)
                        }

                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startForegroundServiceForSensors()
        if (true) startLocationUpdates() // вообще вместо true стоит requestingLocationUpdate
    }

    private fun startLocationUpdates() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10101)
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
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
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 10101)
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
            0// Flags
        )
    }

    // Extension property to get media maximum volume index
    val AudioManager.mediaMaxVolume:Int
        get() = this.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    // Extension property to get media/music current volume index
    val AudioManager.mediaCurrentVolume:Int
        get() = this.getStreamVolume(AudioManager.STREAM_MUSIC)

}
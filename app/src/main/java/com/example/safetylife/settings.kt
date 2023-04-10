package com.example.safetylife

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.safetylife.data.nightSetting
import com.example.safetylife.data.pushSetting
import com.example.safetylife.data.soundSetting


class settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        getSupportActionBar()?.setTitle("Настройки")
        val pushSwitch = findViewById<Switch>(R.id.switch1)
        val nightSwitch = findViewById<Switch>(R.id.switch2)
        val soundSwitch = findViewById<Switch>(R.id.switch4)

        pushSwitch.isChecked = pushSetting
        nightSwitch.isChecked = nightSetting
        soundSwitch.isChecked = soundSetting
        pushSwitch.setOnClickListener{
            pushSetting = pushSwitch.isChecked
        }
        nightSwitch.setOnClickListener{
            nightSetting = nightSwitch.isChecked
        }
        soundSwitch.setOnClickListener{
            soundSetting = soundSwitch.isChecked
        }

        val nightbut = findViewById<TextView>(R.id.textView10)
    }

    fun settingPriority(v: View){
        startActivity(Intent(Settings.ACTION_SETTINGS))
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

    }
}
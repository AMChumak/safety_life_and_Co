package com.example.safetylife

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button

class settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        getSupportActionBar()?.setTitle("Настройки")

    }

    fun openControl(v : View) {
        val intent = Intent(this, Control::class.java)
        startActivity(intent)
    }

    fun openAlert(v : View) {
        val intent = Intent(this, FullScreenAlert::class.java)
        startActivity(intent)
    }

    fun settingPriority(v: View){
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
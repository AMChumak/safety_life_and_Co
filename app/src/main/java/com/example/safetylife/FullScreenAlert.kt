package com.example.safetylife

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class FullScreenAlert : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_alert)

        getSupportActionBar()?.setTitle("Уведомление")
    }

    fun openAct(v : View){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
package com.example.safetylife

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity


class Control : AppCompatActivity() {
    var pref : SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)
        val input = findViewById<EditText>(R.id.Password)
        var setti = findViewById<Button>(R.id.button6)
        setti.setBackgroundColor(Color.RED)
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                pref = getSharedPreferences("Pass", MODE_PRIVATE)
                var test = pref?.getString("pass", "hi")!!
                if(test != input.text.toString()){
                    setti.setBackgroundColor(Color.RED)
                }else{
                    setti.setBackgroundColor(Color.rgb(93,81,237))
                }
            }
        })
    }
    fun save(v : View){
        val result = findViewById<EditText>(R.id.Password).text.toString()
        saveData(result)
    }
    fun saveData(res: String){
        val editor = pref?.edit()
        editor?.putString("pass", res)
        editor?.apply()
    }
}
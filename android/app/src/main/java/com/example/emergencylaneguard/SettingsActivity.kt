package com.example.emergencylaneguard

import com.yrd.emergencylanemobile.R

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val seekBar = findViewById<SeekBar>(R.id.seekBarSensitivity)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // Load values
        seekBar.progress = (prefs.getFloat("sensitivity", 0.6f) * 100).toInt()

        btnSave.setOnClickListener {
            val sensitivity = seekBar.progress / 100f

            prefs.edit()
                .putFloat("sensitivity", sensitivity)
                .apply()

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

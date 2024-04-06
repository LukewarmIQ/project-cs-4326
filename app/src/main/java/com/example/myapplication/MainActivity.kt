package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi


class MainActivity : ComponentActivity() {

    private val permissions = arrayOf(Manifest.permission.CAMERA)
    private val requestCode = 101

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_menu)

        val button1: Button = findViewById(R.id.camera)
        val button2: Button = findViewById(R.id.gallery)
        val button3: Button = findViewById(R.id.results)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        checkPermissions()

        button1.setOnClickListener {
            // Take picture with camera
            startActivity(Intent(this, CameraActivity::class.java))
            // Vibrate the phone
            vibrator.vibrate(VibrationEffect.createOneShot(200,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }

        button2.setOnClickListener {
            // Select picture from gallery
            startActivity(Intent(this, GallerySelect::class.java))
            vibrator.vibrate(VibrationEffect.createOneShot(100,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            )
        }

        button3.setOnClickListener {
            // View previous results
            startActivity(Intent(this, GalleryActivity::class.java))
            vibrator.vibrate(VibrationEffect.createOneShot(100,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            )

        }

    }

    private fun checkPermissions() {
        val notGrantedPermissions = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notGrantedPermissions.add(permission)
            }
        }

        if (notGrantedPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                notGrantedPermissions.toTypedArray(),
                requestCode
            )
        }
    }
}

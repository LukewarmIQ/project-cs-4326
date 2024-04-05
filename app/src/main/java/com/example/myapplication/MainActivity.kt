package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {

    private val permissions = arrayOf(Manifest.permission.CAMERA)
    private val requestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_menu)

        val button1: Button = findViewById(R.id.camera)
        val button2: Button = findViewById(R.id.gallery)
        val button3: Button = findViewById(R.id.results)

        checkPermissions()

        button1.setOnClickListener {
            // Take picture with camera
            startActivity(Intent(this, CameraActivity::class.java))
        }

        button2.setOnClickListener {
            // Select picture from gallery
            startActivity(Intent(this, GallerySelect::class.java))
        }

        button3.setOnClickListener {
            // View previous results
            startActivity(Intent(this, GalleryActivity::class.java))

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

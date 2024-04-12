package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ToggleButton
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private val permissions = arrayOf(Manifest.permission.CAMERA)
    private val requestCode = 101
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var textToSpeech: TextToSpeech

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_menu)

        textToSpeech = TextToSpeech(this, this)

        val button1: Button = findViewById(R.id.camera)
        val button2: Button = findViewById(R.id.gallery)
        val button3: Button = findViewById(R.id.results)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val soundToggle: ToggleButton = findViewById(R.id.sound_toggle)

        checkPermissions()
        checkSoundToggle(soundToggle)
        soundToggle.setOnCheckedChangeListener { _, isChecked ->
            soundToggle.post {
                if (isChecked) {
                    soundToggle.text = "Sound Effects: On"
                } else {
                    soundToggle.text = "Sound Effects: Off"
                }
                speak(soundToggle.text as String)
            }
        }

        button1.setOnClickListener {
            // Take picture with camera
            val intent = Intent(this, CameraActivity::class.java)
            intent.putExtra("SoundEnabled", soundToggle.isChecked)
            startActivity(intent)

            // Play sound effect
            mediaPlayer = MediaPlayer.create(this, R.raw.camera_start)
            if(soundToggle.isChecked) {
                mediaPlayer.start()
            }
            // Vibrate the phone
            vibrator.vibrate(VibrationEffect.createOneShot(200,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }

        button2.setOnClickListener {
            // Select picture from gallery
            startActivity(Intent(this, GallerySelect::class.java))
            if(soundToggle.isChecked) {
                mediaPlayer = MediaPlayer.create(this, R.raw.open_folder)
                mediaPlayer.start()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(100,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            )
        }

        button3.setOnClickListener {
            // View previous results
            startActivity(Intent(this, GalleryActivity::class.java))
            if(soundToggle.isChecked) {
                mediaPlayer = MediaPlayer.create(this, R.raw.open_recent)
                mediaPlayer.start()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(100,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            )

        }

    }

    // Override onDestroy method to release MediaPlayer
    override fun onDestroy() {
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        mediaPlayer.release()
        super.onDestroy()
    }

    private fun checkSoundToggle(soundToggle: ToggleButton) {
        soundToggle.post {
            if (soundToggle.isChecked) {
                soundToggle.text = "Sound Effects: On"
            } else {
                soundToggle.text = "Sound Effects: Off"
            }
        }
    }

    private fun speak(text: String) {
        val result = textToSpeech.setLanguage(Locale.US)
        try {
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not Supported")
            } else {
                var ttsText = text
                textToSpeech.speak(ttsText, TextToSpeech.QUEUE_ADD, null, null)
            }
        }
        catch (e: Exception) {
            Log.e("TTS", "TTS failed to send message")
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

    override fun onInit(status: Int) {
        // Implement your initialization logic here
        if (status == TextToSpeech.SUCCESS) {
            // Text-to-speech initialization successful
        } else {
            Log.e("TTS", "TTS not initialized")
        }
    }

}

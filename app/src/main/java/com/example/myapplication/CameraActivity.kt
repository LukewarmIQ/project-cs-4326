package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private var imageCapture: ImageCapture? = null

    interface ImageCaptureCallback {
        fun onPictureTaken(directoryName: String)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_picture)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()

        captureButton.setOnClickListener {
            takePhoto()
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(200,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            //preview
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            //set Image Capture Builder
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
                Log.d(TAG, "Camera bind success")
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val timeStamp = System.currentTimeMillis() // Get current UNIX timestamp
        val outputDirectory = File(applicationContext.getExternalFilesDir(null), "/results/$timeStamp")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val photoFile = File(outputDirectory, "picture.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: photoFile.toUri()
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    imageCaptureCallback.onPictureTaken(outputDirectory.absolutePath)

                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    // Define a function to handle the captured image
    fun handleCapturedImage(directoryName: String) {
        Log.d("Test", "Attempting to display the result...")
        // Display the result of the [currently dummy] api call.
        //logDirectoryContents("File://$directoryPath")

        // APIRequest(applicationContext,"$directoryName/picture.jpg")

        val intent = Intent(this, ObjectDetector::class.java)
        intent.putExtra("directoryPath", directoryName)
        startActivity(intent)
    }

    val imageCaptureCallback = object : ImageCaptureCallback {
        override fun onPictureTaken(directoryName: String) {
            // Call your function to handle the captured image
            handleCapturedImage(directoryName)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
    }
}
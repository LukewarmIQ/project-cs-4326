package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class GallerySelect : ComponentActivity() {
    //
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    val timeStamp = System.currentTimeMillis()
                    val outputDirectory = File(applicationContext.getExternalFilesDir(null), "/results/$timeStamp")
                    outputDirectory.mkdirs()
                    val file = File(outputDirectory, "picture.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    // Attempt to display the result
                    Log.d("Test", "Attempting to display the result...")
                    val intent = Intent(this, ObjectDetector::class.java)
                    intent.putExtra("directoryPath", outputDirectory.absolutePath)
                    startActivity(intent)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        else {
            // User cancelled, return to main
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }
}
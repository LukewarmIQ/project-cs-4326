package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.Locale

class ObjectDetector : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        private const val MAX_FONT_SIZE = 300F
        // This seems like a good threshold but can be tweaked to allow for less
        // or more objects to be detected
        private const val THRESHOLD_DISTANCE_RATIO = 0.15
    }

    private lateinit var inputImageView: ImageView
    private lateinit var currentPhotoPath: String
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var largeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_api_result)

        // Retrieve directory path passed from intent
        val directoryPath = intent.getStringExtra("directoryPath")
        currentPhotoPath = "$directoryPath/picture.jpg"

        // Find views
        inputImageView = findViewById<ImageView>(R.id.imageView)
        largeTextView = findViewById<TextView>(R.id.largeTextView)

        // Initialize text to speech module
        textToSpeech = TextToSpeech(this, this)

        // Set custom title bar
        supportActionBar?.apply {
            // Set custom title text
            title = "Object Detection Results"
        }
        // Display the image and find objects
        setViewAndDetect(BitmapFactory.decodeFile(currentPhotoPath))
    }

    /**
     * runObjectDetection(bitmap: Bitmap)
     *      TFLite Object Detection function
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        // Step 1: create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(bitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this, // the application context
            "model.tflite", // basic model
            options
        )

        // Step 3: feed given image to the model and print the detection result
        val results = detector.detect(image)

        // Step 4: filter out any objects not in center of image
        val centerX = image.width / 2
        val centerY = image.height / 2
        val thresholdDistance = minOf(image.width, image.height) * THRESHOLD_DISTANCE_RATIO

        var resultsToDisplay = results.filter { detection ->
            val boxCenterX = (detection.boundingBox.left + detection.boundingBox.right) / 2
            val boxCenterY = (detection.boundingBox.top + detection.boundingBox.bottom) / 2
            val distanceToCenter = Math.sqrt(
                ((centerX - boxCenterX) * (centerX - boxCenterX) + (centerY - boxCenterY) * (centerY - boxCenterY)).toDouble()
            )
            distanceToCenter < thresholdDistance
        }.map {detection ->
            // Get the top-1 category and craft the display text
            val category = detection.categories.first()
            val text = "${category.label}"
            // val score = category.score.times(100).toInt()

            // Create a data object to display the detection result
            DetectionResult(detection.boundingBox, text)
        }

        // Step 5: If nothing is detected, say so
        if(resultsToDisplay.isEmpty() || results.isEmpty()){
            resultsToDisplay = listOf(DetectionResult(RectF(0F,0F,0F,0F), "No Object Detected"))
        }

        // Draw the detection result on the bitmap and show it.
        val imgWithResult = drawDetectionResult(bitmap, resultsToDisplay)
        runOnUiThread {
            inputImageView.setImageBitmap(imgWithResult)
        }
    }

    private fun setViewAndDetect(bitmap: Bitmap) {


        // Display capture image
        val bitmap = fixImageRotation(BitmapFactory.decodeFile(currentPhotoPath))
        inputImageView.setImageBitmap(bitmap)

        // Run ODT and display result
        // Note that we run this in the background thread to avoid blocking the app UI because
        // TFLite object detection is a synchronised process.
        lifecycleScope.launch(Dispatchers.Default) { runObjectDetection(bitmap) }
    }

    private fun fixImageRotation(bitmap: Bitmap): Bitmap {

        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>

    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = Color.BLUE
            pen.strokeWidth = 16F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)

            // Text-to-speech the results
            speakResults(it.text)

            runOnUiThread {
                largeTextView.text = it.text // Update largeTextView text here
            }

        }
        return outputBitmap
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Custom back button behavior
        // Return to main menu, remove other back button options from the stack.
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun speakResults(text: String) {
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

    override fun onDestroy() {
        // Shutdown text to speech engine when activity is destroyed
        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }
        textToSpeech.shutdown()
        super.onDestroy()
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

/**
 * DetectionResult
 *      A class to store the visualization info of a detected object.
 */
data class DetectionResult(val boundingBox: RectF, val text: String)
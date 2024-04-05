package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private lateinit var imagePaths: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_gallery)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        imagePaths = getImagePaths()
        adapter = ImageAdapter(imagePaths)
        recyclerView.adapter = adapter

        supportActionBar?.apply {
            // Set custom title text
            title = "Previous Results"
        }
    }

    private fun getImagePaths(): List<String> {
        // Get all images and sort from newest to oldest
        val paths = ArrayList<String>()
        val resultsDir = File(applicationContext.getExternalFilesDir(null), "/results")
        if (resultsDir.exists() && resultsDir.isDirectory) {
            val timestampedFolders = resultsDir.listFiles(File::isDirectory)
            timestampedFolders?.forEach { timestampedFolder ->
                val pictureFile = File(timestampedFolder, "picture.jpg")
                if (pictureFile.exists()) {
                    paths.add(pictureFile.absolutePath)
                } else {
                    Log.d("ERROR","Picture does not exist in $timestampedFolder")
                }
            }
        } else {
            Log.d("ERROR","Results directory does not exist or is not a directory.")
        }

        paths.sortWith(Comparator { o1, o2 ->
            val f1 = File(o1)
            val f2 = File(o2)
            java.lang.Long.compare(f2.lastModified(), f1.lastModified())
        })

        return paths
    }

    inner class ImageAdapter(private val imagePaths: List<String>) :
        RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val imagePath = imagePaths[position]
            val width = recyclerView.width / 3 // Calculate the width of each image (half of RecyclerView's width)
            Glide.with(holder.itemView.context)
                .load(imagePath)
                .override(width, width) // Resize the image to fit the calculated width and height (square)
                .centerCrop()
                .into(holder.imageView)

            holder.imageView.setOnClickListener {
                val timestampedFolder = File(imagePath).parentFile?.absolutePath
                val intent = Intent(holder.itemView.context, ObjectDetector::class.java).apply {
                    putExtra("directoryPath", timestampedFolder)
                }
                holder.itemView.context.startActivity(intent)
            }
        }


        override fun getItemCount(): Int {
            return imagePaths.size
        }

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.image_view)
        }
    }
}
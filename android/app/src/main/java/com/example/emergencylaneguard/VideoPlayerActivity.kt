package com.example.emergencylaneguard

import com.yrd.emergencylanemobile.R

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.MediaController
import android.widget.Toast
import android.widget.ImageView
import android.widget.VideoView
import com.bumptech.glide.Glide
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.emergencylaneguard.database.ViolationRecord
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {

    private val viewModel: ViolationViewModel by viewModels {
        ViolationViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val videoPath = intent.getStringExtra("video_path") ?: return
        val imagePath = intent.getStringExtra("image_path") ?: ""
        val recordId = intent.getLongExtra("record_id", -1L)
        val status = intent.getIntExtra("status", 0)
        
        val videoView = findViewById<VideoView>(R.id.videoView)
        val imageView = findViewById<ImageView>(R.id.imageView)
        
        if (videoPath.isEmpty()) {
            // Image Mode
            videoView.visibility = android.view.View.GONE
            imageView.visibility = android.view.View.VISIBLE
            
            if (imagePath.isNotEmpty()) {
                Glide.with(this)
                    .load(if (imagePath.startsWith("content://")) Uri.parse(imagePath) else File(imagePath))
                    .into(imageView)
            } else {
                Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Video Mode
            videoView.visibility = android.view.View.VISIBLE
            imageView.visibility = android.view.View.GONE
            
            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            
            try {
                if (videoPath.startsWith("content://")) {
                    videoView.setVideoURI(Uri.parse(videoPath))
                    videoView.start()
                } else if (File(videoPath).exists()) {
                    videoView.setVideoPath(videoPath)
                    videoView.start()
                } else {
                    Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Error playing video", e)
                Toast.makeText(this, "Cannot play video", Toast.LENGTH_SHORT).show()
            }
        }

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        val btnArchive = findViewById<Button>(R.id.btnArchive)

        if (status == 1) {
            btnArchive.text = "Uploaded"
            btnArchive.isEnabled = false
        }

        btnDelete.setOnClickListener {
            if (recordId != -1L) {
                viewModel.deleteViolation(recordId)
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnArchive.setOnClickListener {
            if (recordId != -1L) {
                // Use local recognition logic from ViewModel
                // Since processViolations takes a list, we wrap the current record
                val record = ViolationRecord(
                    id = recordId,
                    plateNumber = "",
                    timestamp = System.currentTimeMillis(), // Placeholder, will be updated
                    videoPath = videoPath,
                    imagePath = imagePath,
                    status = 0
                )
                
                viewModel.processViolations(listOf(record))
                
                // Show feedback and finish
                Toast.makeText(this, "Processing in background...", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

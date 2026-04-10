package com.example.emergencylaneguard

import com.yrd.emergencylanemobile.R

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

import android.util.Log

import android.widget.Toast
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData

class CaseDetailActivity : AppCompatActivity() {
    
    private val viewModel: ViolationViewModel by viewModels {
        ViolationViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CaseDetailActivity", "onCreate called")
        setContentView(R.layout.activity_case_detail)
        
        val plate = intent.getStringExtra("plate_number")
        Log.d("CaseDetailActivity", "Received plate: $plate")
        
        if (plate == null) {
            Log.e("CaseDetailActivity", "Plate number is null, finishing")
            return finish()
        }
        
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvNarrative = findViewById<TextView>(R.id.tvNarrative)
        val btnGenerateReport = findViewById<android.widget.Button>(R.id.btnGenerateReport)
        val btnPlayVideo = findViewById<android.widget.Button>(R.id.btnPlayVideo)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        
        tvTitle.text = "Case: $plate"
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        btnGenerateReport.setOnClickListener {
            tvNarrative.text = "Generating report..."
            viewModel.generateReport(plate)
        }
        
        // Load Case Info
        viewModel.getCaseInfoFlow(plate).observe(this) { caseInfo ->
            if (caseInfo != null) {
                tvNarrative.text = caseInfo.narrative.ifEmpty { "No narrative available." }
                
                // Check if video is available
                val videoAvailable = if (caseInfo.videoClipPath.startsWith("content://")) {
                    true // Assume Uri is valid or check via ContentResolver if needed
                } else {
                    caseInfo.videoClipPath.isNotEmpty() && java.io.File(caseInfo.videoClipPath).exists()
                }

                if (videoAvailable) {
                    btnPlayVideo.visibility = android.view.View.VISIBLE
                    btnPlayVideo.setOnClickListener {
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            putExtra("video_path", caseInfo.videoClipPath)
                            putExtra("plate", plate)
                        }
                        startActivity(intent)
                    }
                } else {
                    btnPlayVideo.visibility = android.view.View.GONE
                }
            }
        }
        
        // Long click to copy narrative
        tvNarrative.setOnLongClickListener {
            val text = tvNarrative.text.toString()
            if (text.isNotEmpty() && text != "Narrative..." && text != "Generating report...") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Case Report", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }

        val adapter = ViolationAdapter(onClick = { record ->
            // Open Image Viewer (reuse VideoPlayerActivity in Image Mode)
            val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("video_path", "") // Force Image Mode
                putExtra("image_path", record.imagePath)
                putExtra("record_id", record.id)
                putExtra("status", record.status)
                putExtra("plate", record.plateNumber)
                putExtra("timestamp", record.timestamp)
            }
            startActivity(intent)
        })
        recyclerView.adapter = adapter
        
        // Load Violations
        viewModel.getCaseViolations(plate).observe(this) { list ->
            adapter.submitList(list)
        }
    }
}

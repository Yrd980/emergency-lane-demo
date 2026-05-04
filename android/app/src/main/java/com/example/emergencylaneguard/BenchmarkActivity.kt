package com.example.emergencylaneguard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class BenchmarkActivity : AppCompatActivity() {

    private lateinit var laneDetector: LaneDetector
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var btnBenchmark: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvResults: TextView

    private val executor = Executors.newSingleThreadExecutor()
    private var frameCount = 0
    private var totalInferenceMs = 0L
    private var totalPreprocessMs = 0L
    private var benchmarkStartMs = 0L
    private var isBenchmarking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_benchmark)

        previewView = findViewById(R.id.viewFinder)
        overlayView = findViewById(R.id.overlayView)
        btnBenchmark = findViewById(R.id.btnBenchmark)
        tvStatus = findViewById(R.id.tvStatus)
        tvResults = findViewById(R.id.tvResults)

        laneDetector = LaneDetector()
        if (laneDetector.initialize(assets)) {
            tvStatus.text = "Model: YOLOv8n | Backend: ncnn | Status: Ready"
        } else {
            tvStatus.text = "Model load failed"
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
        }

        btnBenchmark.setOnClickListener {
            if (!isBenchmarking) {
                startBenchmark()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                processImage(imageProxy)
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
            )
            tvStatus.text = "Model: YOLOv8n | Backend: ncnn | Camera: Ready"
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startBenchmark() {
        frameCount = 0
        totalInferenceMs = 0L
        totalPreprocessMs = 0L
        benchmarkStartMs = System.currentTimeMillis()
        isBenchmarking = true
        btnBenchmark.isEnabled = false
        tvResults.text = "Running 100 frames..."
    }

    private fun processImage(imageProxy: ImageProxy) {
        val preprocessStart = System.currentTimeMillis()
        val bitmap = imageProxy.toBitmap()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = if (rotation != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotation.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
        val preprocessMs = System.currentTimeMillis() - preprocessStart
        totalPreprocessMs += preprocessMs

        val inferenceStart = System.currentTimeMillis()
        val results = laneDetector.runDetection(rotatedBitmap)
        val inferenceMs = System.currentTimeMillis() - inferenceStart
        totalInferenceMs += inferenceMs

        if (isBenchmarking) {
            frameCount++
            if (frameCount >= 100) {
                finishBenchmark()
            }
        }

        if (results.isNotEmpty()) {
            val vehicleResults = results.filter {
                it.cls in setOf(2, 5, 7) && it.score > 0.3f
            }
            runOnUiThread {
                overlayView.updateDetections(vehicleResults, rotatedBitmap.width, rotatedBitmap.height)
            }
        }

        imageProxy.close()
    }

    private fun finishBenchmark() {
        isBenchmarking = false
        val avgInference = totalInferenceMs / 100
        val avgPreprocess = totalPreprocessMs / 100
        val avgTotal = avgInference + avgPreprocess
        val fps = 1000.0 / avgTotal

        runOnUiThread {
            tvResults.text = """
                基准测试完成 (100 帧)
                平均推理时间: ${avgInference}ms
                平均预处理时间: ${avgPreprocess}ms
                平均总帧处理时间: ${avgTotal}ms
                有效 FPS: ${"%.1f".format(fps)}
            """.trimIndent()
            btnBenchmark.isEnabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        laneDetector.destroy()
        executor.shutdown()
    }
}

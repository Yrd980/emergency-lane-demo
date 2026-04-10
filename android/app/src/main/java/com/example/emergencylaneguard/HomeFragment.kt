package com.example.emergencylaneguard

import com.yrd.emergencylanemobile.R

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.emergencylaneguard.database.ViolationRecord
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors

import com.example.emergencylaneguard.utils.MediaStoreUtils
import android.provider.MediaStore
import android.content.ContentValues

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: ViolationViewModel by viewModels {
        ViolationViewModelFactory(requireActivity().application)
    }

    private lateinit var overlayView: OverlayView
    private lateinit var laneDetector: LaneDetector
    private lateinit var btnRecord: FloatingActionButton
    private lateinit var btnCapture: FloatingActionButton
    private lateinit var tvStatus: TextView
    private lateinit var tvDetectionStatus: TextView
    
    private val executor = Executors.newSingleThreadExecutor()
    
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var imageCapture: ImageCapture? = null  // Re-added for high-res photos

    // Camera state management
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCameraInitialized = false

    private var isRecording = false
    private var currentVideoFile: File? = null
    private var recordingStartTime: Long = 0
    
    private var detectionThreshold = 0.7f
    private var lastCaptureTime = 0L
    private val CAPTURE_COOLDOWN_MS = 2000L  // 2秒冷却
    private var detectionCount = 0
    private var captureCount = 0

    // Vehicle classes in COCO dataset: only Car (2) and Truck (7)
    private val vehicleClasses = setOf(2, 7)

    private val permissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val cameraGranted = perms[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            startCamera()
        } else {
            tvStatus.text = "Camera permission required"
            Toast.makeText(requireContext(), "需要摄像头权限才能使用此功能", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        overlayView = view.findViewById(R.id.overlayView)
        btnRecord = view.findViewById(R.id.btnRecord)
        btnCapture = view.findViewById(R.id.btnCapture)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvDetectionStatus = view.findViewById(R.id.tvDetectionStatus)
        
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        detectionThreshold = prefs.getFloat("sensitivity", 0.7f)

        laneDetector = LaneDetector()
        try {
            if (laneDetector.initialize(requireContext().assets)) {
                tvStatus.text = "Model Loaded ✓"
                tvDetectionStatus.text = "Click record to start"
            } else {
                tvStatus.text = "Model Load Failed ✗"
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to init LaneDetector", e)
            tvStatus.text = "Error: Model Exception"
        }

        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        btnCapture.setOnClickListener {
            // Manual capture
            if (isRecording) {
                // For manual capture, we don't have a specific bitmap from analysis easily accessible here
                // So we pass null and rely on ImageCapture. If ImageCapture is null, this button might not work well
                // unless we implement a way to grab the latest frame.
                val previewView = view.findViewById<PreviewView>(R.id.viewFinder)
                if (imageCapture != null) {
                    takePhoto(null)
                } else if (previewView != null && previewView.bitmap != null) {
                     // Fallback: Use PreviewView bitmap for manual capture in compatibility mode
                     Log.d("HomeFragment", "Using PreviewView bitmap for manual capture")
                     takePhoto(previewView.bitmap)
                } else {
                    Toast.makeText(context, "Snapshot failed: No image source available", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Start recording first", Toast.LENGTH_SHORT).show()
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun startCamera() {
        // Check if already initialized
        if (isCameraInitialized) {
            Log.d("HomeFragment", "Camera already initialized, skipping")
            return
        }

        Log.d("HomeFragment", "Starting camera initialization...")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Unbind all use cases first
                cameraProvider.unbindAll()

                // Get PreviewView and ensure it's ready
                val previewView = requireView().findViewById<PreviewView>(R.id.viewFinder)

                // Create Preview use case
                val preview = Preview.Builder().build()

                // Delay setting SurfaceProvider to ensure PreviewView is laid out
                previewView.post {
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Image analysis for YOLO detection
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(executor) { imageProxy -> processImage(imageProxy) }

                // Image capture for taking photos
                // Try to initialize ImageCapture, but handle potential binding failures later
                val imageCaptureBuilder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                
                imageCapture = imageCaptureBuilder.build()

                // Video capture for recording
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                // Bind use cases to lifecycle
                // Try binding all 4 use cases first
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                        imageCapture,
                        videoCapture
                    )
                    Log.d("HomeFragment", "Bound all 4 use cases successfully")
                } catch (e: Exception) {
                    Log.e("CameraX", "Failed to bind 4 use cases, falling back to 3 (removing ImageCapture)", e)
                    // Fallback: Bind only Preview + Analysis + Video
                    try {
                        cameraProvider.unbindAll()
                        imageCapture = null // Disable ImageCapture
                        cameraProvider.bindToLifecycle(
                            viewLifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                            videoCapture
                        )
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "相机兼容模式：拍照将使用截图", Toast.LENGTH_LONG).show()
                        }
                    } catch (e2: Exception) {
                        Log.e("CameraX", "Fatal error binding camera use cases", e2)
                        activity?.runOnUiThread {
                            tvStatus.text = "Camera Fatal Error"
                        }
                    }
                }

                // Update state
                this.cameraProvider = cameraProvider
                isCameraInitialized = true

                // Update UI
                activity?.runOnUiThread {
                    tvStatus.text = "Camera Ready"
                }

                Log.d("HomeFragment", "Camera initialized successfully")

            } catch (exc: Exception) {
                Log.e("CameraX", "Camera initialization failed", exc)
                activity?.runOnUiThread {
                    tvStatus.text = "Camera Error: ${exc.message}"
                    Toast.makeText(requireContext(), "摄像头初始化失败: ${exc.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun isCameraAvailable(): Boolean {
        return try {
            val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            cameraIds.any { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to check camera availability", e)
            false
        }
    }

    private fun releaseCamera() {
        try {
            cameraProvider?.unbindAll()
            Log.d("HomeFragment", "Camera released")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error releasing camera", e)
        }
        cameraProvider = null
        isCameraInitialized = false
    }

    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return
        
        recordingStartTime = System.currentTimeMillis()
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_$recordingStartTime.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/EmergencyLaneGuard")
            }
        }
        
        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
        
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        try {
            val builder = videoCapture.output.prepareRecording(requireContext(), mediaStoreOutputOptions)
            
            recording = if (hasAudioPermission) {
                builder.withAudioEnabled().start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    handleRecordEvent(recordEvent)
                }
            } else {
                builder.start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    handleRecordEvent(recordEvent)
                }
            }
            
            isRecording = true
            btnRecord.setImageResource(android.R.drawable.ic_media_pause)
            btnRecord.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_dark)
            tvStatus.text = "Recording..."
            tvDetectionStatus.text = "Monitoring for vehicles..."
            captureCount = 0
            
            Log.d("HomeFragment", "Recording started")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to start recording", e)
            Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        isRecording = false
        btnRecord.setImageResource(android.R.drawable.ic_menu_camera)
        btnRecord.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
        tvStatus.text = "Camera Ready"
        tvDetectionStatus.text = "Click record to start"
        
        overlayView.updateDetections(emptyList(), 0, 0)
        
        try {
            recording?.stop()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error stopping recording", e)
        }
        recording = null
        
        if (captureCount > 0) {
            Toast.makeText(context, "Saved $captureCount vehicle photos", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleRecordEvent(recordEvent: VideoRecordEvent) {
        when(recordEvent) {
            is VideoRecordEvent.Finalize -> {
                if (!recordEvent.hasError()) {
                    val uri = recordEvent.outputResults.outputUri
                    Log.d("HomeFragment", "Video saved: $uri")
                    
                    // Save the full video record to database when recording finishes
                    val record = ViolationRecord(
                        plateNumber = "Full Trip Recording",
                        timestamp = recordingStartTime,
                        videoPath = uri.toString(),
                        imagePath = "", 
                        latitude = 0.0,
                        longitude = 0.0,
                        status = 0
                    )
                    viewModel.addViolation(record)
                    
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "行程视频已保存", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("HomeFragment", "Video error: ${recordEvent.error}")
                }
            }
            else -> {}
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        if (!isRecording) {
            imageProxy.close()
            return
        }
        
        val bitmap = imageProxy.convertToBitmap() ?: run {
            imageProxy.close()
            return
        }
        // Handle rotation
        val rotation = imageProxy.imageInfo.rotationDegrees
        val rotatedBitmap = if (rotation != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotation.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val results = laneDetector.runDetection(rotatedBitmap)
        detectionCount++
        
        // Log all detections for debugging
        if (results.isNotEmpty()) {
            Log.d("HomeFragment", "Raw detections: ${results.size}, Max score: ${results.maxOf { it.score }}")
            results.forEach { Log.d("HomeFragment", "  -> cls=${it.cls}, score=${it.score}") }
        }

        // Filter only vehicles
        val vehicleResults = results.filter {
            it.cls in vehicleClasses && it.score > detectionThreshold
        }
        
        val hasVehicle = vehicleResults.isNotEmpty()
        
        // Update UI
        activity?.runOnUiThread {
            if (hasVehicle) {
                val best = vehicleResults.maxByOrNull { it.score }
                val vehicleName = getVehicleName(best?.cls ?: 0)
                tvDetectionStatus.text = "🚗 $vehicleName (${String.format("%.0f", (best?.score ?: 0f) * 100)}%) | Photos: $captureCount"
                tvDetectionStatus.setTextColor(Color.parseColor("#00FF00"))
                
                // Only show vehicle boxes
                overlayView.updateDetections(vehicleResults, rotatedBitmap.width, rotatedBitmap.height)
            } else {
                if (detectionCount % 30 == 0) {
                    tvDetectionStatus.text = "Monitoring... | Photos: $captureCount"
                    tvDetectionStatus.setTextColor(Color.parseColor("#FFFFFF"))
                }
                overlayView.updateDetections(emptyList(), 0, 0)
            }
        }
        
        // Capture photo when vehicle detected (with cooldown)
        if (hasVehicle) {
            val now = System.currentTimeMillis()
            if (now - lastCaptureTime > CAPTURE_COOLDOWN_MS) {
                lastCaptureTime = now
                // Pass the rotated bitmap as fallback
                takePhoto(rotatedBitmap)
            }
        }
        imageProxy.close()
    }
    
    private fun takePhoto(fallbackBitmap: Bitmap?) {
        // If ImageCapture is available, use it for high-res photo
        if (imageCapture != null) {
            val timestamp = System.currentTimeMillis()
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$timestamp.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/EmergencyLaneGuard/pending")
                }
            }
            
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()
            
            imageCapture?.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        captureCount++
                        val uri = output.savedUri ?: return
                        Log.d("HomeFragment", "High-res photo saved: $uri")
                        saveRecordToDb(timestamp, uri.toString())
                    }
                    
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("HomeFragment", "Photo capture failed, trying fallback", exc)
                        // If capture fails, try fallback if bitmap is available
                        if (fallbackBitmap != null) {
                            saveViolationEvidence(fallbackBitmap)
                        }
                    }
                }
            )
        } else if (fallbackBitmap != null) {
            // Fallback to bitmap save
            saveViolationEvidence(fallbackBitmap)
        }
    }

    private fun saveRecordToDb(timestamp: Long, imagePath: String) {
        // Save to database
        // As requested: Only save the image record. The long video is recorded separately 
        // and should not be attached to every single detection event to avoid duplication confusion.
        // We set videoPath to empty string for these detection snapshots.
        val record = ViolationRecord(
            plateNumber = "Vehicle Detected",
            timestamp = timestamp,
            videoPath = "", // Detach video from individual snapshots
            imagePath = imagePath,
            latitude = 0.0, // TODO: Add real location
            longitude = 0.0,
            status = 0
        )
        viewModel.addViolation(record)
        
        activity?.runOnUiThread {
            Toast.makeText(context, "📸 Vehicle captured!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveViolationEvidence(bitmap: Bitmap) {
        // Fallback using MediaStore manually
        val uri = MediaStoreUtils.createImageUri(requireContext()) ?: return
        
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
            
            MediaStoreUtils.publishUri(requireContext(), uri)
            
            captureCount++
            Log.d("HomeFragment", "Photo saved: $uri")
            
            // Save to database
            val timestamp = System.currentTimeMillis()
            val record = ViolationRecord(
                plateNumber = "Vehicle Detected",
                timestamp = timestamp,
                videoPath = "", // Set to empty string so it's treated as an image record
                imagePath = uri.toString(),
                latitude = 0.0,
                longitude = 0.0,
                status = 0
            )
            viewModel.addViolation(record)
            
            activity?.runOnUiThread {
                Toast.makeText(context, "📸 Vehicle captured!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to save photo", e)
        }
    }
    
    private fun getVehicleName(cls: Int): String {
        return when (cls) {
            2 -> "Car"
            7 -> "Truck"
            else -> "Vehicle"
        }
    }

    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted() && !isCameraInitialized) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseCamera()
        isRecording = false
        laneDetector.destroy()
        executor.shutdown()
        try {
            recording?.stop()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error stopping recording", e)
        }
    }

    private fun ImageProxy.convertToBitmap(): Bitmap? {
        if (format != ImageFormat.YUV_420_888) return null
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}

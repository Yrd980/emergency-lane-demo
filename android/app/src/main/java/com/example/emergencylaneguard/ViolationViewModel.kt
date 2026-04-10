package com.example.emergencylaneguard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.emergencylaneguard.database.AppDatabase
import com.example.emergencylaneguard.database.ViolationRecord
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.emergencylaneguard.database.CaseInfo
import com.example.emergencylaneguard.utils.MediaStoreUtils
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.hyperai.hyperlpr3.HyperLPR3
import com.hyperai.hyperlpr3.bean.HyperLPRParameter

import android.content.Context

class ViolationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).violationDao()

    init {
        try {
            val parameter = HyperLPRParameter()
                .setDetLevel(HyperLPR3.DETECT_LEVEL_LOW)
                .setMaxNum(1)
                .setRecConfidenceThreshold(0.5f)
            HyperLPR3.getInstance().init(application, parameter)
            Log.d("HyperLPR3", "SDK Initialized successfully")
        } catch (e: Exception) {
            Log.e("HyperLPR3", "SDK Initialization failed", e)
        }
    }

    val pendingViolations = dao.getPendingViolations().asLiveData()
    val archivedViolations = dao.getArchivedViolations().asLiveData()
    val allCases = dao.getAllCases().asLiveData()

    fun getCaseViolations(plate: String) = dao.getProcessedViolationsByPlate(plate).asLiveData()
    
    fun getCaseInfoFlow(plate: String) = dao.getCaseByPlateFlow(plate).asLiveData()
    
    suspend fun getCaseInfo(plate: String) = dao.getCaseByPlate(plate)

    fun addViolation(record: ViolationRecord) {
        Log.d("ViolationViewModel", "addViolation called: plateNumber=${record.plateNumber}, imagePath=${record.imagePath}, videoPath=${record.videoPath}, status=${record.status}")
        viewModelScope.launch {
            try {
                val id = dao.insert(record)
                Log.d("ViolationViewModel", "Record inserted with id=$id")
            } catch (e: Exception) {
                Log.e("ViolationViewModel", "Failed to insert record", e)
            }
        }
    }
    
    fun archiveViolation(record: ViolationRecord) {
        viewModelScope.launch {
            dao.update(record.copy(status = ViolationRecord.STATUS_ARCHIVED))
        }
    }
    
    fun deleteViolation(id: Long) {
        viewModelScope.launch {
            val record = dao.getViolationById(id)
            if (record != null) {
                // Delete physical files
                MediaStoreUtils.deleteFile(getApplication(), record.imagePath)
                if (record.videoPath.isNotEmpty()) {
                    MediaStoreUtils.deleteFile(getApplication(), record.videoPath)
                }
                dao.deleteById(id)
            }
        }
    }

    fun deleteViolations(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                val record = dao.getViolationById(id)
                if (record != null) {
                    MediaStoreUtils.deleteFile(getApplication(), record.imagePath)
                    if (record.videoPath.isNotEmpty()) {
                        MediaStoreUtils.deleteFile(getApplication(), record.videoPath)
                    }
                }
            }
            dao.deleteByIds(ids)
        }
    }

    fun deleteCases(plates: List<String>) {
        viewModelScope.launch {
            plates.forEach { plate ->
                // Delete all violations associated with this case
                val violations = dao.getProcessedViolationsByPlateSync(plate)
                violations.forEach { record ->
                    MediaStoreUtils.deleteFile(getApplication(), record.imagePath)
                    if (record.videoPath.isNotEmpty()) {
                        MediaStoreUtils.deleteFile(getApplication(), record.videoPath)
                    }
                    dao.deleteById(record.id)
                }
                
                // Delete case info and clipped video
                val caseInfo = dao.getCaseByPlate(plate)
                if (caseInfo != null) {
                    if (caseInfo.videoClipPath.isNotEmpty()) {
                        MediaStoreUtils.deleteFile(getApplication(), caseInfo.videoClipPath)
                    }
                    dao.deleteCaseByPlate(plate)
                }
            }
        }
    }

    fun processViolations(records: List<ViolationRecord>) {
        viewModelScope.launch(Dispatchers.IO) {
            records.forEach { record ->
                if (record.imagePath.isEmpty()) return@forEach

                try {
                    val bitmap = loadBitmap(getApplication(), record.imagePath)
                    if (bitmap == null) {
                        Log.e("HyperLPR3", "Failed to decode bitmap: ${record.imagePath}")
                        val updatedRecord = record.copy(plateNumber = "Error: Image Load Failed")
                        dao.update(updatedRecord)
                        return@forEach
                    }
                    Log.d("HyperLPR3", "Analyzing image: ${bitmap.width}x${bitmap.height}")

                    // Use HyperLPR3 for recognition
                    val plates = HyperLPR3.getInstance().plateRecognition(bitmap, HyperLPR3.CAMERA_ROTATION_0, HyperLPR3.STREAM_BGRA)
                    
                    if (plates.isNotEmpty()) {
                        val plate = plates[0].code
                        val confidence = plates[0].confidence
                        Log.d("HyperLPR3", "Recognized: $plate with confidence $confidence")

                        // Success: Update Record and Move to Case
                        val updatedRecord = record.copy(
                            plateNumber = plate,
                            status = ViolationRecord.STATUS_PROCESSED
                        )
                        dao.update(updatedRecord)

                        // Move image to case folder
                        if (record.imagePath.startsWith("content://")) {
                            MediaStoreUtils.moveImageToFolder(getApplication(), record.imagePath, plate)
                        }

                        // Handle Case Info
                        val existingCase = dao.getCaseByPlate(plate)
                        if (existingCase == null) {
                            val narrative = "Plate recognized locally ($plate). Click 'Generate Report' to analyze context."
                            val newCase = CaseInfo(
                                plateNumber = plate,
                                narrative = narrative
                            )
                            dao.insertCase(newCase)
                            Log.d("HyperLPR3", "Created new Case: $plate")
                        } else {
                            dao.updateCase(existingCase.copy(updatedAt = System.currentTimeMillis()))
                            Log.d("HyperLPR3", "Updated existing Case: $plate")
                        }
                        
                        // Auto-clip video for the case
                        clipVideoForPlate(plate)
                    } else {
                        // Failure: Keep in Pending, mark as Failed
                        Log.w("HyperLPR3", "No plate detected for ID ${record.id}")
                        val updatedRecord = record.copy(
                            plateNumber = "Recognition Failed"
                        )
                        dao.update(updatedRecord)
                    }
                } catch (e: Exception) {
                    Log.e("HyperLPR3", "Processing Exception", e)
                    val updatedRecord = record.copy(
                        plateNumber = "Error: ${e.message}"
                    )
                    dao.update(updatedRecord)
                }
            }
        }
    }

    fun clipVideoForCase(plates: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            plates.forEach { plate ->
                clipVideoForPlate(plate)
            }
        }
    }

    private suspend fun clipVideoForPlate(plate: String) {
        val violations = dao.getProcessedViolationsByPlateSync(plate)
        if (violations.isEmpty()) return
        
        val minTime = violations.minOf { it.timestamp }
        val maxTime = violations.maxOf { it.timestamp }
        
        val fullTrips = dao.getFullTripRecordingsSync()
        val sourceVideo = fullTrips.find { trip ->
            // Simple heuristic: Trip started before violation and within reasonable range (e.g. 2 hours)
            trip.timestamp <= minTime && (minTime - trip.timestamp) < 7200000
        }
        
        if (sourceVideo != null) {
            // Source could be a file path or a content Uri
            // We use VideoTrimmer which now handles both via Context
            Log.d("VideoTrimmer", "Found source video: ${sourceVideo.videoPath} for plate $plate")
            
            // Create a temporary file for the trimmed video
            val tempFile = File(getApplication<Application>().cacheDir, "temp_clip_${plate}.mp4")
            if (tempFile.exists()) tempFile.delete()
            
            val duration = maxTime - minTime
            val targetDuration = 15000L // 15 seconds
            
            var startAbs: Long
            var endAbs: Long
            
            if (duration > targetDuration) {
                // Duration > 15s: Center crop
                val center = (minTime + maxTime) / 2
                startAbs = center - (targetDuration / 2)
                endAbs = center + (targetDuration / 2)
            } else {
                // Duration < 15s: Pad edges
                val padding = (targetDuration - duration) / 2
                startAbs = minTime - padding
                endAbs = maxTime + padding
            }
            
            // Convert to relative time
            var startMs = startAbs - sourceVideo.timestamp
            var endMs = endAbs - sourceVideo.timestamp
            
            // Boundary checks
            if (startMs < 0) {
                endMs += (-startMs) // Shift window right
                startMs = 0
            }
            
            Log.d("VideoTrimmer", "Trimming from ${startMs}ms to ${endMs}ms")
            
            val success = VideoTrimmer.trimVideo(
                getApplication(),
                sourceVideo.videoPath, 
                tempFile.absolutePath, 
                startMs, 
                endMs
            )
            
            if (success) {
                // Move temp file to MediaStore public directory (in case specific folder)
                val publicUri = MediaStoreUtils.saveFileToMediaStore(getApplication(), tempFile, true, plate)
                
                if (publicUri != null) {
                    val caseInfo = dao.getCaseByPlate(plate)
                    if (caseInfo != null) {
                        dao.updateCase(caseInfo.copy(videoClipPath = publicUri.toString()))
                        Log.d("VideoTrimmer", "Clipped video saved to MediaStore: $publicUri")
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(getApplication(), "Video clipped for $plate", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Cleanup temp file
                    tempFile.delete()
                } else {
                    Log.e("VideoTrimmer", "Failed to save trimmed video to MediaStore")
                }
            } else {
                Log.e("VideoTrimmer", "Trimming failed")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Video trimming failed for $plate", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.w("VideoTrimmer", "No source video found for plate $plate")
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "No source video found for $plate", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateReport(plate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val violations = dao.getProcessedViolationsByPlateSync(plate)
            if (violations.isEmpty()) return@launch

            val record = violations.first()
            val context = getApplication<Application>()
            
            // Geocoding
            var locationStr = "位置信息不可用"
            try {
                if (record.latitude != 0.0 && record.longitude != 0.0) {
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    // Suppress deprecation warning for getFromLocation
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(record.latitude, record.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        locationStr = addresses[0].getAddressLine(0)
                    }
                }
            } catch (e: Exception) {
                Log.e("Geocoder", "Failed to get location", e)
            }
            
            val timeStr = java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", java.util.Locale.CHINA).format(java.util.Date(record.timestamp))
            
            val reportContent = """
                【交通违规报告】
                车牌号码：$plate
                违规时间：$timeStr
                违规地点：$locationStr
                违规行为：该车辆被检测到非法占用应急车道行驶。
            """.trimIndent()
            
            // Save to TXT file
            try {
                val fileName = "Report_${plate}_${record.timestamp}.txt"
                val file = File(context.getExternalFilesDir(null), fileName)
                file.writeText(reportContent)
                Log.d("Report", "Saved report to ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("Report", "Failed to save report file", e)
            }

            // Update CaseInfo
            val caseInfo = dao.getCaseByPlate(plate)
            if (caseInfo != null) {
                dao.updateCase(caseInfo.copy(narrative = reportContent, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    private fun loadBitmap(context: Context, path: String): Bitmap? {
        return try {
            if (path.startsWith("content://")) {
                val uri = Uri.parse(path)
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                pfd?.use {
                    BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                }
            } else {
                BitmapFactory.decodeFile(path)
            }
        } catch (e: Exception) {
            Log.e("ViolationViewModel", "Error loading bitmap", e)
            null
        }
    }

    private fun encodeImage(path: String): String? {
        return try {
            val bitmap = loadBitmap(getApplication(), path) ?: return null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Compress to reduce size
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}

class ViolationViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViolationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViolationViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

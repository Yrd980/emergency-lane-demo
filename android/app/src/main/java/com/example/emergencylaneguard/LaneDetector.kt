package com.example.emergencylaneguard

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log

class LaneDetector {

    // Store pointer to native C++ object
    private var nativePtr: Long = 0

    external fun init(assetManager: AssetManager): Long
    external fun release(ptr: Long)
    external fun detect(ptr: Long, bitmap: Bitmap): FloatArray?

    init {
        System.loadLibrary("emergencylaneguard")
    }

    fun initialize(assetManager: AssetManager): Boolean {
        nativePtr = init(assetManager)
        Log.d("LaneDetector", "initialize: nativePtr=$nativePtr")
        return nativePtr != 0L
    }

    fun destroy() {
        if (nativePtr != 0L) {
            release(nativePtr)
            nativePtr = 0
        }
    }

    fun runDetection(bitmap: Bitmap): List<DetectionResult> {
        if (nativePtr == 0L) {
            Log.w("LaneDetector", "runDetection: nativePtr is 0, not initialized")
            return emptyList()
        }
        
        val rawResults = detect(nativePtr, bitmap)
        
        if (rawResults == null) {
            Log.w("LaneDetector", "runDetection: native detect returned null")
            return emptyList()
        }
        
        Log.d("LaneDetector", "runDetection: rawResults size=${rawResults.size}")
        
        val detections = mutableListOf<DetectionResult>()
        // Format: 6 floats per detection [x, y, w, h, score, cls]
        for (i in rawResults.indices step 6) {
            if (i + 5 < rawResults.size) {
                val det = DetectionResult(
                    rawResults[i],
                    rawResults[i+1],
                    rawResults[i+2],
                    rawResults[i+3],
                    rawResults[i+4],
                    rawResults[i+5].toInt()
                )
                detections.add(det)
                Log.d("LaneDetector", "  Detection: cls=${det.cls}, score=${det.score}, x=${det.x}, y=${det.y}")
            }
        }
        return detections
    }
}

data class DetectionResult(
    val x: Float, val y: Float, val w: Float, val h: Float,
    val score: Float, val cls: Int
)

package com.example.emergencylaneguard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private var detections: List<DetectionResult> = emptyList()
    
    // Source image size (from camera)
    var srcWidth: Int = 0
    var srcHeight: Int = 0

    fun updateDetections(results: List<DetectionResult>) {
        detections = results
        invalidate()
    }
    
    fun updateDetections(results: List<DetectionResult>, imgWidth: Int, imgHeight: Int) {
        detections = results
        srcWidth = imgWidth
        srcHeight = imgHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate scale factors
        val scaleX = if (srcWidth > 0) width.toFloat() / srcWidth else 1f
        val scaleY = if (srcHeight > 0) height.toFloat() / srcHeight else 1f
        
        // Use the smaller scale to maintain aspect ratio
        val scale = minOf(scaleX, scaleY)
        
        // Calculate offset to center the image
        val offsetX = (width - srcWidth * scale) / 2f
        val offsetY = (height - srcHeight * scale) / 2f

        for (det in detections) {
            // Scale coordinates to view size
            val left = det.x * scale + offsetX
            val top = det.y * scale + offsetY
            val right = (det.x + det.w) * scale + offsetX
            val bottom = (det.y + det.h) * scale + offsetY
            
            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)
            
            // Draw class name
            val className = getClassName(det.cls)
            canvas.drawText("$className (${String.format("%.0f", det.score * 100)}%)", left, top - 10, textPaint)
        }
    }
    
    private fun getClassName(cls: Int): String {
        return when (cls) {
            0 -> "Car"
            1 -> "Bicycle"
            2 -> "Truck"
            3 -> "Motorcycle"
            4 -> "Airplane"
            5 -> "Bus"
            6 -> "Train"
            7 -> "Boat"
            else -> "Obj$cls"
        }
    }
}

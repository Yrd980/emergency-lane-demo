package com.example.emergencylaneguard.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object MediaStoreUtils {

    fun createImageUri(context: Context, subFolder: String? = null): Uri? {
        val timestamp = System.currentTimeMillis()
        val path = if (subFolder != null) "Pictures/EmergencyLaneGuard/$subFolder" else "Pictures/EmergencyLaneGuard/pending"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$timestamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, path)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun createVideoUri(context: Context, subFolder: String? = null): Uri? {
        val timestamp = System.currentTimeMillis()
        val path = if (subFolder != null) "Movies/EmergencyLaneGuard/$subFolder" else "Movies/EmergencyLaneGuard"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "VID_$timestamp.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, path)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun saveFileToMediaStore(context: Context, file: File, isVideo: Boolean, subFolder: String? = null): Uri? {
        if (!file.exists()) return null

        val uri = if (isVideo) createVideoUri(context, subFolder) else createImageUri(context, subFolder)
        if (uri == null) return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, contentValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup if failed
            context.contentResolver.delete(uri, null, null)
            return null
        }
    }
    
    fun moveImageToFolder(context: Context, uriString: String, newSubFolder: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        
        try {
            val uri = Uri.parse(uriString)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/EmergencyLaneGuard/$newSubFolder")
            }
            context.contentResolver.update(uri, values, null, null)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    fun publishUri(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            try {
                context.contentResolver.update(uri, contentValues, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteFile(context: Context, uriString: String): Boolean {
        if (uriString.isEmpty()) return false
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "content") {
                val rows = context.contentResolver.delete(uri, null, null)
                return rows > 0
            } else {
                val file = File(uriString)
                return file.exists() && file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
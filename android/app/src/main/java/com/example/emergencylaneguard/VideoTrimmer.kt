package com.example.emergencylaneguard

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileDescriptor
import java.nio.ByteBuffer

object VideoTrimmer {
    private const val TAG = "VideoTrimmer"

    /**
     * Trims a video file from startMs to endMs.
     * Supports both File path and Content Uri (via Context).
     */
    fun trimVideo(context: Context, srcPath: String, dstPath: String, startMs: Long, endMs: Long): Boolean {
        var pfd: android.os.ParcelFileDescriptor? = null
        try {
            val extractor = MediaExtractor()
            
            if (srcPath.startsWith("content://")) {
                val uri = Uri.parse(srcPath)
                pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd == null) return false
                extractor.setDataSource(pfd.fileDescriptor)
            } else {
                val file = File(srcPath)
                if (!file.exists()) return false
                extractor.setDataSource(file.absolutePath)
            }

            val trackCount = extractor.trackCount
            val muxer = MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val indexMap = HashMap<Int, Int>(trackCount)
            var bufferSize = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true || mime?.startsWith("video/") == true) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    indexMap[i] = dstIndex
                    if (format.containsKey(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (newSize > bufferSize) bufferSize = newSize
                    }
                }
            }

            if (bufferSize < 0) bufferSize = 1024 * 1024
            
            // Set start position (seek to closest sync frame)
            // SEEK_TO_PREVIOUS_SYNC ensures we cover the requested start time
            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            muxer.start()
            
            val offsetBuffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(offsetBuffer, 0)
                
                if (bufferInfo.size < 0) {
                    break
                }
                
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                
                // Check if we passed the end time
                if (bufferInfo.presentationTimeUs > endMs * 1000) {
                    break
                }

                val trackIndex = extractor.sampleTrackIndex
                muxer.writeSampleData(indexMap[trackIndex]!!, offsetBuffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            pfd?.close()
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Trim failed", e)
            pfd?.close()
            return false
        }
    }
}

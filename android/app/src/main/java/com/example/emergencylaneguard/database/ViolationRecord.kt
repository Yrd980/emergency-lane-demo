package com.example.emergencylaneguard.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "violation_records")
data class ViolationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plateNumber: String,
    val timestamp: Long,
    val videoPath: String,
    val imagePath: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: Int = 0 
) {
    companion object {
        const val STATUS_PENDING = 0   // In PendingFragment
        const val STATUS_PROCESSED = 1 // In CasesFragment
        const val STATUS_ARCHIVED = 2  // Archived/Uploaded (Optional)
    }
}

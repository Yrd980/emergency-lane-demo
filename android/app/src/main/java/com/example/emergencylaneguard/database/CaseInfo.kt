package com.example.emergencylaneguard.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "case_info")
data class CaseInfo(
    @PrimaryKey
    val plateNumber: String,
    
    val narrative: String = "",
    val videoClipPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

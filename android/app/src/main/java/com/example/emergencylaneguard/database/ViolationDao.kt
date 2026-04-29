package com.example.emergencylaneguard.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {
    // --- ViolationRecord Operations ---
    @Query("SELECT * FROM violation_records WHERE status = 0 ORDER BY timestamp DESC")
    fun getPendingViolations(): Flow<List<ViolationRecord>>

    @Query("SELECT * FROM violation_records WHERE status > 0 ORDER BY timestamp DESC")
    fun getArchivedViolations(): Flow<List<ViolationRecord>>

    @Query("SELECT * FROM violation_records WHERE plateNumber = :plate AND status = 1 ORDER BY timestamp ASC")
    fun getProcessedViolationsByPlate(plate: String): Flow<List<ViolationRecord>>

    @Query("SELECT * FROM violation_records WHERE plateNumber = :plate AND status = 1 ORDER BY timestamp ASC")
    suspend fun getProcessedViolationsByPlateSync(plate: String): List<ViolationRecord>

    @Query("SELECT * FROM violation_records WHERE plateNumber = 'Full Trip Recording' ORDER BY timestamp DESC")
    suspend fun getFullTripRecordingsSync(): List<ViolationRecord>

    @Insert
    suspend fun insert(record: ViolationRecord): Long

    @Update
    suspend fun update(record: ViolationRecord)
    
    @Query("SELECT * FROM violation_records WHERE id = :id LIMIT 1")
    suspend fun getViolationById(id: Long): ViolationRecord?

    @Query("SELECT COUNT(*) FROM violation_records WHERE imagePath = :imagePath")
    suspend fun countByImagePath(imagePath: String): Int
    
    @Query("DELETE FROM violation_records WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM violation_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    // --- CaseInfo Operations ---
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertCase(caseInfo: CaseInfo): Long

    @Update
    suspend fun updateCase(caseInfo: CaseInfo)

    @Query("SELECT * FROM case_info ORDER BY updatedAt DESC")
    fun getAllCases(): Flow<List<CaseInfo>>

    @Query("SELECT * FROM case_info WHERE plateNumber = :plate LIMIT 1")
    fun getCaseByPlateFlow(plate: String): Flow<CaseInfo?>

    @Query("SELECT * FROM case_info WHERE plateNumber = :plate LIMIT 1")
    suspend fun getCaseByPlate(plate: String): CaseInfo?
    
    @Query("DELETE FROM case_info WHERE plateNumber = :plate")
    suspend fun deleteCaseByPlate(plate: String)
}

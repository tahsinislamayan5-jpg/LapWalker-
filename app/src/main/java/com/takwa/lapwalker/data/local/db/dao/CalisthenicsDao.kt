package com.takwa.lapwalker.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsAttemptEntity
import com.takwa.lapwalker.data.local.db.entity.CalisthenicsProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalisthenicsDao {
    @Query("SELECT * FROM calisthenics_progress")
    fun getAllProgress(): Flow<List<CalisthenicsProgressEntity>>

    @Query("SELECT * FROM calisthenics_progress WHERE stepId = :stepId")
    suspend fun getProgressForStep(stepId: String): CalisthenicsProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: CalisthenicsProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialProgress(list: List<CalisthenicsProgressEntity>)

    @Insert
    suspend fun logAttempt(attempt: CalisthenicsAttemptEntity)
}

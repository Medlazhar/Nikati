package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Grade
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {

    @Query("SELECT * FROM grades ORDER BY lastUpdated DESC")
    fun getAllGrades(): Flow<List<Grade>>

    @Query("SELECT * FROM grades WHERE classCode = :classCode AND term = :term")
    fun getGradesByClassAndTerm(classCode: String, term: Int): Flow<List<Grade>>

    @Query("SELECT * FROM grades WHERE studentId = :studentId AND term = :term LIMIT 1")
    fun getGradeForStudentFlow(studentId: String, term: Int): Flow<Grade?>

    @Query("SELECT * FROM grades WHERE studentId = :studentId AND term = :term LIMIT 1")
    suspend fun getGradeForStudent(studentId: String, term: Int): Grade?

    @Query("SELECT * FROM grades WHERE studentId = :studentId ORDER BY term ASC")
    fun getAllGradesForStudent(studentId: String): Flow<List<Grade>>

    @Query("SELECT * FROM grades WHERE isSynced = 0")
    suspend fun getUnsyncedGrades(): List<Grade>

    @Query("SELECT COUNT(*) FROM grades WHERE isSynced = 0")
    fun getUnsyncedCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGrade(grade: Grade): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGrades(grades: List<Grade>)

    @Update
    suspend fun updateGrade(grade: Grade)

    @Query("UPDATE grades SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markGradesAsSynced(ids: List<Long>)

    @Query("UPDATE grades SET classCode = :newClassCode WHERE studentId = :studentId")
    suspend fun updateStudentClassCodeInGrades(studentId: String, newClassCode: String)

    @Query("DELETE FROM grades WHERE studentId = :studentId")
    suspend fun deleteGradesForStudent(studentId: String)

    @Query("DELETE FROM grades")
    suspend fun deleteAllGrades()
}

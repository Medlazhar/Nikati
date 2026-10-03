package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClassSection
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSectionDao {

    @Query("SELECT * FROM class_sections ORDER BY level ASC, sectionNumber ASC")
    fun getAllClasses(): Flow<List<ClassSection>>

    @Query("SELECT * FROM class_sections WHERE classCode = :code LIMIT 1")
    suspend fun getClassByCode(code: String): ClassSection?

    @Query("SELECT * FROM class_sections WHERE classCode = :code LIMIT 1")
    fun getClassFlowByCode(code: String): Flow<ClassSection?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classSection: ClassSection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassSection>)

    @Update
    suspend fun updateClass(classSection: ClassSection)

    @Query("UPDATE class_sections SET studentCount = (SELECT COUNT(*) FROM students WHERE students.classCode = class_sections.classCode)")
    suspend fun updateAllStudentCounts()

    @Query("DELETE FROM class_sections WHERE classCode = :classCode")
    suspend fun deleteClassByCode(classCode: String)

    @Query("DELETE FROM class_sections")
    suspend fun deleteAllClasses()
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students ORDER BY classCode ASC, orderNumber ASC, fullName ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE classCode = :classCode ORDER BY orderNumber ASC, fullName ASC")
    fun getStudentsByClass(classCode: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id OR nationalId = :id LIMIT 1")
    suspend fun getStudentById(id: String): Student?

    @Query("SELECT * FROM students WHERE id = :query OR nationalId = :query OR fullName = :query OR TRIM(fullName) = TRIM(:query) LIMIT 1")
    suspend fun getStudentByQrContent(query: String): Student?

    @Query("SELECT * FROM students WHERE classCode = :classCode AND TRIM(fullName) = TRIM(:fullName) LIMIT 1")
    suspend fun findStudentByNameAndClass(fullName: String, classCode: String): Student?

    @Query("SELECT MAX(orderNumber) FROM students WHERE classCode = :classCode")
    suspend fun getMaxOrderNumber(classCode: String): Int?

    @Query("SELECT * FROM students WHERE id = :id OR nationalId = :id LIMIT 1")
    fun getStudentFlowById(id: String): Flow<Student?>

    @Query("SELECT * FROM students WHERE fullName LIKE '%' || :query || '%' OR nationalId LIKE '%' || :query || '%'")
    fun searchStudents(query: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Update
    suspend fun updateStudent(student: Student)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: String)

    @Query("DELETE FROM students WHERE classCode = :classCode")
    suspend fun deleteStudentsByClass(classCode: String)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}

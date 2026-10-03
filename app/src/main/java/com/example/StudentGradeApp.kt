package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.StudentRepository
import com.example.data.repository.SyncRepository
import com.example.data.sync.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class StudentGradeApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getInstance(this) }
    val networkMonitor by lazy { NetworkMonitor(this) }
    val studentRepository by lazy {
        StudentRepository(
            studentDao = database.studentDao(),
            gradeDao = database.gradeDao(),
            classSectionDao = database.classSectionDao()
        )
    }
    val syncRepository by lazy {
        SyncRepository(
            gradeDao = database.gradeDao(),
            networkMonitor = networkMonitor,
            applicationScope = applicationScope
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: StudentGradeApp
            private set
    }
}

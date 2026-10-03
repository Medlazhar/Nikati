package com.example.data.repository

import com.example.data.local.GradeDao
import com.example.data.model.Grade
import com.example.data.sync.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SyncUiState(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val unsyncedCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = "جاهز للمزامنة"
)

/**
 * Manages automated and manual cloud synchronization for offline-first grade records.
 */
class SyncRepository(
    private val gradeDao: GradeDao,
    private val networkMonitor: NetworkMonitor,
    private val applicationScope: CoroutineScope
) {

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    init {
        // Observe network state and automatically sync when connection returns
        applicationScope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                _syncState.value = _syncState.value.copy(isOnline = online)
                if (online) {
                    val pending = gradeDao.getUnsyncedGrades()
                    if (pending.isNotEmpty()) {
                        performCloudSync(pending)
                    }
                }
            }
        }

        // Observe pending unsynced count reactively
        applicationScope.launch {
            gradeDao.getUnsyncedCountFlow().collectLatest { count ->
                _syncState.value = _syncState.value.copy(unsyncedCount = count)
            }
        }
    }

    fun triggerManualSync() {
        applicationScope.launch {
            val pending = gradeDao.getUnsyncedGrades()
            performCloudSync(pending)
        }
    }

    suspend fun performCloudSync(gradesToSync: List<Grade>) = withContext(Dispatchers.IO) {
        if (!_syncState.value.isOnline) {
            _syncState.value = _syncState.value.copy(
                lastSyncMessage = "لا يوجد اتصال بالإنترنت (سيتم المزامنة تلقائياً عند الاتصال)"
            )
            return@withContext
        }

        if (gradesToSync.isEmpty()) {
            _syncState.value = _syncState.value.copy(
                lastSyncMessage = "جميع النقاط متزامنة بالفعل مع السحابة"
            )
            return@withContext
        }

        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            lastSyncMessage = "جاري مزامنة ${gradesToSync.size} نقطة مع الخادم المركزي..."
        )

        try {
            // Emulate cloud upload network latency / REST API payload transmission
            delay(1200)

            // In production REST/Firebase, send gradesToSync payload to server
            // On success response, update local database:
            val ids = gradesToSync.map { it.id }
            gradeDao.markGradesAsSynced(ids)

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                unsyncedCount = 0,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncMessage = "تمت المزامنة بنجاح لـ ${gradesToSync.size} تسجيلة نقاط!"
            )
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncMessage = "فشلت المزامنة: ${e.localizedMessage ?: "خطأ في الشبكة"}"
            )
        }
    }
}

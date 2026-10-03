package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents academic scores for a student in Algerian Middle School.
 * Follows the official grading formula of the Ministry of National Education.
 */
@Entity(
    tableName = "grades",
    indices = [
        Index(value = ["studentId", "term"], unique = true),
        Index(value = ["classCode"]),
        Index(value = ["isSynced"])
    ]
)
data class Grade(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val classCode: String,
    val term: Int = 1, // 1 = الفصل الأول, 2 = الفصل الثاني, 3 = الفصل الثالث
    val subjectName: String = "المادة",
    // Continuous assessment components (Max 20)
    val disciplineScore: Double = 0.0, // الانضباط والمواظبة (0 - 7)
    val inClassScore: Double = 0.0,    // المردود في أنشطة التعلم داخل القسم (0 - 7)
    val outClassScore: Double = 0.0,   // المردود في أنشطة التعلم خارج القسم (0 - 6)
    val continuousAssessmentTotal: Double = 0.0, // نقطة التقويم المستمر (Max 20)
    // Assignment & Exam
    val assignmentScore: Double = 0.0, // نقطة الفرض (0 - 20)
    val interimAverage: Double = 0.0,  // معدل التقويم والفرض ((continuous + assignment) / 2)
    val examScore: Double = 0.0,       // نقطة الاختبار (0 - 20)
    val finalAverage: Double = 0.0,    // المعدل النهائي للمادة (((interim * 2) + (exam * 2)) / 4)
    // Sync metadata
    val isSynced: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

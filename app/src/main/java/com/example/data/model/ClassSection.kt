package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Middle School class / section (فوج تربوي).
 * Example: sheet "2400004" -> "4م4" (4th Year, Section 4).
 */
@Entity(tableName = "class_sections")
data class ClassSection(
    @PrimaryKey val classCode: String, // e.g. "2400004"
    val sheetName: String,             // Original sheet name
    val nameArabic: String,            // e.g. "4م4"
    val nameFrench: String = "",       // e.g. "4M4"
    val level: Int,                    // 1..4 (سنة أولى..رابعة متوسط)
    val sectionNumber: Int,            // 1..20
    val studentCount: Int = 0,
    val academicYear: String = "2024-2025",
    val createdAt: Long = System.currentTimeMillis()
)

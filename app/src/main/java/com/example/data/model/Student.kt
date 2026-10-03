package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a student enrolled in an Algerian Middle School (التعليم المتوسط).
 * Imported from official digitization platform (منصة الرقمنة).
 */
@Entity(
    tableName = "students",
    indices = [
        Index(value = ["nationalId"]),
        Index(value = ["classCode"]),
        Index(value = ["fullName"])
    ]
)
data class Student(
    @PrimaryKey val id: String, // National ID or unique student registration number
    val nationalId: String,
    val fullName: String,
    val classCode: String, // Numeric code e.g. "2400004"
    val className: String, // Human-readable e.g. "4م4"
    val birthDate: String? = null,
    val gender: String? = null,
    val orderNumber: Int = 0,
    val isManualEntry: Boolean = false
) {
    /**
     * Determines QR Code payload according to digitization rule:
     * - Imported from digitization Excel file -> QR encodes student registration / national ID
     * - Added via manual entry -> QR encodes student Full Name (اسم ولقب التلميذ)
     */
    val qrCodeContent: String
        get() = if (isManualEntry || nationalId.isBlank()) fullName.trim() else nationalId.trim()
}

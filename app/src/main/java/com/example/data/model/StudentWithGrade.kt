package com.example.data.model

/**
 * Composite view model combining a student and their grade record for a given term.
 */
data class StudentWithGrade(
    val student: Student,
    val grade: Grade?
)

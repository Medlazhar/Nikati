package com.example.domain

import java.util.Locale
import kotlin.math.round

/**
 * Encapsulates the official grading rules and calculation formulas for Algerian Middle School
 * (المنشور الوزاري الخاص بحساب معدلات مواد التعليم المتوسط - وزارة التربية الوطنية).
 */
object GradeCalculator {

    const val MAX_DISCIPLINE = 7.0
    const val MAX_IN_CLASS = 7.0
    const val MAX_OUT_CLASS = 6.0
    const val MAX_CONTINUOUS = 20.0
    const val MAX_ASSIGNMENT = 20.0
    const val MAX_EXAM = 20.0

    /**
     * Continuous Assessment Total (نقطة التقويم المستمر):
     * ContinuousAssessment = DisciplineScore + InClassScore + OutClassScore (Max = 20)
     */
    fun calculateContinuousAssessment(
        discipline: Double,
        inClass: Double,
        outClass: Double
    ): Double {
        val d = discipline.coerceIn(0.0, MAX_DISCIPLINE)
        val i = inClass.coerceIn(0.0, MAX_IN_CLASS)
        val o = outClass.coerceIn(0.0, MAX_OUT_CLASS)
        return roundToTwoDecimals((d + i + o).coerceIn(0.0, MAX_CONTINUOUS))
    }

    /**
     * Mid-term Score / Interim Average (معدل التقويم والفرض):
     * InterimAverage = (ContinuousAssessment + AssignmentScore) / 2
     */
    fun calculateInterimAverage(
        continuousAssessment: Double,
        assignmentScore: Double
    ): Double {
        val c = continuousAssessment.coerceIn(0.0, MAX_CONTINUOUS)
        val a = assignmentScore.coerceIn(0.0, MAX_ASSIGNMENT)
        return roundToTwoDecimals((c + a) / 2.0)
    }

    /**
     * Final Subject Average (المعدل النهائي للمادة):
     * FinalAverage = ((InterimAverage * 2) + (ExamScore * 2)) / 4
     * (Algebraically equal to (InterimAverage + ExamScore) / 2)
     */
    fun calculateFinalAverage(
        interimAverage: Double,
        examScore: Double
    ): Double {
        val e = examScore.coerceIn(0.0, MAX_EXAM)
        val finalVal = ((interimAverage * 2.0) + (e * 2.0)) / 4.0
        return roundToTwoDecimals(finalVal.coerceIn(0.0, 20.0))
    }

    fun roundToTwoDecimals(value: Double): Double {
        return round(value * 100.0) / 100.0
    }

    fun formatGrade(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    /**
     * Official academic appreciation (ملاحظة الأستاذ حسب المعدل)
     */
    fun getAppreciation(average: Double): String {
        return when {
            average >= 18.0 -> "ممتاز (تهنئة)"
            average >= 16.0 -> "جيد جداً (لوحة شرف)"
            average >= 14.0 -> "جيد (تشجيع)"
            average >= 12.0 -> "قريب من الجيد"
            average >= 10.0 -> "مقبول"
            average >= 8.0 -> "دون المتوسط (يحتاج لمضاعفة الجهد)"
            else -> "ضعيف (إنذار بالعمل)"
        }
    }

    fun isPassing(average: Double): Boolean {
        return average >= 10.0
    }
}

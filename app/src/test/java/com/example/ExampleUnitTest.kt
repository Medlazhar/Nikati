package com.example

import com.example.domain.AlgerianClassCodeMapper
import com.example.domain.GradeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testContinuousAssessmentCalculation() {
        // Discipline (max 7) = 6.5, In-class (max 7) = 6.0, Out-class (max 6) = 5.5 -> 18.0
        val cont = GradeCalculator.calculateContinuousAssessment(6.5, 6.0, 5.5)
        assertEquals(18.0, cont, 0.01)

        // Clamping to max boundaries
        val clamped = GradeCalculator.calculateContinuousAssessment(9.0, 8.0, 10.0)
        assertEquals(20.0, clamped, 0.01)
    }

    @Test
    fun testInterimAverageCalculation() {
        // Continuous = 16.0, Assignment = 14.0 -> (16 + 14) / 2 = 15.0
        val interim = GradeCalculator.calculateInterimAverage(16.0, 14.0)
        assertEquals(15.0, interim, 0.01)
    }

    @Test
    fun testFinalSubjectAverageCalculation() {
        // Interim = 15.0, Exam = 13.0
        // FinalAverage = ((15.0 * 2) + (13.0 * 2)) / 4 = (30 + 26) / 4 = 56 / 4 = 14.0
        val finalAvg = GradeCalculator.calculateFinalAverage(15.0, 13.0)
        assertEquals(14.0, finalAvg, 0.01)
        assertTrue(GradeCalculator.isPassing(finalAvg))
        assertEquals("جيد (تشجيع)", GradeCalculator.getAppreciation(finalAvg))
    }

    @Test
    fun testAlgerianClassCodeMapping() {
        // Rule: 2400004 -> 4م4 (4M4)
        val info4M4 = AlgerianClassCodeMapper.parseSheetCode("2400004")
        assertEquals(4, info4M4.level)
        assertEquals(4, info4M4.sectionNumber)
        assertEquals("4م4", info4M4.nameArabic)

        // Rule: 2200001 -> 2م1 (2M1)
        val info2M1 = AlgerianClassCodeMapper.parseSheetCode("2200001")
        assertEquals(2, info2M1.level)
        assertEquals(1, info2M1.sectionNumber)
        assertEquals("2م1", info2M1.nameArabic)

        // Rule: 2100003 -> 1م3 (1M3)
        val info1M3 = AlgerianClassCodeMapper.parseSheetCode("2100003")
        assertEquals(1, info1M3.level)
        assertEquals(3, info1M3.sectionNumber)
        assertEquals("1م3", info1M3.nameArabic)
    }

    @Test
    fun testColumnRefToIndex() {
        assertEquals(0, com.example.domain.ExcelParser.columnRefToIndex("A1"))
        assertEquals(1, com.example.domain.ExcelParser.columnRefToIndex("B9"))
        assertEquals(2, com.example.domain.ExcelParser.columnRefToIndex("C8"))
        assertEquals(3, com.example.domain.ExcelParser.columnRefToIndex("D15"))
        assertEquals(25, com.example.domain.ExcelParser.columnRefToIndex("Z1"))
        assertEquals(26, com.example.domain.ExcelParser.columnRefToIndex("AA1"))
    }

    @Test
    fun testExcelParsingStartsFromRow9UntilLastInformationRow() {
        // Construct simulated Excel rows where:
        // Rows 1-7 are institutional headers
        // Row 8 is table column headers
        // Rows 9, 10, 11 are students
        // Row 12 is an empty row
        // Row 13 is an administrative footer
        // Row 14 is trailing blank row
        val rows = listOf(
            com.example.domain.ExcelParser.SheetRowData(1, mapOf(0 to "الجمهورية الجزائرية الديمقراطية الشعبية")),
            com.example.domain.ExcelParser.SheetRowData(2, mapOf(0 to "وزارة التربية الوطنية")),
            com.example.domain.ExcelParser.SheetRowData(3, mapOf(0 to "مديرية التربية لولاية الجزائر")),
            com.example.domain.ExcelParser.SheetRowData(4, mapOf(0 to "متوسطة العربي بن مهيدي")),
            com.example.domain.ExcelParser.SheetRowData(5, mapOf(0 to "السنة الدراسية: 2024-2025")),
            com.example.domain.ExcelParser.SheetRowData(6, mapOf(0 to "الفوج التربوي: 4م4")),
            com.example.domain.ExcelParser.SheetRowData(7, emptyMap()),
            com.example.domain.ExcelParser.SheetRowData(8, mapOf(
                0 to "الرقم",
                1 to "رقم التعريف الوطني",
                2 to "اللقب والاسم",
                3 to "تاريخ الميلاد"
            )),
            // Row 9: First real student (as requested: start reading at row 9)
            com.example.domain.ExcelParser.SheetRowData(9, mapOf(
                0 to "1",
                1 to "1040004001",
                2 to "بن عيسى ياسمين",
                3 to "2010-02-15"
            )),
            // Row 10: Second student
            com.example.domain.ExcelParser.SheetRowData(10, mapOf(
                0 to "2",
                1 to "1040004002",
                2 to "بوجمعة إسلام",
                3 to "2010-03-20"
            )),
            // Row 11: Third student
            com.example.domain.ExcelParser.SheetRowData(11, mapOf(
                0 to "3",
                1 to "1040004003",
                2 to "قادري فاطمة",
                3 to "2010-05-11"
            )),
            // Row 12: Empty row
            com.example.domain.ExcelParser.SheetRowData(12, emptyMap()),
            // Row 13: Administrative footer
            com.example.domain.ExcelParser.SheetRowData(13, mapOf(0 to "حرر بالجزائر في 2024/09/15 مدير المؤسسة")),
            // Row 14: Empty trailing row
            com.example.domain.ExcelParser.SheetRowData(14, emptyMap())
        )

        val extracted = com.example.domain.ExcelParser.extractStudentsFromRows(
            rows = rows,
            classCode = "2400004",
            className = "4م4"
        )

        // Must extract exactly the 3 students starting from row 9
        assertEquals(3, extracted.size)

        // First student from Row 9
        assertEquals("1040004001", extracted[0].nationalId)
        assertEquals("بن عيسى ياسمين", extracted[0].fullName)
        assertEquals(1, extracted[0].orderNumber)

        // Second student from Row 10
        assertEquals("1040004002", extracted[1].nationalId)
        assertEquals("بوجمعة إسلام", extracted[1].fullName)
        assertEquals(2, extracted[1].orderNumber)

        // Third student from Row 11
        assertEquals("1040004003", extracted[2].nationalId)
        assertEquals("قادري فاطمة", extracted[2].fullName)
        assertEquals(3, extracted[2].orderNumber)
    }
}

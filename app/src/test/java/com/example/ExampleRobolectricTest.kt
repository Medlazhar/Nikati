package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("نقاطي", appName)
  }

  @Test
  fun `test ExcelExporter createCsvFile and createXlsxFile`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val student = com.example.data.model.Student(
      id = "std-1",
      nationalId = "1040004001",
      fullName = "ياسمين بن عيسى",
      classCode = "2400004",
      className = "4م4",
      orderNumber = 1
    )
    val grade = com.example.data.model.Grade(
      studentId = "std-1",
      classCode = "2400004",
      term = 1,
      disciplineScore = 7.0,
      inClassScore = 7.0,
      outClassScore = 6.0,
      continuousAssessmentTotal = 20.0,
      assignmentScore = 18.0,
      interimAverage = 19.0,
      examScore = 17.0,
      finalAverage = 18.0,
      isSynced = true
    )
    val list = listOf(com.example.data.model.StudentWithGrade(student, grade))

    val csvFile = com.example.domain.ExcelExporter.createCsvFile(
      context = context,
      className = "4م4",
      term = 1,
      studentsWithGrades = list
    )
    org.junit.Assert.assertTrue(csvFile.exists())
    org.junit.Assert.assertTrue(csvFile.length() > 0)
    val csvContent = csvFile.readText(java.nio.charset.StandardCharsets.UTF_8)
    org.junit.Assert.assertTrue(csvContent.contains("نقاطي (NIKATI)"))
    org.junit.Assert.assertTrue(csvContent.contains("ياسمين بن عيسى"))
    org.junit.Assert.assertTrue(csvContent.contains("1040004001"))

    val xlsxFile = com.example.domain.ExcelExporter.createXlsxFile(
      context = context,
      className = "4م4",
      term = 1,
      studentsWithGrades = list
    )
    org.junit.Assert.assertTrue(xlsxFile.exists())
    org.junit.Assert.assertTrue(xlsxFile.length() > 0)

    csvFile.delete()
    xlsxFile.delete()
  }

  @Test
  fun `test manual student creation and QR payload from full name`() {
    val studentManual = com.example.data.model.Student(
      id = "man-1",
      nationalId = "",
      fullName = "بلقاسم يوسف",
      classCode = "2400004",
      className = "4م4",
      isManualEntry = true
    )
    assertEquals("بلقاسم يوسف", studentManual.qrCodeContent)

    val studentExcel = com.example.data.model.Student(
      id = "exc-1",
      nationalId = "1040004001",
      fullName = "ياسمين بن عيسى",
      classCode = "2400004",
      className = "4م4",
      isManualEntry = false
    )
    assertEquals("1040004001", studentExcel.qrCodeContent)
  }
}

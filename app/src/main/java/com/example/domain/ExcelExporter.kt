package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.StudentWithGrade
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Handles exporting student grade sheets to Microsoft Excel formats (.xlsx and .csv)
 * and emailing them directly using Android's Intent system and FileProvider.
 */
object ExcelExporter {

    /**
     * Generates a genuine OpenXML Excel workbook (.xlsx) containing the complete
     * official Algerian Middle School grades report for a specific class and term.
     */
    fun createXlsxFile(
        context: Context,
        className: String,
        term: Int,
        studentsWithGrades: List<StudentWithGrade>
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeClassName = className.replace(Regex("[^\\w\\u0600-\\u06FF]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(exportsDir, "نقاط_${safeClassName}_فصل${term}_$timestamp.xlsx")

        val sheetXml = buildSheetXml(className, term, studentsWithGrades)
        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="نقاط $className - فصل $term" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val wbRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            addZipEntry(zos, "[Content_Types].xml", contentTypesXml)
            addZipEntry(zos, "_rels/.rels", relsXml)
            addZipEntry(zos, "xl/workbook.xml", workbookXml)
            addZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRelsXml)
            addZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml)
        }

        return file
    }

    /**
     * Also generates a universal UTF-8 BOM CSV that opens directly in Microsoft Excel
     * on any platform without encoding artifacts.
     */
    fun createCsvFile(
        context: Context,
        className: String,
        term: Int,
        studentsWithGrades: List<StudentWithGrade>
    ): File {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeClassName = className.replace(Regex("[^\\w\\u0600-\\u06FF]"), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(exportsDir, "نقاط_${safeClassName}_فصل${term}_$timestamp.csv")

        val sb = StringBuilder()
        // UTF-8 BOM
        sb.append('\uFEFF')

        // Header info
        sb.append("الجمهورية الجزائرية الديمقراطية الشعبية - وزارة التربية الوطنية\n")
        sb.append("تطبيق نقاطي (NIKATI) - جدول رصد نقاط التعليم المتوسط\n")
        sb.append("الفوج: $className;الفصل: $term;تاريخ التصدير: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}\n\n")

        // Table Columns
        sb.append("الرقم;رقم التعريف الوطني;الاسم واللقب;الفوج;الانضباط والمواظبة (0-7);نشاط داخل القسم (0-7);نشاط خارج القسم (0-6);مجموع التقويم المستمر (ع/20);نقطة الفرض (ع/20);معدل التقويم والفرض (ع/20);نقطة الاختبار (ع/20);المعدل النهائي (ع/20);التقدير والملاحظة;حالة المزامنة\n")

        studentsWithGrades.forEach { item ->
            val s = item.student
            val g = item.grade
            if (g != null) {
                sb.append("${s.orderNumber};\"${s.nationalId}\";\"${s.fullName}\";\"${s.className}\";")
                sb.append("${GradeCalculator.formatGrade(g.disciplineScore)};")
                sb.append("${GradeCalculator.formatGrade(g.inClassScore)};")
                sb.append("${GradeCalculator.formatGrade(g.outClassScore)};")
                sb.append("${GradeCalculator.formatGrade(g.continuousAssessmentTotal)};")
                sb.append("${GradeCalculator.formatGrade(g.assignmentScore)};")
                sb.append("${GradeCalculator.formatGrade(g.interimAverage)};")
                sb.append("${GradeCalculator.formatGrade(g.examScore)};")
                sb.append("${GradeCalculator.formatGrade(g.finalAverage)};")
                sb.append("\"${GradeCalculator.getAppreciation(g.finalAverage)}\";")
                sb.append(if (g.isSynced) "متزامن" else "محلي\n")
            } else {
                sb.append("${s.orderNumber};\"${s.nationalId}\";\"${s.fullName}\";\"${s.className}\";-;-;-;-;-;-;-;-;لم ترصد;-\n")
            }
        }

        FileOutputStream(file).use { fos ->
            fos.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
        }

        return file
    }

    /**
     * Creates and dispatches an Android Intent to send the generated Excel file via email.
     */
    fun sendExcelViaEmail(
        context: Context,
        recipientEmail: String,
        className: String,
        term: Int,
        studentsWithGrades: List<StudentWithGrade>,
        useCsvFormat: Boolean = false
    ): Boolean {
        return try {
            val file = if (useCsvFormat) {
                createCsvFile(context, className, term, studentsWithGrades)
            } else {
                createXlsxFile(context, className, term, studentsWithGrades)
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val extension = if (useCsvFormat) "CSV" else "Excel (.xlsx)"
            val subject = "نقاطي (NIKATI) - كشف نقاط الفوج $className (الفصل $term)"
            val gradedCount = studentsWithGrades.count { it.grade != null }
            val totalCount = studentsWithGrades.size
            val body = """السلام عليكم ورحمة الله وبركاته،

تجدون مرفقاً طيه ملف رصد نقاط تلاميذ الفوج التربوي $className (الفصل $term) بصيغة Microsoft $extension.
تم استخراج هذا الملف وتجهيزه عبر تطبيق نقاطي (NIKATI) لأساتذة التعليم المتوسط (الرقمنة).

بيانات الملف:
• الفوج التربوي: $className
• الفصل الدراسي: الفصل $term
• إجمالي التلاميذ: $totalCount تلميذ
• التلاميذ المرصودة نقاطهم: $gradedCount تلميذ
• تاريخ وتوقيت التصدير: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())}

مع تحيات تطبيق نقاطي (NIKATI)."""

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (useCsvFormat) "text/csv" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail.trim()))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(emailIntent, "إرسال ملف النقاط Excel عبر البريد...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Shares the generated Excel file via any installed compatible app (Google Drive, WhatsApp, Telegram, etc.)
     */
    fun shareExcelFile(
        context: Context,
        className: String,
        term: Int,
        studentsWithGrades: List<StudentWithGrade>,
        useCsvFormat: Boolean = false
    ): Boolean {
        return try {
            val file = if (useCsvFormat) {
                createCsvFile(context, className, term, studentsWithGrades)
            } else {
                createXlsxFile(context, className, term, studentsWithGrades)
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (useCsvFormat) "text/csv" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_SUBJECT, "كشف نقاط $className (الفصل $term)")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة ملف Excel...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
    }

    private fun buildSheetXml(
        className: String,
        term: Int,
        studentsWithGrades: List<StudentWithGrade>
    ): String {
        val rowsXml = StringBuilder()

        // Row 1: Ministry Title
        rowsXml.append("""<row r="1"><c r="A1" t="inlineStr"><is><t>الجمهورية الجزائرية الديمقراطية الشعبية - وزارة التربية الوطنية</t></is></c></row>""")

        // Row 2: App Title
        rowsXml.append("""<row r="2"><c r="A2" t="inlineStr"><is><t>تطبيق نقاطي (NIKATI) - جدول رصد نقاط مادة التعليم المتوسط</t></is></c></row>""")

        // Row 3: Class & Term
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
        rowsXml.append("""<row r="3"><c r="A3" t="inlineStr"><is><t>الفوج التربوي: $className | الفصل: $term | تاريخ التصدير: $dateStr</t></is></c></row>""")

        // Row 5: Column Headers
        rowsXml.append("""<row r="5">
<c r="A5" t="inlineStr"><is><t>الرقم</t></is></c>
<c r="B5" t="inlineStr"><is><t>رقم التعريف الوطني</t></is></c>
<c r="C5" t="inlineStr"><is><t>الاسم واللقب</t></is></c>
<c r="D5" t="inlineStr"><is><t>الفوج</t></is></c>
<c r="E5" t="inlineStr"><is><t>الانضباط (0-7)</t></is></c>
<c r="F5" t="inlineStr"><is><t>داخل القسم (0-7)</t></is></c>
<c r="G5" t="inlineStr"><is><t>خارج القسم (0-6)</t></is></c>
<c r="H5" t="inlineStr"><is><t>التقويم المستمر (ع/20)</t></is></c>
<c r="I5" t="inlineStr"><is><t>الفرض (ع/20)</t></is></c>
<c r="J5" t="inlineStr"><is><t>معدل التقويم والفرض (ع/20)</t></is></c>
<c r="K5" t="inlineStr"><is><t>الاختبار (ع/20)</t></is></c>
<c r="L5" t="inlineStr"><is><t>المعدل النهائي (ع/20)</t></is></c>
<c r="M5" t="inlineStr"><is><t>التقدير والملاحظة</t></is></c>
<c r="N5" t="inlineStr"><is><t>حالة المزامنة</t></is></c>
</row>""")

        // Data Rows starting from row 6
        var r = 6
        studentsWithGrades.forEach { item ->
            val s = item.student
            val g = item.grade

            val disc = g?.disciplineScore ?: 0.0
            val inClass = g?.inClassScore ?: 0.0
            val outClass = g?.outClassScore ?: 0.0
            val cont = g?.continuousAssessmentTotal ?: 0.0
            val assign = g?.assignmentScore ?: 0.0
            val interim = g?.interimAverage ?: 0.0
            val exam = g?.examScore ?: 0.0
            val finalAvg = g?.finalAverage ?: 0.0
            val appreciation = if (g != null) GradeCalculator.getAppreciation(finalAvg) else "لم ترصد"
            val syncTxt = if (g?.isSynced == true) "متزامن" else "محلي"

            rowsXml.append("""<row r="$r">
<c r="A$r"><v>${s.orderNumber}</v></c>
<c r="B$r" t="inlineStr"><is><t>${escapeXml(s.nationalId)}</t></is></c>
<c r="C$r" t="inlineStr"><is><t>${escapeXml(s.fullName)}</t></is></c>
<c r="D$r" t="inlineStr"><is><t>${escapeXml(s.className)}</t></is></c>
<c r="E$r"><v>${GradeCalculator.formatGrade(disc)}</v></c>
<c r="F$r"><v>${GradeCalculator.formatGrade(inClass)}</v></c>
<c r="G$r"><v>${GradeCalculator.formatGrade(outClass)}</v></c>
<c r="H$r"><v>${GradeCalculator.formatGrade(cont)}</v></c>
<c r="I$r"><v>${GradeCalculator.formatGrade(assign)}</v></c>
<c r="J$r"><v>${GradeCalculator.formatGrade(interim)}</v></c>
<c r="K$r"><v>${GradeCalculator.formatGrade(exam)}</v></c>
<c r="L$r"><v>${GradeCalculator.formatGrade(finalAvg)}</v></c>
<c r="M$r" t="inlineStr"><is><t>${escapeXml(appreciation)}</t></is></c>
<c r="N$r" t="inlineStr"><is><t>${escapeXml(syncTxt)}</t></is></c>
</row>""")
            r++
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0" rightToLeft="1"/>
  </sheetViews>
  <sheetData>
$rowsXml
  </sheetData>
</worksheet>"""
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

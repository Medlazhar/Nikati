package com.example.domain

import android.content.Context
import android.net.Uri
import com.example.data.model.ClassSection
import com.example.data.model.Student
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * High-performance, lightweight parser for Excel (.xlsx) workbooks exported from
 * the Algerian digitization platform (منصة الرقمنة - وزارة التربية الوطنية).
 *
 * Implemented using pure Android XML Pull Parsing and ZIP streams without bloated external libraries.
 */
object ExcelParser {

    data class ImportResult(
        val classes: List<ClassSection>,
        val students: List<Student>,
        val sheetCount: Int,
        val totalStudents: Int,
        val errors: List<String> = emptyList()
    )

    /**
     * Parses an .xlsx file from a Content Uri.
     */
    fun parseXlsxUri(context: Context, uri: Uri): ImportResult {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("تعذر فتح الملف المحدد")
        return parseXlsxStream(inputStream)
    }

    /**
     * Parses an .xlsx input stream directly.
     */
    fun parseXlsxStream(inputStream: InputStream): ImportResult {
        val zipBytes = inputStream.readBytes()
        val sharedStrings = extractSharedStrings(zipBytes)
        val sheetsMetadata = extractSheetsMetadata(zipBytes)
        val sheetTargets = extractSheetTargets(zipBytes)

        val extractedClasses = mutableListOf<ClassSection>()
        val extractedStudents = mutableListOf<Student>()
        val errors = mutableListOf<String>()

        for (sheet in sheetsMetadata) {
            val targetPath = sheetTargets[sheet.rId] ?: "worksheets/sheet${sheet.sheetIndex}.xml"
            val normalizedPath = if (targetPath.startsWith("xl/")) targetPath else "xl/$targetPath"

            val sheetBytes = extractFileFromZip(zipBytes, normalizedPath)
            if (sheetBytes == null) {
                // Try direct relative match
                val altMatch = extractFileFromZip(zipBytes, "xl/worksheets/sheet${sheet.sheetIndex}.xml")
                if (altMatch == null) {
                    errors.add("لم يتم العثور على محتوى الفوج: ${sheet.sheetName}")
                    continue
                }
            }

            val actualSheetBytes = sheetBytes ?: extractFileFromZip(zipBytes, "xl/worksheets/sheet${sheet.sheetIndex}.xml")!!
            val rows = parseSheetRows(actualSheetBytes, sharedStrings)

            val parsedInfo = AlgerianClassCodeMapper.parseSheetCode(sheet.sheetName)
            val studentsFromSheet = extractStudentsFromRows(rows, parsedInfo.classCode, parsedInfo.nameArabic)

            val classSection = ClassSection(
                classCode = parsedInfo.classCode,
                sheetName = sheet.sheetName,
                nameArabic = parsedInfo.nameArabic,
                nameFrench = parsedInfo.nameFrench,
                level = parsedInfo.level,
                sectionNumber = parsedInfo.sectionNumber,
                studentCount = studentsFromSheet.size
            )

            extractedClasses.add(classSection)
            extractedStudents.addAll(studentsFromSheet)
        }

        return ImportResult(
            classes = extractedClasses,
            students = extractedStudents,
            sheetCount = extractedClasses.size,
            totalStudents = extractedStudents.size,
            errors = errors
        )
    }

    /**
     * Represents a single parsed row with its 1-based row number and 0-based column cells.
     */
    data class SheetRowData(
        val rowNumber: Int,
        val cells: Map<Int, String>
    ) {
        fun getCell(colIndex: Int): String = cells[colIndex]?.trim() ?: ""
        fun hasInformation(): Boolean = cells.values.any { it.isNotBlank() }
    }

    /**
     * Parses CSV fallback if teachers export or copy-paste CSV data.
     * In accordance with Algerian digitization format, data rows begin at row 9
     * until the last row containing information.
     */
    fun parseCsvStream(inputStream: InputStream, classCode: String, className: String): List<Student> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        val students = mutableListOf<Student>()
        if (lines.isEmpty()) return students

        // If file has 9 or more lines, begin reading at row 9 (0-indexed line 8), otherwise row 2
        val startLineIndex = if (lines.size >= 9) 8 else 1
        val lastIndex = lines.indexOfLast { it.isNotBlank() }
        if (lastIndex < startLineIndex) return students

        var order = 1
        for (i in startLineIndex..lastIndex) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            val parts = line.split(",", ";", "\t").map { it.trim().removeSurrounding("\"") }
            if (parts.size >= 2) {
                val id = parts[0]
                val name = parts[1]
                if (id.isNotBlank() && name.isNotBlank() && !isAdministrativeFooter(name) && !name.contains("رقم")) {
                    val validId = if (id.length >= 4) id else "STD-${classCode}-${order}"
                    students.add(
                        Student(
                            id = validId,
                            nationalId = validId,
                            fullName = name,
                            classCode = classCode,
                            className = className,
                            orderNumber = order
                        )
                    )
                    order++
                }
            }
        }
        return students
    }

    // --- XML Pull Parsing Internals for .xlsx (OpenXML) ---

    private data class SheetMeta(val sheetName: String, val rId: String, val sheetIndex: Int)

    private fun extractFileFromZip(zipBytes: ByteArray, targetName: String): ByteArray? {
        val zis = ZipInputStream(zipBytes.inputStream())
        var entry: ZipEntry? = zis.nextEntry
        while (entry != null) {
            val name = entry.name.replace("\\", "/")
            if (name.equals(targetName, ignoreCase = true) || name.endsWith(targetName, ignoreCase = true)) {
                return zis.readBytes()
            }
            entry = zis.nextEntry
        }
        return null
    }

    private fun extractSharedStrings(zipBytes: ByteArray): List<String> {
        val sharedXml = extractFileFromZip(zipBytes, "xl/sharedStrings.xml") ?: return emptyList()
        val strings = mutableListOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(String(sharedXml, Charsets.UTF_8)))

        var eventType = parser.eventType
        var currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t") {
                        insideT = true
                    } else if (parser.name == "si") {
                        currentText = StringBuilder()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") {
                        insideT = false
                    } else if (parser.name == "si") {
                        strings.add(currentText.toString())
                    }
                }
            }
            eventType = parser.next()
        }
        return strings
    }

    private fun extractSheetsMetadata(zipBytes: ByteArray): List<SheetMeta> {
        val workbookXml = extractFileFromZip(zipBytes, "xl/workbook.xml") ?: return emptyList()
        val sheets = mutableListOf<SheetMeta>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(String(workbookXml, Charsets.UTF_8)))

        var eventType = parser.eventType
        var index = 1

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet$index"
                var rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                if (rId == null) {
                    rId = parser.getAttributeValue(null, "r:id") ?: "rId$index"
                }
                sheets.add(SheetMeta(sheetName = name, rId = rId, sheetIndex = index))
                index++
            }
            eventType = parser.next()
        }
        return sheets
    }

    private fun extractSheetTargets(zipBytes: ByteArray): Map<String, String> {
        val relsXml = extractFileFromZip(zipBytes, "xl/_rels/workbook.xml.rels") ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(String(relsXml, Charsets.UTF_8)))

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null) {
                    map[id] = target
                }
            }
            eventType = parser.next()
        }
        return map
    }

    /**
     * Converts an Excel cell reference such as "B9", "C8", or "AA12" into a 0-based column index (A=0, B=1...).
     */
    fun columnRefToIndex(cellRef: String): Int {
        var index = 0
        var foundLetter = false
        for (ch in cellRef.uppercase()) {
            if (ch in 'A'..'Z') {
                index = index * 26 + (ch - 'A' + 1)
                foundLetter = true
            } else {
                break
            }
        }
        return if (foundLetter) (index - 1).coerceAtLeast(0) else 0
    }

    fun isAdministrativeFooter(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("مدير") ||
                trimmed.startsWith("حرر") ||
                trimmed.startsWith("تأشيرة") ||
                trimmed.startsWith("المجموع") ||
                trimmed.startsWith("ختم") ||
                trimmed.startsWith("ملاحظة") ||
                trimmed.contains("تأشيرة الإدارة") ||
                trimmed.contains("مدير المؤسسة")
    }

    private fun parseSheetRows(sheetXml: ByteArray, sharedStrings: List<String>): List<SheetRowData> {
        val rows = mutableListOf<SheetRowData>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(String(sheetXml, Charsets.UTF_8)))

        var eventType = parser.eventType
        var currentRowMap = mutableMapOf<Int, String>()
        var currentRowNum = 1
        var fallbackColIndex = 0
        var currentCellColIndex = 0
        var currentCellType: String? = null
        var cellText = StringBuilder()
        var insideV = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowMap = mutableMapOf()
                            val rAttr = parser.getAttributeValue(null, "r")
                            currentRowNum = rAttr?.toIntOrNull() ?: (if (rows.isEmpty()) 1 else rows.last().rowNumber + 1)
                            fallbackColIndex = 0
                        }
                        "c" -> {
                            val rAttr = parser.getAttributeValue(null, "r")
                            currentCellColIndex = if (!rAttr.isNullOrBlank()) {
                                columnRefToIndex(rAttr)
                            } else {
                                fallbackColIndex
                            }
                            fallbackColIndex = currentCellColIndex + 1
                            currentCellType = parser.getAttributeValue(null, "t")
                            cellText = StringBuilder()
                        }
                        "v" -> insideV = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideV) {
                        cellText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> insideV = false
                        "c" -> {
                            val rawVal = cellText.toString().trim()
                            val finalVal = if (currentCellType == "s") {
                                val idx = rawVal.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                            } else {
                                rawVal
                            }
                            if (finalVal.isNotBlank()) {
                                currentRowMap[currentCellColIndex] = finalVal
                            }
                        }
                        "row" -> {
                            if (currentRowMap.isNotEmpty()) {
                                rows.add(SheetRowData(currentRowNum, currentRowMap.toMap()))
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    /**
     * Extracts student records by reading strictly from row number 9 until the last row containing information
     * (القراءة من السطر رقم 9 حتى آخر سطر به معلومة).
     */
    fun extractStudentsFromRows(
        rows: List<SheetRowData>,
        classCode: String,
        className: String
    ): List<Student> {
        val sortedRows = rows.sortedBy { it.rowNumber }
        val students = mutableListOf<Student>()
        if (sortedRows.isEmpty()) return students

        // 1. Inspect header rows (Rows 1 to 8, especially row 8) to locate column mappings
        val headerRows = sortedRows.filter { it.rowNumber < 9 }
        var idCol = -1
        var nameCol = -1
        var firstNameCol = -1
        var lastNameCol = -1
        var birthDateCol = -1
        var orderCol = -1

        for (row in headerRows) {
            for ((colIndex, cellText) in row.cells) {
                val header = cellText.trim()
                val lower = header.lowercase()
                if (header.contains("تسجيل") || header.contains("تعريف") || header.contains("وطني") ||
                    lower.contains("nin") || lower.contains("identifiant") || lower.contains("matricule")
                ) {
                    idCol = colIndex
                } else if (header.contains("اللقب والاسم") || header.contains("الاسم واللقب") ||
                    lower.contains("nom et prénom") || lower.contains("nom & prenom")
                ) {
                    nameCol = colIndex
                } else if ((header == "اللقب" || lower == "nom" || lower.startsWith("nom ")) && !header.contains("الاسم")) {
                    lastNameCol = colIndex
                } else if ((header == "الاسم" || lower == "prénom" || lower == "prenom" || lower.startsWith("prénom ")) && !header.contains("اللقب")) {
                    firstNameCol = colIndex
                } else if (header.contains("ميلاد") || lower.contains("naissance") || lower.contains("date_naiss")) {
                    birthDateCol = colIndex
                } else if (header.contains("ترتيب") || lower == "n°" || lower == "no" || header == "ر.ت") {
                    orderCol = colIndex
                }
            }
        }

        // Default heuristic for standard official Algerian digitization sheets (منصة الرقمنة):
        // Col A (0): N° / ترتيب
        // Col B (1): رقم التعريف الوطني (NIN / Matricule)
        // Col C (2): اللقب (Nom)
        // Col D (3): الاسم (Prénom)
        // Col E (4): تاريخ الميلاد
        if (idCol == -1 && nameCol == -1 && lastNameCol == -1) {
            orderCol = 0
            idCol = 1
            lastNameCol = 2
            firstNameCol = 3
            birthDateCol = 4
        } else if (idCol == -1) {
            idCol = 1
        }

        // 2. Strict Requirement: Read starting from row 9 until the last row containing information
        // (القراءة من السطر رقم 9 حتى آخر سطر به معلومة)
        val rowsFrom9 = sortedRows.filter { it.rowNumber >= 9 }
        val lastInfoIndex = rowsFrom9.indexOfLast { it.hasInformation() }
        if (lastInfoIndex < 0) return students

        val candidateDataRows = rowsFrom9.subList(0, lastInfoIndex + 1)

        var orderCounter = 1
        for (row in candidateDataRows) {
            if (!row.hasInformation()) continue

            // Extract National ID
            var nationalId = if (idCol != -1) row.getCell(idCol) else ""
            if (nationalId.isBlank() || !nationalId.any { it.isDigit() }) {
                // Try finding any cell with 5+ digits
                val digitCell = row.cells.values.firstOrNull { it.count { c -> c.isDigit() } >= 5 }
                if (digitCell != null) {
                    nationalId = digitCell.trim()
                }
            }

            // Extract Full Name
            var fullName = when {
                nameCol != -1 && row.getCell(nameCol).isNotBlank() -> row.getCell(nameCol)
                lastNameCol != -1 && firstNameCol != -1 && (row.getCell(lastNameCol).isNotBlank() || row.getCell(firstNameCol).isNotBlank()) -> {
                    "${row.getCell(lastNameCol)} ${row.getCell(firstNameCol)}".trim()
                }
                lastNameCol != -1 && row.getCell(lastNameCol).isNotBlank() -> row.getCell(lastNameCol)
                else -> ""
            }

            // Fallback for name if specific columns were blank
            if (fullName.isBlank()) {
                val candidateName = row.cells.entries
                    .filter { it.key != idCol && it.key != orderCol }
                    .map { it.value.trim() }
                    .firstOrNull { it.length >= 2 && !it.all { c -> c.isDigit() } && !isAdministrativeFooter(it) }
                if (candidateName != null) {
                    fullName = candidateName
                }
            }

            // Skip administrative footer rows (e.g. "مدير المؤسسة", "حرر بـ", "تأشيرة")
            if (isAdministrativeFooter(fullName) || fullName.contains("مدير") || fullName.contains("المجموع")) {
                continue
            }

            // Extract Birth Date
            val birthDate = if (birthDateCol != -1) row.getCell(birthDateCol).takeIf { it.isNotBlank() } else null

            // Extract Order Number
            val parsedOrder = if (orderCol != -1) row.getCell(orderCol).toIntOrNull() else null
            val finalOrder = parsedOrder ?: orderCounter

            if (fullName.isNotBlank()) {
                val finalId = if (nationalId.isNotBlank()) nationalId else "STD-${classCode}-${finalOrder}"
                students.add(
                    Student(
                        id = finalId,
                        nationalId = finalId,
                        fullName = fullName,
                        classCode = classCode,
                        className = className,
                        birthDate = birthDate,
                        orderNumber = finalOrder
                    )
                )
                orderCounter++
            }
        }

        return students
    }

    /**
     * Generates comprehensive authentic sample Algerian middle school data
     * (منصة الرقمنة - وزارة التربية الوطنية) for testing and immediate offline exploration.
     */
    fun generateSampleAlgerianData(): Pair<List<ClassSection>, List<Student>> {
        val sampleClasses = listOf(
            ClassSection(
                classCode = "2400004",
                sheetName = "2400004",
                nameArabic = "4م4",
                nameFrench = "4M4",
                level = 4,
                sectionNumber = 4,
                studentCount = 8
            ),
            ClassSection(
                classCode = "2200001",
                sheetName = "2200001",
                nameArabic = "2م1",
                nameFrench = "2M1",
                level = 2,
                sectionNumber = 1,
                studentCount = 8
            ),
            ClassSection(
                classCode = "2100003",
                sheetName = "2100003",
                nameArabic = "1م3",
                nameFrench = "1M3",
                level = 1,
                sectionNumber = 3,
                studentCount = 8
            )
        )

        val names4M4 = listOf(
            "بن عيسى ياسمين",
            "بوجمعة إسلام",
            "قادري فاطمة الزهراء",
            "محي الدين عبد الرحمان",
            "بلقاسم أمين",
            "منصوري سيرين",
            "دحماني أيوب",
            "زرواقي هديل"
        )

        val names2M1 = listOf(
            "عمراني إلياس",
            "شريف نور الهدى",
            "سليماني محمد أنيس",
            "براهيمي مريم",
            "طاهري زكرياء",
            "حركات رانية",
            "جباري وسيم",
            "مزيان أسامة"
        )

        val names1M3 = listOf(
            "بلحاج يوسف",
            "بن ناصر إيناس",
            "رحماني عبد القادر",
            "حمادي خديجة",
            "موساوي ريان",
            "لعريبي آية",
            "بوزيد صهيب",
            "عماري تسنيم"
        )

        val sampleStudents = mutableListOf<Student>()

        names4M4.forEachIndexed { idx, name ->
            val order = idx + 1
            val id = "104000${4000 + order}"
            sampleStudents.add(
                Student(
                    id = id,
                    nationalId = id,
                    fullName = name,
                    classCode = "2400004",
                    className = "4م4",
                    birthDate = "2010-0${(order % 9) + 1}-15",
                    orderNumber = order
                )
            )
        }

        names2M1.forEachIndexed { idx, name ->
            val order = idx + 1
            val id = "102000${1000 + order}"
            sampleStudents.add(
                Student(
                    id = id,
                    nationalId = id,
                    fullName = name,
                    classCode = "2200001",
                    className = "2م1",
                    birthDate = "2012-0${(order % 9) + 1}-10",
                    orderNumber = order
                )
            )
        }

        names1M3.forEachIndexed { idx, name ->
            val order = idx + 1
            val id = "101000${3000 + order}"
            sampleStudents.add(
                Student(
                    id = id,
                    nationalId = id,
                    fullName = name,
                    classCode = "2100003",
                    className = "1م3",
                    birthDate = "2013-0${(order % 9) + 1}-20",
                    orderNumber = order
                )
            )
        }

        return Pair(sampleClasses, sampleStudents)
    }
}

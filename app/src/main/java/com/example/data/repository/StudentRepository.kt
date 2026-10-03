package com.example.data.repository

import com.example.data.local.ClassSectionDao
import com.example.data.local.GradeDao
import com.example.data.local.StudentDao
import com.example.data.model.ClassSection
import com.example.data.model.Grade
import com.example.data.model.Student
import com.example.data.model.StudentWithGrade
import com.example.domain.AlgerianClassCodeMapper
import com.example.domain.ExcelParser
import com.example.domain.GradeCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Main repository managing students, classes, and grade calculations.
 */
class StudentRepository(
    private val studentDao: StudentDao,
    private val gradeDao: GradeDao,
    private val classSectionDao: ClassSectionDao
) {

    val allClasses: Flow<List<ClassSection>> = classSectionDao.getAllClasses()
    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val unsyncedCount: Flow<Int> = gradeDao.getUnsyncedCountFlow()

    fun getStudentsByClass(classCode: String): Flow<List<Student>> {
        return studentDao.getStudentsByClass(classCode)
    }

    suspend fun getStudentById(id: String): Student? {
        return studentDao.getStudentById(id)
    }

    suspend fun getStudentByQrContent(query: String): Student? {
        val clean = query.trim()
        return studentDao.getStudentByQrContent(clean)
    }

    suspend fun addStudentManually(
        fullName: String,
        className: String,
        customOrderNumber: Int? = null
    ): Student {
        val cleanName = fullName.trim()
        val cleanClassName = className.trim()

        val parsed = AlgerianClassCodeMapper.parseSheetCode(cleanClassName)
        val generatedClassCode = if (parsed.classCode.isNotBlank()) parsed.classCode else "2${parsed.level}0000${parsed.sectionNumber}"
        val classArabicName = if (parsed.nameArabic.isNotBlank()) parsed.nameArabic else cleanClassName

        // Ensure ClassSection exists
        val existingClass = classSectionDao.getClassByCode(generatedClassCode)
        if (existingClass == null) {
            val newClass = ClassSection(
                classCode = generatedClassCode,
                sheetName = cleanClassName,
                nameArabic = classArabicName,
                nameFrench = parsed.nameFrench,
                level = parsed.level,
                sectionNumber = parsed.sectionNumber,
                studentCount = 0
            )
            classSectionDao.insertClass(newClass)
        }

        val nextOrder = customOrderNumber ?: ((studentDao.getMaxOrderNumber(generatedClassCode) ?: 0) + 1)
        val manualStudentId = "MAN_" + System.currentTimeMillis()

        val student = Student(
            id = manualStudentId,
            nationalId = "",
            fullName = cleanName,
            classCode = generatedClassCode,
            className = classArabicName,
            birthDate = null,
            gender = null,
            orderNumber = nextOrder,
            isManualEntry = true
        )

        studentDao.insertStudent(student)
        classSectionDao.updateAllStudentCounts()
        return student
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
        gradeDao.updateStudentClassCodeInGrades(student.id, student.classCode)
        classSectionDao.updateAllStudentCounts()
    }

    fun getStudentFlowById(id: String): Flow<Student?> {
        return studentDao.getStudentFlowById(id)
    }

    fun searchStudents(query: String): Flow<List<Student>> {
        return studentDao.searchStudents(query)
    }

    fun getGradeForStudent(studentId: String, term: Int): Flow<Grade?> {
        return gradeDao.getGradeForStudentFlow(studentId, term)
    }

    suspend fun getGradeForStudentSync(studentId: String, term: Int): Grade? {
        return gradeDao.getGradeForStudent(studentId, term)
    }

    fun getStudentsWithGrades(classCode: String, term: Int): Flow<List<StudentWithGrade>> {
        val studentsFlow = studentDao.getStudentsByClass(classCode)
        val gradesFlow = gradeDao.getGradesByClassAndTerm(classCode, term)

        return combine(studentsFlow, gradesFlow) { students, grades ->
            val gradesMap = grades.associateBy { it.studentId }
            students.map { student ->
                StudentWithGrade(
                    student = student,
                    grade = gradesMap[student.id]
                )
            }
        }
    }

    suspend fun saveGradeWithCalculations(
        studentId: String,
        classCode: String,
        term: Int,
        disciplineScore: Double,
        inClassScore: Double,
        outClassScore: Double,
        assignmentScore: Double,
        examScore: Double,
        subjectName: String = "المادة"
    ): Grade {
        val continuousTotal = GradeCalculator.calculateContinuousAssessment(
            discipline = disciplineScore,
            inClass = inClassScore,
            outClass = outClassScore
        )

        val interimAvg = GradeCalculator.calculateInterimAverage(
            continuousAssessment = continuousTotal,
            assignmentScore = assignmentScore
        )

        val finalAvg = GradeCalculator.calculateFinalAverage(
            interimAverage = interimAvg,
            examScore = examScore
        )

        val existing = gradeDao.getGradeForStudent(studentId, term)
        val gradeToSave = Grade(
            id = existing?.id ?: 0,
            studentId = studentId,
            classCode = classCode,
            term = term,
            subjectName = subjectName,
            disciplineScore = disciplineScore,
            inClassScore = inClassScore,
            outClassScore = outClassScore,
            continuousAssessmentTotal = continuousTotal,
            assignmentScore = assignmentScore,
            interimAverage = interimAvg,
            examScore = examScore,
            finalAverage = finalAvg,
            isSynced = false,
            lastUpdated = System.currentTimeMillis()
        )

        gradeDao.insertOrUpdateGrade(gradeToSave)
        return gradeToSave
    }

    suspend fun importParsedData(classes: List<ClassSection>, students: List<Student>) {
        classSectionDao.insertClasses(classes)
        studentDao.insertStudents(students)
        classSectionDao.updateAllStudentCounts()
    }

    suspend fun insertStudent(student: Student) {
        studentDao.insertStudent(student)
        classSectionDao.updateAllStudentCounts()
    }

    suspend fun deleteStudent(studentId: String) {
        studentDao.deleteStudentById(studentId)
        gradeDao.deleteGradesForStudent(studentId)
        classSectionDao.updateAllStudentCounts()
    }

    suspend fun deleteClass(classCode: String) {
        studentDao.deleteStudentsByClass(classCode)
        classSectionDao.deleteClassByCode(classCode)
    }

    suspend fun clearAllData() {
        studentDao.deleteAllStudents()
        gradeDao.deleteAllGrades()
        classSectionDao.deleteAllClasses()
    }

    suspend fun loadAlgerianSampleData() {
        val (classes, students) = ExcelParser.generateSampleAlgerianData()
        importParsedData(classes, students)

        // Seed realistic sample grades for students
        students.take(12).forEach { student ->
            saveGradeWithCalculations(
                studentId = student.id,
                classCode = student.classCode,
                term = 1,
                disciplineScore = 6.0 + (student.orderNumber % 2),
                inClassScore = 5.5 + (student.orderNumber % 2),
                outClassScore = 5.0,
                assignmentScore = 14.0 + (student.orderNumber % 5),
                examScore = 13.5 + (student.orderNumber % 6),
                subjectName = "الرياضيات"
            )
        }
    }
}

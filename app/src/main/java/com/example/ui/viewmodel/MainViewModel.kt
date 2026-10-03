package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ClassSection
import com.example.data.model.Grade
import com.example.data.model.Student
import com.example.data.model.StudentWithGrade
import com.example.data.repository.StudentRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.SyncUiState
import com.example.domain.AlgerianClassCodeMapper
import com.example.domain.ExcelParser
import com.example.domain.GradeCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    WELCOME,
    CLASSES,
    GRADE_ENTRY,
    SCANNER,
    PRINT_BADGES,
    IMPORT_EXCEL,
    EXPORT_REPORTS,
    MANUAL_ENTRY
}

data class GradeFormState(
    val student: Student? = null,
    val term: Int = 1,
    val disciplineScore: Double = 0.0,
    val inClassScore: Double = 0.0,
    val outClassScore: Double = 0.0,
    val continuousAssessmentTotal: Double = 0.0,
    val assignmentScore: Double = 0.0,
    val interimAverage: Double = 0.0,
    val examScore: Double = 0.0,
    val finalAverage: Double = 0.0,
    val isSaved: Boolean = false
)

class MainViewModel(
    private val studentRepository: StudentRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(ScreenTab.WELCOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    private val _selectedClassCode = MutableStateFlow<String?>(null)
    val selectedClassCode: StateFlow<String?> = _selectedClassCode.asStateFlow()

    private val _selectedTerm = MutableStateFlow(1)
    val selectedTerm: StateFlow<Int> = _selectedTerm.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _gradeFormState = MutableStateFlow(GradeFormState())
    val gradeFormState: StateFlow<GradeFormState> = _gradeFormState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    val allClasses: StateFlow<List<ClassSection>> = studentRepository.allClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncUiState> = syncRepository.syncState

    val studentsInSelectedClass: StateFlow<List<StudentWithGrade>> = combine(
        _selectedClassCode,
        _selectedTerm
    ) { code, term ->
        Pair(code, term)
    }.flatMapLatest { (code, term) ->
        if (code != null) {
            studentRepository.getStudentsWithGrades(code, term)
        } else {
            MutableStateFlow(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<List<Student>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            studentRepository.allStudents
        } else {
            studentRepository.searchStudents(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto-select first class when list loads if none selected
        viewModelScope.launch {
            allClasses.collect { list ->
                if (_selectedClassCode.value == null && list.isNotEmpty()) {
                    _selectedClassCode.value = list.first().classCode
                }
            }
        }
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun selectClass(classCode: String) {
        _selectedClassCode.value = classCode
    }

    fun selectTerm(term: Int) {
        _selectedTerm.value = term
        // Update current grade form if student is selected
        _gradeFormState.value.student?.let { student ->
            loadStudentForGrading(student, term)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadStudentForGrading(student: Student, term: Int = _selectedTerm.value) {
        viewModelScope.launch {
            val existingGrade = studentRepository.getGradeForStudentSync(student.id, term)
            val discipline = existingGrade?.disciplineScore ?: 0.0
            val inClass = existingGrade?.inClassScore ?: 0.0
            val outClass = existingGrade?.outClassScore ?: 0.0
            val assignment = existingGrade?.assignmentScore ?: 0.0
            val exam = existingGrade?.examScore ?: 0.0

            val contTotal = GradeCalculator.calculateContinuousAssessment(discipline, inClass, outClass)
            val interim = GradeCalculator.calculateInterimAverage(contTotal, assignment)
            val finalAvg = GradeCalculator.calculateFinalAverage(interim, exam)

            _gradeFormState.value = GradeFormState(
                student = student,
                term = term,
                disciplineScore = discipline,
                inClassScore = inClass,
                outClassScore = outClass,
                continuousAssessmentTotal = contTotal,
                assignmentScore = assignment,
                interimAverage = interim,
                examScore = exam,
                finalAverage = finalAvg,
                isSaved = existingGrade != null
            )
            _currentTab.value = ScreenTab.GRADE_ENTRY
        }
    }

    fun onQrCodeScanned(scannedId: String) {
        viewModelScope.launch {
            val clean = scannedId.trim()
            val student = studentRepository.getStudentByQrContent(clean)
                ?: studentRepository.getStudentById(clean)
            if (student != null) {
                loadStudentForGrading(student, _selectedTerm.value)
                _uiEvents.emit("تم التعرف على التلميذ: ${student.fullName}")
            } else {
                _uiEvents.emit("لم يتم العثور على تلميذ بالرمز: $clean")
            }
        }
    }

    fun addStudentManually(
        fullName: String,
        className: String,
        customOrderNumber: Int? = null,
        onSuccess: (Student) -> Unit = {}
    ) {
        viewModelScope.launch {
            val trimmedName = fullName.trim()
            val trimmedClass = className.trim()
            if (trimmedName.isBlank() || trimmedClass.isBlank()) {
                _uiEvents.emit("يرجى إدخال اسم ولقب التلميذ والقسم بشكل صحيح")
                return@launch
            }

            try {
                val student = studentRepository.addStudentManually(
                    fullName = trimmedName,
                    className = trimmedClass,
                    customOrderNumber = customOrderNumber
                )
                _selectedClassCode.value = student.classCode
                _uiEvents.emit("تمت إضافة التلميذ ${student.fullName} بنجاح! تم إنشاء رمز QR باسمه.")
                onSuccess(student)
            } catch (e: Exception) {
                _uiEvents.emit("تعذر إضافة التلميذ: ${e.localizedMessage}")
            }
        }
    }

    fun updateStudentDetails(
        studentId: String,
        newName: String,
        newClassName: String,
        newNationalId: String,
        newOrderNumber: Int
    ) {
        viewModelScope.launch {
            val currentStudent = _gradeFormState.value.student
            if (currentStudent == null || currentStudent.id != studentId) return@launch
            val cleanName = newName.trim()
            val cleanClass = newClassName.trim()
            if (cleanName.isBlank() || cleanClass.isBlank()) {
                _uiEvents.emit("يرجى إدخال اسم ولقب وقسم صالحين")
                return@launch
            }

            val parsed = AlgerianClassCodeMapper.parseSheetCode(cleanClass)
            val generatedClassCode = if (parsed.classCode.isNotBlank()) parsed.classCode else "2${parsed.level}0000${parsed.sectionNumber}"
            val classArabicName = if (parsed.nameArabic.isNotBlank()) parsed.nameArabic else cleanClass

            val updated = currentStudent.copy(
                fullName = cleanName,
                className = classArabicName,
                classCode = generatedClassCode,
                nationalId = newNationalId.trim(),
                orderNumber = newOrderNumber
            )

            try {
                studentRepository.updateStudent(updated)
                _gradeFormState.value = _gradeFormState.value.copy(student = updated)
                _uiEvents.emit("تم تعديل وتحديث بيانات التلميذ بنجاح")
            } catch (e: Exception) {
                _uiEvents.emit("خطأ أثناء تحديث بيانات التلميذ: ${e.localizedMessage}")
            }
        }
    }

    fun updateDisciplineScore(score: Double) {
        val current = _gradeFormState.value
        val clamped = score.coerceIn(0.0, GradeCalculator.MAX_DISCIPLINE)
        recalculateForm(current.copy(disciplineScore = clamped))
    }

    fun updateInClassScore(score: Double) {
        val current = _gradeFormState.value
        val clamped = score.coerceIn(0.0, GradeCalculator.MAX_IN_CLASS)
        recalculateForm(current.copy(inClassScore = clamped))
    }

    fun updateOutClassScore(score: Double) {
        val current = _gradeFormState.value
        val clamped = score.coerceIn(0.0, GradeCalculator.MAX_OUT_CLASS)
        recalculateForm(current.copy(outClassScore = clamped))
    }

    fun updateAssignmentScore(score: Double) {
        val current = _gradeFormState.value
        val clamped = score.coerceIn(0.0, GradeCalculator.MAX_ASSIGNMENT)
        recalculateForm(current.copy(assignmentScore = clamped))
    }

    fun updateExamScore(score: Double) {
        val current = _gradeFormState.value
        val clamped = score.coerceIn(0.0, GradeCalculator.MAX_EXAM)
        recalculateForm(current.copy(examScore = clamped))
    }

    private fun recalculateForm(draft: GradeFormState) {
        val contTotal = GradeCalculator.calculateContinuousAssessment(
            discipline = draft.disciplineScore,
            inClass = draft.inClassScore,
            outClass = draft.outClassScore
        )
        val interim = GradeCalculator.calculateInterimAverage(
            continuousAssessment = contTotal,
            assignmentScore = draft.assignmentScore
        )
        val finalAvg = GradeCalculator.calculateFinalAverage(
            interimAverage = interim,
            examScore = draft.examScore
        )

        _gradeFormState.value = draft.copy(
            continuousAssessmentTotal = contTotal,
            interimAverage = interim,
            finalAverage = finalAvg,
            isSaved = false
        )
    }

    fun saveCurrentGrade() {
        val current = _gradeFormState.value
        val student = current.student ?: return

        viewModelScope.launch {
            studentRepository.saveGradeWithCalculations(
                studentId = student.id,
                classCode = student.classCode,
                term = current.term,
                disciplineScore = current.disciplineScore,
                inClassScore = current.inClassScore,
                outClassScore = current.outClassScore,
                assignmentScore = current.assignmentScore,
                examScore = current.examScore
            )
            _gradeFormState.value = current.copy(isSaved = true)
            _uiEvents.emit("تم حفظ وتحديث معدل ${student.fullName} بنجاح!")
        }
    }

    fun navigateToAdjacentStudent(direction: Int) {
        val currentStudent = _gradeFormState.value.student ?: return
        val list = studentsInSelectedClass.value.map { it.student }
        val currentIndex = list.indexOfFirst { it.id == currentStudent.id }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + direction).coerceIn(0, list.lastIndex)
            if (nextIndex != currentIndex) {
                saveCurrentGrade()
                loadStudentForGrading(list[nextIndex], _gradeFormState.value.term)
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            studentRepository.loadAlgerianSampleData()
            _uiEvents.emit("تم تحميل 3 أفواج نموذجية من الرقمنة بنجاح!")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            studentRepository.clearAllData()
            _gradeFormState.value = GradeFormState()
            _uiEvents.emit("تم مسح كافة البيانات المسجلة")
        }
    }

    fun triggerCloudSync() {
        syncRepository.triggerManualSync()
    }

    private val _recipientEmail = MutableStateFlow(
        com.example.StudentGradeApp.instance.getSharedPreferences("nikati_prefs", android.content.Context.MODE_PRIVATE)
            .getString("teacher_email", "") ?: ""
    )
    val recipientEmail: StateFlow<String> = _recipientEmail.asStateFlow()

    fun updateRecipientEmail(email: String) {
        _recipientEmail.value = email
        com.example.StudentGradeApp.instance.getSharedPreferences("nikati_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("teacher_email", email.trim())
            .apply()
    }

    fun exportAndEmailExcel(
        context: android.content.Context,
        recipientEmail: String,
        className: String,
        term: Int,
        studentsWithGrades: List<StudentWithGrade>,
        useCsvFormat: Boolean = false
    ) {
        if (recipientEmail.isNotBlank()) {
            updateRecipientEmail(recipientEmail)
        }
        val success = com.example.domain.ExcelExporter.sendExcelViaEmail(
            context = context,
            recipientEmail = recipientEmail,
            className = className,
            term = term,
            studentsWithGrades = studentsWithGrades,
            useCsvFormat = useCsvFormat
        )
        viewModelScope.launch {
            if (success) {
                _uiEvents.emit("تم تجهيز كشف النقاط Excel وفتح تطبيق البريد")
            } else {
                _uiEvents.emit("تعذر إرسال الملف. يرجى التأكد من توفر تطبيق بريد")
            }
        }
    }

    fun exportAndShareExcel(
        context: android.content.Context,
        className: String,
        term: Int,
        studentsWithGrades: List<StudentWithGrade>,
        useCsvFormat: Boolean = false
    ) {
        val success = com.example.domain.ExcelExporter.shareExcelFile(
            context = context,
            className = className,
            term = term,
            studentsWithGrades = studentsWithGrades,
            useCsvFormat = useCsvFormat
        )
        viewModelScope.launch {
            if (success) {
                _uiEvents.emit("تم تجهيز كشف النقاط للمشاركة والحفظ")
            } else {
                _uiEvents.emit("تعذر تجهيز الملف للمشاركة")
            }
        }
    }
}

class MainViewModelFactory(
    private val studentRepository: StudentRepository,
    private val syncRepository: SyncRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(studentRepository, syncRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

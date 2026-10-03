package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.EmailExcelDialog
import com.example.ui.components.StudentQrBadgeDialog
import com.example.ui.theme.AlgerianGreenDark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Student
import com.example.data.model.StudentWithGrade
import com.example.domain.GradeCalculator
import com.example.domain.QrCodeHelper
import com.example.ui.theme.GradeDanger
import com.example.ui.theme.GradeSuccess
import com.example.ui.theme.GradeWarning
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

@Composable
fun ClassesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val classes by viewModel.allClasses.collectAsStateWithLifecycle()
    val selectedClassCode by viewModel.selectedClassCode.collectAsStateWithLifecycle()
    val selectedTerm by viewModel.selectedTerm.collectAsStateWithLifecycle()
    val studentsWithGrades by viewModel.studentsInSelectedClass.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var studentForQrDialog by remember { mutableStateOf<Student?>(null) }

    val currentSelectedClass = remember(classes, selectedClassCode) {
        classes.find { it.classCode == selectedClassCode } ?: classes.firstOrNull()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top App Bar Header
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "نقاطي (NIKATI)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "منظومة الرقمنة - التعليم المتوسط الجزائري",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.selectTab(ScreenTab.SCANNER) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("quick_scan_button")
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = "مسح سريع")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مسح QR", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

        // Term Selector (الفصل الأول / الثاني / الثالث)
        TabRow(
            selectedTabIndex = selectedTerm - 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            listOf("الفصل الأول", "الفصل الثاني", "الفصل الثالث").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTerm == index + 1,
                    onClick = { viewModel.selectTerm(index + 1) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTerm == index + 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("term_tab_${index + 1}")
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("بحث عن تلميذ بالاسم أو رقم التعريف الوطني...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_student_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Class Selection Chips Row
        if (classes.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(classes) { classSection ->
                    val isSelected = classSection.classCode == selectedClassCode
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectClass(classSection.classCode) },
                        label = {
                            Text(
                                text = "${classSection.nameArabic} (${classSection.studentCount})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("class_chip_${classSection.classCode}")
                    )
                }
            }

            // Quick Class Action Banner: Send Excel to Email
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val gradedInClass = studentsWithGrades.count { it.grade != null }
                    val totalInClass = studentsWithGrades.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AlgerianGreenDark.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = AlgerianGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${currentSelectedClass?.nameArabic ?: ""} - الفصل $selectedTerm",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "النقاط المرصودة: $gradedInClass من $totalInClass",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Main Content: Student List or Empty State
        if (classes.isEmpty()) {
            EmptyDatabasePrompt(
                onGoToImport = { viewModel.selectTab(ScreenTab.IMPORT_EXCEL) },
                onGoToManualEntry = { viewModel.selectTab(ScreenTab.MANUAL_ENTRY) }
            )
        } else {
            val filteredList = if (searchQuery.isBlank()) {
                studentsWithGrades
            } else {
                studentsWithGrades.filter {
                    it.student.fullName.contains(searchQuery, ignoreCase = true) ||
                            it.student.nationalId.contains(searchQuery)
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد تلاميذ يطابقون البحث في هذا الفوج",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.student.id }) { item ->
                        StudentGradeCard(
                            item = item,
                            term = selectedTerm,
                            onGradeClick = { viewModel.loadStudentForGrading(item.student, selectedTerm) },
                            onShowQr = { studentForQrDialog = item.student }
                        )
                    }
                }
            }
        }
    }

    // QR Badge Dialog
    studentForQrDialog?.let { student ->
        StudentQrBadgeDialog(
            student = student,
            onDismiss = { studentForQrDialog = null },
            onGradeClick = {
                studentForQrDialog = null
                viewModel.loadStudentForGrading(student, selectedTerm)
            }
        )
    }

    // Email Excel Sheet Dialog
    if (showEmailExcelDialog) {
        EmailExcelDialog(
            className = currentSelectedClass?.nameArabic ?: "الفوج الحالي",
            term = selectedTerm,
            studentsWithGrades = studentsWithGrades,
            initialEmail = recipientEmail,
            onDismiss = { showEmailExcelDialog = false },
            onSendEmail = { email, useCsv ->
                viewModel.exportAndEmailExcel(
                    context = context,
                    recipientEmail = email,
                    className = currentSelectedClass?.nameArabic ?: "الفوج",
                    term = selectedTerm,
                    studentsWithGrades = studentsWithGrades,
                    useCsvFormat = useCsv
                )
            },
            onShareFile = { useCsv ->
                viewModel.exportAndShareExcel(
                    context = context,
                    className = currentSelectedClass?.nameArabic ?: "الفوج",
                    term = selectedTerm,
                    studentsWithGrades = studentsWithGrades,
                    useCsvFormat = useCsv
                )
            }
        )
    }
}

@Composable
private fun StudentGradeCard(
    item: StudentWithGrade,
    term: Int,
    onGradeClick: () -> Unit,
    onShowQr: () -> Unit
) {
    val student = item.student
    val grade = item.grade

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGradeClick() }
            .testTag("student_card_${student.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Order number avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${student.orderNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "الرقم: ${student.nationalId} | الفوج: ${student.className}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // QR button
                IconButton(
                    onClick = onShowQr,
                    modifier = Modifier.testTag("student_qr_btn_${student.id}")
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "عرض رمز QR",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grade Summary Row
            if (grade != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreColumn(
                        label = "التقويم",
                        value = GradeCalculator.formatGrade(grade.continuousAssessmentTotal),
                        maxValue = "20"
                    )
                    ScoreColumn(
                        label = "الفرض",
                        value = GradeCalculator.formatGrade(grade.assignmentScore),
                        maxValue = "20"
                    )
                    ScoreColumn(
                        label = "الاختبار",
                        value = GradeCalculator.formatGrade(grade.examScore),
                        maxValue = "20"
                    )

                    // Final Average pill
                    val isPass = grade.finalAverage >= 10.0
                    val avgColor = if (isPass) GradeSuccess else GradeDanger

                    Surface(
                        color = avgColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "المعدل النهائي",
                                style = MaterialTheme.typography.labelSmall,
                                color = avgColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${GradeCalculator.formatGrade(grade.finalAverage)}/20",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = avgColor
                            )
                        }
                    }
                }

                // Sync indicator & appreciation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = GradeCalculator.getAppreciation(grade.finalAverage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (grade.isSynced) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "متزامن",
                                tint = GradeSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "متزامن",
                                style = MaterialTheme.typography.labelSmall,
                                color = GradeSuccess
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = "معلق المزامنة",
                                tint = GradeWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "محلي (بانتظار المزامنة)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GradeWarning
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "لم يتم رصد نقاط الفصل $term بعد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onGradeClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رصد الآن", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreColumn(
    label: String,
    value: String,
    maxValue: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value/$maxValue",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyDatabasePrompt(
    onGoToImport: () -> Unit,
    onGoToManualEntry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "مرحباً بك في تطبيق نقاطي (NIKATI)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لم يتم تسجيل أي أفواج أو تلاميذ بعد. يمكنك البدء بالإدخال اليدوي للتلاميذ أو استيراد ملف Excel (.xlsx) مباشرة من منصة الرقمنة.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onGoToManualEntry,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("go_to_manual_entry_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("الإدخال اليدوي للتلاميذ (الاسم، القسم، وQR)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onGoToImport,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("go_to_import_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استيراد ملف Excel من الرقمنة", fontWeight = FontWeight.Bold)
            }
        }
    }
}

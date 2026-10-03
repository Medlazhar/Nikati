package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.EmailExcelDialog
import com.example.ui.theme.AlgerianGreenDark
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.GradeCalculator
import com.example.ui.theme.GradeDanger
import com.example.ui.theme.GradeHonor
import com.example.ui.theme.GradeSuccess
import com.example.ui.theme.GradeWarning
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val classes by viewModel.allClasses.collectAsStateWithLifecycle()
    val selectedClassCode by viewModel.selectedClassCode.collectAsStateWithLifecycle()
    val selectedTerm by viewModel.selectedTerm.collectAsStateWithLifecycle()
    val studentsWithGrades by viewModel.studentsInSelectedClass.collectAsStateWithLifecycle()

    val grades = remember(studentsWithGrades) {
        studentsWithGrades.mapNotNull { it.grade }
    }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val recipientEmail by viewModel.recipientEmail.collectAsStateWithLifecycle()
    var showEmailDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Export Center Hero Card
        val currentClass = classes.find { it.classCode == selectedClassCode } ?: classes.firstOrNull()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = AlgerianGreenDark,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تصدير كشف النقاط (Excel)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "ملف رسمي متوافق 100% مع منصة الرقمنة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Class & Term Info Summary
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الفوج: ${currentClass?.nameArabic ?: "غير محدد"} • الفصل الدراسي $selectedTerm",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val graded = studentsWithGrades.count { it.grade != null }
                            Text(
                                text = "تم رصد $graded من إجمالي ${studentsWithGrades.size} تلميذ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showEmailDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlgerianGreenDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_hero_email_btn")
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إرسال للإيميل", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Class Analytics & Statistics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إحصائيات ونتائج الفوج (الفصل $selectedTerm)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Class selection chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(classes) { classSection ->
                        val isSelected = classSection.classCode == selectedClassCode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectClass(classSection.classCode) },
                            label = { Text(classSection.nameArabic, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (grades.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نقاط مرصودة في هذا الفوج بعد لعرض الإحصائيات",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val count = grades.size
                    val finalAverages = grades.map { it.finalAverage }
                    val classAvg = finalAverages.average()
                    val maxGrade = finalAverages.maxOrNull() ?: 0.0
                    val minGrade = finalAverages.minOrNull() ?: 0.0
                    val passingCount = finalAverages.count { it >= 10.0 }
                    val passingRate = (passingCount.toDouble() / count.toDouble()) * 100.0

                    // 4 Grid Stats Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "معدل الفوج العام",
                            value = "${GradeCalculator.formatGrade(classAvg)}/20",
                            color = if (classAvg >= 10.0) GradeSuccess else GradeWarning
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "نسبة النجاح",
                            value = "${String.format(Locale.US, "%.1f", passingRate)}%",
                            color = if (passingRate >= 60.0) GradeSuccess else GradeWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "أعلى معدل",
                            value = "${GradeCalculator.formatGrade(maxGrade)}/20",
                            color = GradeHonor
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "أدنى معدل",
                            value = "${GradeCalculator.formatGrade(minGrade)}/20",
                            color = GradeDanger
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Bar for Success Rate
                    Text(
                        text = "نسبة النجاح (الحاصلين على 10/20 فما فوق): $passingCount من $count تلميذ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (passingRate / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = GradeSuccess,
                        trackColor = GradeDanger.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Distribution Breakdown
                    Text(
                        text = "توزيع الملاحظات والتقديرات:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val excellent = finalAverages.count { it >= 16.0 }
                    val good = finalAverages.count { it in 12.0..<16.0 }
                    val acceptable = finalAverages.count { it in 10.0..<12.0 }
                    val belowAverage = finalAverages.count { it < 10.0 }

                    DistributionRow(label = "ممتاز وجيد جداً (>= 16)", count = excellent, total = count, color = GradeHonor)
                    DistributionRow(label = "جيد وقريب من الجيد (12 - 15.99)", count = good, total = count, color = GradeSuccess)
                    DistributionRow(label = "مقبول (10 - 11.99)", count = acceptable, total = count, color = GradeWarning)
                    DistributionRow(label = "دون المتوسط (< 10)", count = belowAverage, total = count, color = GradeDanger)
                }
            }
        }

        // Section: Email & Export Excel Grades Sheet
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AlgerianGreenDark.copy(alpha = 0.12f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = AlgerianGreenDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "إرسال كشف النقاط (Excel) عبر البريد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تصدير رسمي بصيغة Microsoft Excel متوافق مع منظومة الرقمنة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showEmailDialog = true },
                    enabled = studentsWithGrades.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlgerianGreenDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_screen_send_excel_email_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال كشف ${currentClass?.nameArabic ?: "الفوج"} (الفصل $selectedTerm) إلى الإيميل",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showEmailDialog) {
        val currentClass = classes.find { it.classCode == selectedClassCode } ?: classes.firstOrNull()
        EmailExcelDialog(
            className = currentClass?.nameArabic ?: "الفوج الحالي",
            term = selectedTerm,
            studentsWithGrades = studentsWithGrades,
            initialEmail = recipientEmail,
            onDismiss = { showEmailDialog = false },
            onSendEmail = { email, useCsv ->
                viewModel.exportAndEmailExcel(
                    context = context,
                    recipientEmail = email,
                    className = currentClass?.nameArabic ?: "الفوج",
                    term = selectedTerm,
                    studentsWithGrades = studentsWithGrades,
                    useCsvFormat = useCsv
                )
            },
            onShareFile = { useCsv ->
                viewModel.exportAndShareExcel(
                    context = context,
                    className = currentClass?.nameArabic ?: "الفوج",
                    term = selectedTerm,
                    studentsWithGrades = studentsWithGrades,
                    useCsvFormat = useCsv
                )
            }
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DistributionRow(
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = color,
                shape = CircleShape,
                modifier = Modifier.size(10.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "$count تلميذ (${(pct * 100).toInt()}%)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.GradeCalculator
import com.example.ui.theme.GradeDanger
import com.example.ui.theme.GradeHonor
import com.example.ui.theme.GradeSuccess
import com.example.ui.theme.GradeWarning
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeEntryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val formState by viewModel.gradeFormState.collectAsStateWithLifecycle()
    val student = formState.student

    if (student == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "لم يتم تحديد أي تلميذ للرصد",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.selectTab(ScreenTab.CLASSES) }) {
                    Text("العودة إلى قائمة الأفواج")
                }
            }
        }
        return
    }

    val scrollState = rememberScrollState()

    val passColor by animateColorAsState(
        targetValue = when {
            formState.finalAverage >= 15.0 -> GradeHonor
            formState.finalAverage >= 10.0 -> GradeSuccess
            formState.finalAverage >= 8.0 -> GradeWarning
            else -> GradeDanger
        },
        label = "avgColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "رصد نقطة: ${student.fullName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "الفوج: ${student.className} | رقم التعريف: ${student.nationalId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { viewModel.selectTab(ScreenTab.CLASSES) },
                    modifier = Modifier.testTag("grade_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            actions = {
                // Save Button in Top Bar
                Button(
                    onClick = { viewModel.saveCurrentGrade() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("save_grade_top_btn")
                ) {
                    Icon(
                        if (formState.isSaved) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = "حفظ",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (formState.isSaved) "تم الحفظ" else "حفظ")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Scrollable Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Term Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(1, 2, 3).forEach { term ->
                    FilterChip(
                        selected = formState.term == term,
                        onClick = { viewModel.selectTerm(term) },
                        label = { Text("الفصل $term", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .testTag("form_term_chip_$term")
                    )
                }
            }

            // Real-Time Results Summary Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = passColor.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "المعدل النهائي للمادة (الفصل ${formState.term})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = passColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${GradeCalculator.formatGrade(formState.finalAverage)} / 20",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = passColor,
                        modifier = Modifier.testTag("final_average_display")
                    )

                    Surface(
                        color = passColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = GradeCalculator.getAppreciation(formState.finalAverage),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Intermediate formulas summary pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummarySubPill(
                            title = "مجموع التقويم (ع/20)",
                            value = "${GradeCalculator.formatGrade(formState.continuousAssessmentTotal)}",
                            formula = "انضباط + داخل + خارج"
                        )
                        SummarySubPill(
                            title = "معدل التقويم والفرض",
                            value = "${GradeCalculator.formatGrade(formState.interimAverage)}",
                            formula = "(التقويم + الفرض) ÷ 2"
                        )
                    }
                }
            }

            // 1. Continuous Assessment Components (التقويم المستمر - Max 20 pts)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. عناصر التقويم المستمر (المجموع: 20)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${GradeCalculator.formatGrade(formState.continuousAssessmentTotal)} / 20",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1.1 Discipline & Attendance (الانضباط والمواظبة - 0..7)
                    ScoreInputRow(
                        label = "الانضباط والمواظبة",
                        subLabel = "السلوك، الغيابات، الحضور والتجهيز",
                        score = formState.disciplineScore,
                        maxScore = GradeCalculator.MAX_DISCIPLINE,
                        onScoreChange = { viewModel.updateDisciplineScore(it) },
                        testTagPrefix = "discipline"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1.2 In-Class Learning Performance (المردود في أنشطة التعلم داخل القسم - 0..7)
                    ScoreInputRow(
                        label = "المردود في أنشطة التعلم داخل القسم",
                        subLabel = "المشاركة الصفية، الفهم، العمل الجماعي",
                        score = formState.inClassScore,
                        maxScore = GradeCalculator.MAX_IN_CLASS,
                        onScoreChange = { viewModel.updateInClassScore(it) },
                        testTagPrefix = "in_class"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1.3 Out-of-Class Homework (المردود في أنشطة التعلم خارج القسم - 0..6)
                    ScoreInputRow(
                        label = "المردود في أنشطة التعلم خارج القسم",
                        subLabel = "الواجبات المنزلية، المشاريع، البحوث",
                        score = formState.outClassScore,
                        maxScore = GradeCalculator.MAX_OUT_CLASS,
                        onScoreChange = { viewModel.updateOutClassScore(it) },
                        testTagPrefix = "out_class"
                    )
                }
            }

            // 2. Assignment Score (نقطة الفرض - 0..20)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "2. نقطة الفرض المحروس (0 - 20)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ScoreInputRow(
                        label = "نقطة الفرض (ع/20)",
                        subLabel = "الفرض الرسمي للفصل ${formState.term}",
                        score = formState.assignmentScore,
                        maxScore = GradeCalculator.MAX_ASSIGNMENT,
                        onScoreChange = { viewModel.updateAssignmentScore(it) },
                        testTagPrefix = "assignment"
                    )
                }
            }

            // 3. Exam Score (نقطة الاختبار - 0..20)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "3. نقطة الاختبار الفصلي (0 - 20)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ScoreInputRow(
                        label = "نقطة الاختبار (ع/20)",
                        subLabel = "اختبار الفصل ${formState.term} الرسمي",
                        score = formState.examScore,
                        maxScore = GradeCalculator.MAX_EXAM,
                        onScoreChange = { viewModel.updateExamScore(it) },
                        testTagPrefix = "exam"
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }

        // Bottom Bar: Fast Student Stepper (Previous / Next Student)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateToAdjacentStudent(-1) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("prev_student_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("السابق")
                }

                Button(
                    onClick = { viewModel.saveCurrentGrade() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_grade_bottom_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ النقطة", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.navigateToAdjacentStudent(1) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("next_student_btn")
                ) {
                    Text("التالي")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun SummarySubPill(
    title: String,
    value: String,
    formula: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formula,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ScoreInputRow(
    label: String,
    subLabel: String,
    score: Double,
    maxScore: Double,
    onScoreChange: (Double) -> Unit,
    testTagPrefix: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Numeric score badge
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "${GradeCalculator.formatGrade(score)} / $maxScore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("${testTagPrefix}_score_value")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider control
        Slider(
            value = score.toFloat(),
            onValueChange = { onScoreChange(GradeCalculator.roundToTwoDecimals(it.toDouble())) },
            valueRange = 0f..maxScore.toFloat(),
            steps = if (maxScore <= 7.0) (maxScore * 2 - 1).toInt() else (maxScore * 2 - 1).toInt(), // 0.5 increments
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("${testTagPrefix}_slider")
        )

        // Quick Steppers (-1, -0.5, +0.5, +1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickStepButton(text = "-1") { onScoreChange((score - 1.0).coerceAtLeast(0.0)) }
            Spacer(modifier = Modifier.width(4.dp))
            QuickStepButton(text = "-0.5") { onScoreChange((score - 0.5).coerceAtLeast(0.0)) }
            Spacer(modifier = Modifier.width(8.dp))
            QuickStepButton(text = "+0.5") { onScoreChange((score + 0.5).coerceAtMost(maxScore)) }
            Spacer(modifier = Modifier.width(4.dp))
            QuickStepButton(text = "+1") { onScoreChange((score + 1.0).coerceAtMost(maxScore)) }
        }
    }
}

@Composable
private fun QuickStepButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.size(width = 38.dp, height = 28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

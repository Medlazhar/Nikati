package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.AlgerianGreen
import com.example.ui.theme.AlgerianGreenDark
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

/**
 * صفحة الدخول للتطبيق (Welcome / Login / Entry Landing Screen).
 * مصممة بخلفية بيضاء نقية مع أيقونة كبيرة وواضحة للتطبيق
 * ومعلومات عن المنظومة وزر بارز للدخول إلى التطبيق.
 */
@Composable
fun WelcomeScreen(
    viewModel: MainViewModel,
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val classes by viewModel.allClasses.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val totalStudents = classes.sumOf { it.studentCount }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("welcome_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Official Algerian Flag Accent Strip / Badge
            Surface(
                color = AlgerianGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AlgerianGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = AlgerianGreenDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الجمهورية الجزائرية الديمقراطية الشعبية",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlgerianGreenDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Large App Icon with Pure White Background and Subtle Elevation
            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(36.dp), spotColor = AlgerianGreenDark.copy(alpha = 0.25f))
                    .border(3.dp, AlgerianGreen.copy(alpha = 0.2f), RoundedCornerShape(36.dp))
                    .testTag("welcome_app_large_icon"),
                shape = RoundedCornerShape(36.dp),
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nikati_app_icon_1790948284399),
                    contentDescription = "أيقونة تطبيق نقاطي",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Title & Branding
            Text(
                text = "نِقَاطِي",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = AlgerianGreenDark,
                modifier = Modifier.testTag("welcome_app_title")
            )
            Text(
                text = "NIKATI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "منظومة رقمنة متابعة ورصد وتصدير نقاط تلاميذ التعليم المتوسط",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Statistics Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Class,
                                contentDescription = null,
                                tint = AlgerianGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${classes.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AlgerianGreenDark
                            )
                        }
                        Text(
                            text = "الأفواج المسجلة",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF718096)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(Color(0xFFCBD5E0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalStudents",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "إجمالي التلاميذ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF718096)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Main Entry Button (زر الدخول للتطبيق)
            Button(
                onClick = onEnterApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_enter_application"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlgerianGreenDark,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Login,
                        contentDescription = "دخول",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "الدخول إلى التطبيق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Shortcut Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.selectTab(ScreenTab.IMPORT_EXCEL) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("welcome_import_excel_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استيراد Excel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.selectTab(ScreenTab.SCANNER) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("welcome_scanner_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسح QR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Official Note Footer
            Text(
                text = "وفق المعايير الرسمية لوزارة التربية الوطنية للجمهورية الجزائرية\nحساب المعدل الفصلي: ((التقويم + الفرض) ÷ 2 × 2 + الاختبار × 2) ÷ 4",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = Color(0xFFA0AEC0),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

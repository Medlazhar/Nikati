package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.ClassesScreen
import com.example.ui.screens.GradeEntryScreen
import com.example.ui.screens.ImportExcelScreen
import com.example.ui.screens.ManualEntryScreen
import com.example.ui.screens.PrintBadgesScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as StudentGradeApp
        MainViewModelFactory(app.studentRepository, app.syncRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for Arabic primary educational UI
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreenContainer(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreenContainer(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect UI event notifications (snackbar)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Hardware back navigation handler
    BackHandler(enabled = currentTab != ScreenTab.WELCOME) {
        if (currentTab == ScreenTab.CLASSES) {
            viewModel.selectTab(ScreenTab.WELCOME)
        } else {
            viewModel.selectTab(ScreenTab.CLASSES)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentTab != ScreenTab.WELCOME) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    // Tab 1: Classes & Students
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.CLASSES,
                        onClick = { viewModel.selectTab(ScreenTab.CLASSES) },
                        icon = { Icon(Icons.Default.Group, contentDescription = "الأفواج") },
                        label = { Text("الأفواج", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_classes_tab")
                    )

                    // Tab 2: QR Scanner
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.SCANNER,
                        onClick = { viewModel.selectTab(ScreenTab.SCANNER) },
                        icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح QR") },
                        label = { Text("مسح QR", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_scanner_tab")
                    )

                    // Tab 3: Print Badges
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.PRINT_BADGES,
                        onClick = { viewModel.selectTab(ScreenTab.PRINT_BADGES) },
                        icon = { Icon(Icons.Default.Print, contentDescription = "الطباعة") },
                        label = { Text("الطباعة A4", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_print_tab")
                    )

                    // Tab 4: Import Excel (الرقمنة)
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.IMPORT_EXCEL,
                        onClick = { viewModel.selectTab(ScreenTab.IMPORT_EXCEL) },
                        icon = { Icon(Icons.Default.FileUpload, contentDescription = "الرقمنة") },
                        label = { Text("استيراد", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_import_tab")
                    )

                    // Tab 5: Export File (تصدير الملف)
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.EXPORT_REPORTS,
                        onClick = { viewModel.selectTab(ScreenTab.EXPORT_REPORTS) },
                        icon = { Icon(Icons.Default.FileDownload, contentDescription = "تصدير الملف") },
                        label = { Text("تصدير الملف", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_export_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.WELCOME -> WelcomeScreen(
                    viewModel = viewModel,
                    onEnterApp = { viewModel.selectTab(ScreenTab.CLASSES) }
                )
                ScreenTab.CLASSES -> ClassesScreen(viewModel = viewModel)
                ScreenTab.GRADE_ENTRY -> GradeEntryScreen(viewModel = viewModel)
                ScreenTab.SCANNER -> ScannerScreen(viewModel = viewModel)
                ScreenTab.PRINT_BADGES -> PrintBadgesScreen(viewModel = viewModel)
                ScreenTab.IMPORT_EXCEL -> ImportExcelScreen(viewModel = viewModel)
                ScreenTab.EXPORT_REPORTS -> SyncScreen(viewModel = viewModel)
                ScreenTab.MANUAL_ENTRY -> ManualEntryScreen(viewModel = viewModel, onBack = { viewModel.selectTab(ScreenTab.CLASSES) })
            }
        }
    }
}

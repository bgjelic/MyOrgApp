package com.example.myorgapp

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import com.example.myorgapp.ui.theme.MyOrgAppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myorgapp.SharedCardViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SharedCardViewModel by viewModels()
    private val notificationCardId = mutableStateOf<Long?>(null)

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("card_pref", MODE_PRIVATE)
        val lang = prefs.getString("settings_language", "ENGLISH") ?: "ENGLISH"
        val locale = if (lang == "CROATIAN") Locale("hr") else Locale.ENGLISH
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createReminderNotificationChannel(this)
        checkNotificationIntent(intent)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            MyOrgAppTheme(themeMode = settings.themeMode, colorTheme = settings.colorTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                val navController = rememberNavController()

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ -> }
                val exactAlarmLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { _ -> }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!am.canScheduleExactAlarms()) {
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:$packageName")
                            }.let { exactAlarmLauncher.launch(it) }
                        }
                    }
                }

                val cardId = notificationCardId.value
                LaunchedEffect(cardId) {
                    cardId?.let { id ->
                        viewModel.setHighlightedCardId(id)
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                        notificationCardId.value = null
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "main",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("main") {
                        MainScreen(
                            viewModel = viewModel,
                            onAdd = {
                                viewModel.setEditing(null)
                                navController.navigate("editor")
                            },
                            onEdit = { card: CardItem ->
                                viewModel.setEditing(card)
                                navController.navigate("editor")
                            },
                            onDelete = { id -> viewModel.deleteCard(id) },
                            onToggleFinished = { card -> viewModel.toggleFinished(card) },
                            onDeleteCompleted = { id -> viewModel.deleteCompletedCard(id) },
                            onSettings = { navController.navigate("settings") },
                            onCalendar = { navController.navigate("calendar") },
                            onStats = { navController.navigate("stats") }
                        )
                    }
                    composable("editor") {
                        EditorScreen(
                            viewModel = viewModel,
                            onDone = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onDone = { navController.popBackStack() }
                        )
                    }
                    composable("calendar") {
                        CalendarScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onDayDrill = { card: CardItem ->
                                viewModel.setEditing(card)
                                navController.navigate("editor")
                            },
                            onToggleFinished = { card -> viewModel.toggleFinished(card) }
                        )
                    }
                    composable("stats") {
                        StatsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading…")
                        }
                    }
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkNotificationIntent(intent)
    }

    private fun checkNotificationIntent(intent: Intent) {
        val id = intent.getLongExtra(NotificationHelper.EXTRA_CARD_ID, -1L)
        if (id != -1L) {
            notificationCardId.value = id
        }
    }
}
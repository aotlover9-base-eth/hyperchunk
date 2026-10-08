package com.hyperchunk.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.hyperchunk.app.alarm.AlarmScheduler
import com.hyperchunk.app.alarm.NotificationHelper
import com.hyperchunk.app.data.storage.ChunkRepository
import com.hyperchunk.app.data.storage.SettingsRepository
import com.hyperchunk.app.ui.screens.HomeScreen
import com.hyperchunk.app.ui.theme.HyperTheme
import com.hyperchunk.app.widget.HyperChunkWidgetProvider

class MainActivity : ComponentActivity() {

    private lateinit var repository: ChunkRepository
    private lateinit var settingsRepository: SettingsRepository

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notification permission handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = ChunkRepository(this)
        settingsRepository = SettingsRepository(this)
        NotificationHelper.createNotificationChannel(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule all configured alarms
        AlarmScheduler.rescheduleAllAlarms(this)

        // Update widget on launch
        HyperChunkWidgetProvider.updateAllWidgets(this)

        setContent {
            val settings by settingsRepository.settings.collectAsState()
            HyperTheme(settings = settings) {
                HomeScreen(
                    repository = repository,
                    settingsRepository = settingsRepository,
                    onScheduleAlarm = { chunk ->
                        AlarmScheduler.scheduleChunkAlarm(this, chunk)
                        HyperChunkWidgetProvider.updateAllWidgets(this)
                    },
                    onCancelAlarm = { chunkId ->
                        AlarmScheduler.cancelChunkAlarm(this, chunkId)
                        HyperChunkWidgetProvider.updateAllWidgets(this)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        HyperChunkWidgetProvider.updateAllWidgets(this)
    }
}

package com.example.amasamya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.amasamya.theme.AMASAMYATheme
import com.example.amasamya.settings.SettingsManager
import com.example.amasamya.utils.AdbReportServer

class MainActivity : ComponentActivity() {
  companion object {
    var hasAnnouncedLaunch = false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val settingsManager = SettingsManager(this)
    if (settingsManager.isAdbServerEnabled) {
      AdbReportServer.start(this)
    }

    enableEdgeToEdge()
    setContent {
      AMASAMYATheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    AdbReportServer.stop()
  }
}

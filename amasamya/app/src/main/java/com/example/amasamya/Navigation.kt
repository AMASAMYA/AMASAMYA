package com.example.amasamya

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.amasamya.ui.screens.DashboardScreen
import com.example.amasamya.ui.screens.SettingsScreen
import com.example.amasamya.ui.screens.HistoryScreen
import com.example.amasamya.ui.screens.ReportDetailScreen
import com.example.amasamya.ui.screens.FocusPathScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Dashboard)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Dashboard> {
          DashboardScreen(
            onNavigate = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<Settings> {
          SettingsScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<History> {
          HistoryScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToDetail = { navKey -> backStack.add(navKey) },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<ReportDetail> { route ->
          ReportDetailScreen(
            sessionId = route.sessionId,
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToFocusPath = { screenName ->
              backStack.add(FocusPath(sessionId = route.sessionId, screenName = screenName))
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
        entry<FocusPath> { route ->
          FocusPathScreen(
            sessionId = route.sessionId,
            screenName = route.screenName,
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}

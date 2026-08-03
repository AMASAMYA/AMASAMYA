package com.example.amasamya.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.amasamya.ReportDetail
import com.example.amasamya.db.DatabaseHelper
import com.example.amasamya.db.AuditSession
import com.example.amasamya.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var sessions by remember { mutableStateOf<List<AuditSession>>(emptyList()) }

    // Fetch reports
    LaunchedEffect(Unit) {
        sessions = dbHelper.getAllSessions()
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        if (manager.isEnabled) {
            try {
                val event = android.view.accessibility.AccessibilityEvent.obtain(
                    android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                )
                event.text.add("Saved Audit Reports screen loaded successfully")
                event.className = "com.example.amasamya.ui.screens.HistoryScreen"
                event.packageName = context.packageName
                manager.sendAccessibilityEvent(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(DeepSpace, MidnightBlue)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Audit Reports",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back to dashboard",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(backgroundBrush)
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No audit reports saved yet.\nStart a recording session on the dashboard to test apps.",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { session ->
                    SessionItem(
                        session = session,
                        onClick = { onNavigateToDetail(ReportDetail(session.id)) },
                        onDelete = {
                            dbHelper.deleteSession(session.id)
                            sessions = dbHelper.getAllSessions() // Refresh list
                            
                            // Send accessibility announcement
                            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
                            if (manager.isEnabled) {
                                try {
                                    val event = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT)
                                    event.text.add("Report \"${session.name}\" deleted")
                                    event.className = "com.example.amasamya.ui.screens.HistoryScreen"
                                    event.packageName = context.packageName
                                    manager.sendAccessibilityEvent(event)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            android.widget.Toast.makeText(context, "Report \"${session.name}\" deleted", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionItem(
    session: AuditSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val formattedDate = remember(session.date) { sdf.format(Date(session.date)) }

    val levelColor = remember(session.wcagLevel) {
        when (session.wcagLevel) {
            "AAA" -> ElectricLavender
            "AA" -> VibrantCyan
            else -> AmberGold
        }
    }

    Surface(
        color = GlassySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF2C3246)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = PureWhite
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        onClick(label = "Open report details for ${session.name}") {
                            onClick()
                            true
                        }
                    }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = session.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PureWhite
                    )
                    Text(
                        text = "App: ${session.packageName}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = levelColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, levelColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Level ${session.wcagLevel}",
                                color = levelColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = formattedDate,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics {
                    contentDescription = "Delete report: ${session.name}"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = NeonRed
                )
            }
        }
    }
}

package com.example.amasamya.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amasamya.db.DatabaseHelper
import com.example.amasamya.db.FocusPathNode
import com.example.amasamya.utils.ReportExporter
import com.example.amasamya.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPathScreen(
    sessionId: Long,
    screenName: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    
    var focusNodes by remember { mutableStateOf<List<FocusPathNode>>(emptyList()) }
    var selectedNode by remember { mutableStateOf<FocusPathNode?>(null) }
    var sessionName by remember { mutableStateOf("") }

    LaunchedEffect(sessionId, screenName) {
        val session = dbHelper.getSession(sessionId)
        if (session != null) {
            sessionName = session.name
        }
        val allNodes = dbHelper.getFocusNodesForSession(sessionId)
        focusNodes = allNodes.filter { it.screenName == screenName }
        
        val manager = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        if (manager.isEnabled) {
            try {
                val event = android.view.accessibility.AccessibilityEvent.obtain(
                    android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
                )
                event.text.add("Focus step path screen loaded successfully")
                event.className = "com.example.amasamya.ui.screens.FocusPathScreen"
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
                        text = "Focus Path Map",
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = PureWhite
                        )
                    }
                },
                actions = {
                    if (focusNodes.isNotEmpty()) {
                        IconButton(onClick = {
                            val session = dbHelper.getSession(sessionId)
                            if (session != null) {
                                val file = ReportExporter.exportFocusPathSvg(context, session, screenName, focusNodes)
                                if (file != null) {
                                    shareReportFile(context, file)
                                } else {
                                    Toast.makeText(context, "Failed to export SVG", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export focus path as SVG",
                                tint = PureWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(backgroundBrush)
    ) { paddingValues ->
        if (focusNodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No focus path elements recorded.\nMake sure screen reader gestures were active on this screen.",
                    color = LightGrey,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
            }
        } else {
            // Parse bounds dynamically to scale Canvas
            val parsedBounds = remember(focusNodes) {
                focusNodes.mapNotNull { node ->
                    val boundsArr = parseBounds(node.bounds)
                    if (boundsArr != null && (boundsArr[0] != 0 || boundsArr[1] != 0 || boundsArr[2] != 0 || boundsArr[3] != 0)) {
                        Pair(node, boundsArr)
                    } else null
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Screen: $screenName",
                    fontSize = 14.sp,
                    color = VibrantCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )

                // 1. Canvas Flow Visualizer Card
                Surface(
                    color = GlassySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C3246)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .semantics {
                            contentDescription = "Visual diagram representing the recorded focus path sequence from start to finish."
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (parsedBounds.isNotEmpty()) {
                                // Find boundaries
                                var minX = Int.MAX_VALUE
                                var minY = Int.MAX_VALUE
                                var maxX = Int.MIN_VALUE
                                var maxY = Int.MIN_VALUE

                                for ((_, rect) in parsedBounds) {
                                    if (rect[0] < minX) minX = rect[0]
                                    if (rect[1] < minY) minY = rect[1]
                                    if (rect[2] > maxX) maxX = rect[2]
                                    if (rect[3] > maxY) maxY = rect[3]
                                }

                                minX = maxOf(0, minX - 40)
                                minY = maxOf(0, minY - 40)
                                maxX += 40
                                maxY += 40

                                val boundsWidth = maxX - minX
                                val boundsHeight = maxY - minY

                                val scaleX = size.width / boundsWidth.toFloat()
                                val scaleY = size.height / boundsHeight.toFloat()
                                val scale = minOf(scaleX, scaleY)

                                val offsetX = (size.width - boundsWidth * scale) / 2 - minX * scale
                                val offsetY = (size.height - boundsHeight * scale) / 2 - minY * scale

                                // Draw connecting path lines
                                for (i in 0 until parsedBounds.size - 1) {
                                    val rectA = parsedBounds[i].second
                                    val rectB = parsedBounds[i + 1].second

                                    val cxA = rectA[0] + (rectA[2] - rectA[0]) / 2
                                    val cyA = rectA[1] + (rectA[3] - rectA[1]) / 2
                                    
                                    val cxB = rectB[0] + (rectB[2] - rectB[0]) / 2
                                    val cyB = rectB[1] + (rectB[3] - rectB[1]) / 2

                                    drawLine(
                                        color = VibrantCyan,
                                        start = Offset(cxA * scale + offsetX, cyA * scale + offsetY),
                                        end = Offset(cxB * scale + offsetX, cyB * scale + offsetY),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }

                                // Draw element bounds boxes
                                for ((node, rect) in parsedBounds) {
                                    val left = rect[0] * scale + offsetX
                                    val top = rect[1] * scale + offsetY
                                    val w = (rect[2] - rect[0]) * scale
                                    val h = (rect[3] - rect[1]) * scale

                                    val isCurrentSelected = selectedNode?.id == node.id
                                    val boxColor = if (isCurrentSelected) ElectricLavender else VibrantCyan
                                    val fillAlpha = if (isCurrentSelected) 0.3f else 0.12f

                                    // Draw translucent filled area
                                    drawRect(
                                        color = boxColor.copy(alpha = fillAlpha),
                                        topLeft = Offset(left, top),
                                        size = Size(w, h)
                                    )

                                    // Draw stroke outline
                                    drawRect(
                                        color = boxColor,
                                        topLeft = Offset(left, top),
                                        size = Size(w, h),
                                        style = Stroke(width = if (isCurrentSelected) 3.dp.toPx() else 1.5.dp.toPx())
                                    )

                                    // Draw order sequence circles
                                    val circleRadius = 10.dp.toPx()
                                    val circleX = left + circleRadius
                                    val circleY = top + circleRadius
                                    drawCircle(
                                        color = boxColor,
                                        radius = circleRadius,
                                        center = Offset(circleX, circleY)
                                    )

                                    // Draw order text
                                    drawContext.canvas.nativeCanvas.drawText(
                                        node.focusOrder.toString(),
                                        circleX,
                                        circleY + 4.dp.toPx(),
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 10.sp.toPx()
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Focused Elements Sequence List
                Text(
                    text = "Focused Sequence List",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PureWhite
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(focusNodes) { node ->
                        val isSelected = selectedNode?.id == node.id
                        FocusNodeItemCard(
                            node = node,
                            isSelected = isSelected,
                            onClick = {
                                selectedNode = if (isSelected) null else node
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FocusNodeItemCard(
    node: FocusPathNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelColor = if (isSelected) ElectricLavender else VibrantCyan

    Surface(
        color = GlassySurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) ElectricLavender else Color(0xFF2C3246)),
        modifier = modifier.fillMaxWidth()
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
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    selected = isSelected
                    onClick(label = if (isSelected) "Deselect step ${node.focusOrder}" else "Select step ${node.focusOrder}") {
                        onClick()
                        true
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sequence Number Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(levelColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node.focusOrder.toString(),
                        color = DeepSpace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Node details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = node.className.substringAfterLast("."),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) ElectricLavender else PureWhite
                    )
                    
                    val textContent = if (node.text.isNotBlank()) node.text else node.contentDescription
                    if (textContent.isNotBlank()) {
                        Text(
                            text = "\"$textContent\"",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// Sharing provider helper
private fun shareReportFile(context: android.content.Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/svg+xml"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Focus Path SVG Map")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = android.content.Intent.createChooser(shareIntent, "Share Focus Path SVG")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing SVG file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun parseBounds(boundsStr: String): IntArray? {
    try {
        val regex = Regex("\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]")
        val match = regex.find(boundsStr)
        if (match != null) {
            val left = match.groupValues[1].toInt()
            val top = match.groupValues[2].toInt()
            val right = match.groupValues[3].toInt()
            val bottom = match.groupValues[4].toInt()
            return intArrayOf(left, top, right, bottom)
        }
    } catch (e: Exception) {
        // Ignore
    }
    return null
}

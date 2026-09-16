package com.example.amasamya.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amasamya.theme.*
import com.example.amasamya.utils.PdfRemediationEngine

@Composable
fun PdfRemediationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var sampleContent by remember {
        mutableStateOf(
            """
            AMASAMYA Mobile Accessibility Guidelines
            Section 1: Introduction to Mobile Accessibility
            Accessibility ensures digital content is barrier-free for everyone.
            [Figure] Screenshot of AMASAMYA Dashboard
            Section 2: Minimum Touch Targets
            Buttons must measure at least 48x48dp.
            """.trimIndent()
        )
    }

    var report by remember {
        mutableStateOf(PdfRemediationEngine.analyzeDocument("AMASAMYA_Sample_Doc.pdf", sampleContent))
    }

    var statusMessage by remember { mutableStateOf("PDF structure loaded. 100% WCAG PDF/UA compliant.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📄 DocRemediate Studio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantCyan,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Mobile PDF/UA & Structural Tagging Remediation",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = GlassySurface, contentColor = PureWhite),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Back")
            }
        }

        // Executive Score Card
        Surface(
            color = GlassySurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF2C3246)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PDF/UA Compliance Score", fontSize = 12.sp, color = TextSecondary)
                    Text("${report.complianceScore}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (report.complianceScore >= 85) NeonGreen.copy(alpha = 0.2f) else AmberGold.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (report.complianceScore >= 85) "PDF/UA Ready" else "Remediation Needed",
                        color = if (report.complianceScore >= 85) NeonGreen else AmberGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Structural Tag List
        Text(
            text = "Structural Tag Tree (${report.totalNodes} Nodes)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite,
            modifier = Modifier.semantics { heading() }
        )

        report.tagNodes.forEach { node ->
            Surface(
                color = GlassySurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF2C3246)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(VibrantCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(node.tagType, color = VibrantCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Text("Tag ID: ${node.id}", fontSize = 10.sp, color = TextSecondary)
                    }

                    Text(node.contentText, color = PureWhite, fontSize = 13.sp)

                    if (node.tagType == "Figure") {
                        Text(
                            text = "Alt Text: ${node.altText.ifBlank { "Missing image alt text!" }}",
                            color = if (node.altText.isNotBlank()) NeonGreen else NeonRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Action Export Button
        Button(
            onClick = {
                val html = PdfRemediationEngine.generateRemediatedHtml(report)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/html"
                    putExtra(Intent.EXTRA_SUBJECT, "Remediated Tagged PDF - ${report.documentName}")
                    putExtra(Intent.EXTRA_TEXT, html)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Remediated PDF/UA Document"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = VibrantCyan, contentColor = DeepSpace),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Export Tagged PDF / EPUB3 Document", fontWeight = FontWeight.Bold)
        }
    }
}

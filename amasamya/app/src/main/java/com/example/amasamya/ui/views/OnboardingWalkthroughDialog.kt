package com.example.amasamya.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amasamya.theme.*

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tipText: String
)

@Composable
fun OnboardingWalkthroughDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "Welcome to AMASAMYA",
            description = "AMASAMYA is an offline, privacy-first accessibility auditing suite built to test Android apps against WCAG 2.2, GIGW 3.0, and IS 17802 standards.",
            icon = Icons.Default.CheckCircle,
            tipText = "Designed from the ground up to be 100% screen-reader accessible."
        ),
        OnboardingStep(
            title = "1. Enable Accessibility Service",
            description = "Tap 'Enable Service' on the dashboard to allow AMASAMYA to inspect UI elements, contrast ratios, and touch target sizes in target applications.",
            icon = Icons.Default.Settings,
            tipText = "All audit calculations occur locally on your device. Zero data leaves your phone."
        ),
        OnboardingStep(
            title = "2. Trigger Audits & Voice Commands",
            description = "Use the floating audit button or enable Hands-Free Voice Commands to say 'Scan screen' or 'Start session' while testing any Android app.",
            icon = Icons.Default.PlayArrow,
            tipText = "You can also test focus navigation using our built-in Screen Reader Simulator Mode."
        ),
        OnboardingStep(
            title = "3. Executive VPAT & Code Fixes",
            description = "View instant audit scores, copy Jetpack Compose & XML code fixes, and export official VPAT 2.4 ACR compliance reports in HTML or Markdown.",
            icon = Icons.Default.Share,
            tipText = "VPAT reports map findings directly to WCAG 2.2, Section 508, and EN 301 549 tables."
        )
    )

    val step = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.size - 1

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSpace,
        titleContentColor = PureWhite,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.semantics { heading() }
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = VibrantCyan,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = step.description,
                    color = PureWhite,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassySurface)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 Pro Tip:",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = step.tipText,
                            color = LightGrey,
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = "Step ${currentStepIndex + 1} of ${steps.size}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isLastStep) {
                        onComplete()
                    } else {
                        currentStepIndex++
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantCyan,
                    contentColor = DeepSpace
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.semantics {
                    contentDescription = if (isLastStep) "Complete Walkthrough and Get Started" else "Next Step"
                }
            ) {
                Text(
                    text = if (isLastStep) "Get Started 🚀" else "Next ➔",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            if (currentStepIndex > 0) {
                OutlinedButton(
                    onClick = { currentStepIndex-- },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Previous", color = PureWhite)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Skip Tour", color = TextSecondary)
                }
            }
        }
    )
}

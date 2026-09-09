package com.example.amasamya.ui.views

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amasamya.theme.*

@Composable
fun UserFeedbackDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    fun announceFeedbackSubmission() {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        if (manager.isEnabled) {
            try {
                val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                event.text.add("Feedback submitted successfully. Thank you for helping improve AMASAMYA accessibility.")
                event.className = "UserFeedbackDialog"
                event.packageName = context.packageName
                manager.sendAccessibilityEvent(event)
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSpace,
        titleContentColor = PureWhite,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "💬 Submit Feedback & Barrier Report",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.semantics { heading() }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                if (isSubmitted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassySurface, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅ Thank You!", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Your feedback has been logged locally. Our accessibility team values your input!",
                                color = PureWhite,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Encounters any accessibility barriers, missing labels, or feature ideas? Share your feedback directly with our development team.",
                        color = PureWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Your Email (Optional)", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibrantCyan,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text("Feedback or Barrier Description", color = TextSecondary) },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibrantCyan,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantCyan, contentColor = DeepSpace),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            val textToSend = feedbackText
                            val emailToSend = userEmail
                            isSubmitted = true
                            announceFeedbackSubmission()
                            sendFeedbackEmail(context, emailToSend, textToSend)
                        }
                    },
                    enabled = feedbackText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantCyan, contentColor = DeepSpace),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.semantics {
                        contentDescription = "Submit Feedback Button"
                    }
                ) {
                    Text("Submit Feedback via Email 📧", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}

private fun sendFeedbackEmail(context: Context, userEmail: String, feedbackText: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:akhilesh@amasamya.com")
            putExtra(Intent.EXTRA_SUBJECT, "[AMASAMYA App Feedback] User Suggestion / Barrier Report")
            val body = StringBuilder()
            body.append("AMASAMYA User Feedback & Barrier Report\n\n")
            body.append("From Email: ").append(if (userEmail.isNotBlank()) userEmail else "Not provided").append("\n")
            body.append("App Version: v1.0.1 (Build 16)\n")
            body.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append(" (Android ").append(android.os.Build.VERSION.RELEASE).append(")\n\n")
            body.append("Feedback / Barrier Details:\n")
            body.append(feedbackText).append("\n\n")
            body.append("---\nSent from AMASAMYA Accessibility Engine")
            putExtra(Intent.EXTRA_TEXT, body.toString())
        }
        val chooser = Intent.createChooser(intent, "Send feedback to akhilesh@amasamya.com via...")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

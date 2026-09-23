package com.example.amasamya.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amasamya.theme.DeepSpace
import com.example.amasamya.theme.PureWhite
import com.example.amasamya.theme.VibrantCyan

@Composable
fun FeatureInstructionDialog(
    featureTitle: String,
    featureDescription: String,
    steps: List<String>,
    onDismiss: (doNotShowAgain: Boolean) -> Unit
) {
    var doNotShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss(doNotShowAgain) },
        title = {
            Text(
                text = "💡 How to Use: $featureTitle",
                fontWeight = FontWeight.Bold,
                color = VibrantCyan,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = featureDescription,
                    color = PureWhite.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            fontWeight = FontWeight.Bold,
                            color = VibrantCyan,
                            fontSize = 13.sp
                        )
                        Text(
                            text = step,
                            color = PureWhite.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            role = Role.Button
                            contentDescription = "Do not show instructions again checkbox"
                        }
                ) {
                    Checkbox(
                        checked = doNotShowAgain,
                        onCheckedChange = { doNotShowAgain = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = VibrantCyan,
                            uncheckedColor = PureWhite.copy(alpha = 0.6f),
                            checkmarkColor = DeepSpace
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Do not show again",
                        color = PureWhite.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDismiss(doNotShowAgain) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantCyan,
                    contentColor = DeepSpace
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "Got it, close feature instructions"
                }
            ) {
                Text(
                    text = "Got it",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = DeepSpace,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    )
}

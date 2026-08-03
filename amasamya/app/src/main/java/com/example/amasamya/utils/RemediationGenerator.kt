package com.example.amasamya.utils

object RemediationGenerator {

    data class CodeFix(
        val issueTitle: String,
        val issueCategory: String,
        val composeSnippet: String,
        val xmlSnippet: String,
        val explanation: String
    )

    fun generateFix(category: String, title: String, elementClassName: String?, currentDetails: String?): CodeFix {
        val catUpper = category.uppercase()
        val titleUpper = title.uppercase()

        return when {
            catUpper.contains("LABEL") || catUpper.contains("CONTENT_DESCRIPTION") || titleUpper.contains("LABEL") || titleUpper.contains("CONTENT DESCRIPTION") -> {
                CodeFix(
                    issueTitle = title,
                    issueCategory = "Missing Accessibility Label",
                    composeSnippet = """
                        // Jetpack Compose Fix:
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = "Descriptive action label here", // Added label
                            modifier = Modifier.semantics { role = Role.Button }
                        )
                    """.trimIndent(),
                    xmlSnippet = """
                        <!-- Android XML Fix: -->
                        <ImageView
                            android:id="@+id/action_icon"
                            android:layout_width="48dp"
                            android:layout_height="48dp"
                            android:contentDescription="@string/action_button_description" />
                    """.trimIndent(),
                    explanation = "Screen readers require a non-empty contentDescription on interactive icons and image controls to speak their purpose to blind users."
                )
            }

            catUpper.contains("TOUCH") || catUpper.contains("TARGET") || catUpper.contains("SIZE") || titleUpper.contains("TARGET") || titleUpper.contains("SIZE") -> {
                CodeFix(
                    issueTitle = title,
                    issueCategory = "Insufficient Touch Target Size",
                    composeSnippet = """
                        // Jetpack Compose Fix:
                        IconButton(
                            onClick = { /* action */ },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add item")
                        }
                    """.trimIndent(),
                    xmlSnippet = """
                        <!-- Android XML Fix: -->
                        <ImageButton
                            android:id="@+id/btn_add"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:minWidth="48dp"
                            android:minHeight="48dp"
                            android:padding="12dp"
                            android:contentDescription="@string/add_item" />
                    """.trimIndent(),
                    explanation = "WCAG guidelines require clickable touch targets to be at least 48dp x 48dp (or 44dp EN 301 549) to accommodate users with motor disabilities."
                )
            }

            catUpper.contains("CONTRAST") || catUpper.contains("COLOR") || titleUpper.contains("CONTRAST") -> {
                CodeFix(
                    issueTitle = title,
                    issueCategory = "Low Color Contrast",
                    composeSnippet = """
                        // Jetpack Compose Fix:
                        Text(
                            text = "Sample Text",
                            color = Color(0xFF111827), // High contrast dark text on light bg (7:1 ratio)
                            style = MaterialTheme.typography.bodyMedium
                        )
                    """.trimIndent(),
                    xmlSnippet = """
                        <!-- Android XML Fix: -->
                        <TextView
                            android:id="@+id/tv_title"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:textColor="#111827"
                            android:background="#FFFFFF" />
                    """.trimIndent(),
                    explanation = "Ensure text and background color combination yields at least a 4.5:1 contrast ratio for normal text and 3.0:1 for large text (24dp+)."
                )
            }

            catUpper.contains("INPUT") || catUpper.contains("EDIT") || catUpper.contains("FIELD") || titleUpper.contains("INPUT") -> {
                CodeFix(
                    issueTitle = title,
                    issueCategory = "Unlabelled Input Field",
                    composeSnippet = """
                        // Jetpack Compose Fix:
                        OutlinedTextField(
                            value = textState,
                            onValueChange = { textState = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.semantics {
                                contentDescription = "Enter your full name"
                            }
                        )
                    """.trimIndent(),
                    xmlSnippet = """
                        <!-- Android XML Fix: -->
                        <com.google.android.material.textfield.TextInputLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="@string/full_name_label">
                            <com.google.android.material.textfield.TextInputEditText
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content" />
                        </com.google.android.material.textfield.TextInputLayout>
                    """.trimIndent(),
                    explanation = "Editable text fields must have visual hints or accessibility labels so screen readers announce what input is expected."
                )
            }

            else -> {
                CodeFix(
                    issueTitle = title,
                    issueCategory = "General Accessibility Compliance",
                    composeSnippet = """
                        // Jetpack Compose Fix:
                        Row(
                            modifier = Modifier.semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = "Card item description"
                            }
                        ) {
                            // Child views merged as a single focusable unit
                        }
                    """.trimIndent(),
                    xmlSnippet = """
                        <!-- Android XML Fix: -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:focusable="true"
                            android:clickable="true"
                            android:contentDescription="@string/card_description">
                            <!-- Child elements -->
                        </LinearLayout>
                    """.trimIndent(),
                    explanation = "Group related views together using merged semantics or focusable container wrappers to streamline TalkBack reading flow."
                )
            }
        }
    }
}

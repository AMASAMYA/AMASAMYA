# AMASAMYA Proguard & R8 Deobfuscation Rules

# Preserve Jetpack Compose Semantics & UI Nodes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Preserve AMASAMYA Database & Room Entities
-keep class com.example.amasamya.db.** { *; }
-keepclassmembers class com.example.amasamya.db.** { *; }

# Preserve Accessibility Service & Navigation Components
-keep class com.example.amasamya.service.** { *; }
-keep class com.example.amasamya.ui.** { *; }
-keep class com.example.amasamya.utils.** { *; }

# Preserve Line Numbers & Source Attributes for Play Console Symbolication
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

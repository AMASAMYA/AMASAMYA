# ♿ AMASAMYA — Android Real-Time Accessibility Audit Engine

[![Android Build](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/about/versions/nougat)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Compliance](https://img.shields.io/badge/Compliance-WCAG%202.2%20%7C%20Section%20508%20%7C%20EN%20301%20549-00E5FF?style=flat-square)](#-compliance-standards)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%26%20Local-FF3D00?style=flat-square)](#-privacy--data-safety)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.style=flat-square)](LICENSE)

**AMASAMYA** is an open-source, blind-first, offline-first real-time accessibility auditing utility for Android. Built using modern Android architecture (Jetpack Compose, AccessibilityService API, custom WindowManager canvas overlays), AMASAMYA empowers developers, QA engineers, and accessibility auditors to evaluate target applications in real time without leaving the device.

---

## 🌟 Key Features

### 1. ⚡ Real-Time Diagnostic Overlays
Inspect app accessibility dynamically while interacting with any third-party Android application:
* 💬 **Live TalkBack Speech Captions**: Displays real-time visual text captions for screen reader spoken announcements at the bottom of the viewport.
* 📍 **Live Focus Trail Visualizer**: Paints animated vector paths connecting consecutive TalkBack focus coordinates to visualize screen reader reading order.
* 🟩🟨🟥 **Live Touch Target Boundary Mapper**: Highlights clickable layout views with color-coded bounding rects based on minimum dimension rules (Green >= 48dp, Yellow >= 24dp, Red < 24dp).
* 🔁 **Live Focus Trap & Loop Detector**: Detects repetitive focus traps and emits haptic warning pulses when tab navigation gets stuck.
* 🎨 **Live Contrast Drift Scanner**: Performs real-time local screenshot cropping and dominant-color extraction on node focus, alerting users if contrast falls below WCAG minimums (4.5:1 or 7:1).

### 2. 🎛️ Multi-Standard Compliance Rules Engine
Select international standards directly in Settings:
* **WCAG 2.2** (Level A, AA, AAA)
* **Section 508** (US Federal Government Standard)
* **EN 301 549** (European Accessibility Act)
* **Custom Minimum Target Size** (Configurable: 48dp, 44dp, 36dp)

### 3. 🛠️ One-Click Code Fix Generator
Generates exact, copyable fix code for every detected violation:
* **Jetpack Compose**: Generates `Modifier.semantics { contentDescription = "..." }` and `Modifier.defaultMinSize(48.dp)`.
* **Android XML**: Generates `android:contentDescription="..."`, `android:minWidth="48dp"`, `android:textColor="#..."`, and `android:hint="..."`.
* **Jira / GitHub Markdown Copy**: Copies fully formatted bug tickets directly to clipboard.

### 4. 📄 Executive Offline Report Exporter
Export reports without any internet connection in multiple formats:
* **Interactive Dark-Mode HTML** with embedded SVG focus maps.
* **PDF Executive Document** ready for sharing.
* **Markdown & SVG Focus Path Diagrams**.

### 5. 🎙️ Hands-Free Voice Commands
Control testing sessions via speech input:
* *"Scan screen"* — Triggers full screen hierarchy audit.
* *"Start session"* — Initializes recording session.
* *"Stop session"* — Saves audit report locally.
* *"Read summary"* — Narrates issue metrics via TTS.

### 6. 🎯 TalkBack Simulator Mode for Sighted Testers
Simulate TalkBack focus rings, navigation order, and spoken announcements directly without toggling Android system-wide screen reader gestures.

---

## 🏛️ System Architecture

```
+-----------------------------------------------------------------------+
|                            AMASAMYA App                               |
+-----------------------------------+-----------------------------------+
|  Jetpack Compose UI               | Accessibility Service Layer       |
|  - DashboardScreen                | - A11yAuditService                |
|  - SettingsScreen                 | - WindowManager Canvas Overlays    |
|  - ReportDetailScreen             | - ContrastAnalyzer Engine         |
|  - FocusPathScreen                | - VoiceCommandManager             |
+-----------------------------------+-----------------------------------+
|                    SQLite Offline Storage & Exporters                 |
|  - DatabaseHelper (Sessions, Issues, Nodes)                           |
|  - ReportExporter (HTML, PDF, SVG, Markdown)                          |
+-----------------------------------------------------------------------+
```

---

## 📱 Getting Started & Building from Source

### Prerequisites
* **JDK**: Version 17+ (e.g. Eclipse Adoptium OpenJDK 17)
* **Android SDK**: API 36 (Android 15+)
* **Gradle**: 8.7+ (Wrapper included)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/accessitestai/AMASAMYA.git
cd AMASAMYA/amasamya

# Run Unit Tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug

# Build Signed Production Release App Bundle (.aab)
./gradlew bundleRelease
```

The compiled App Bundle will be located at:  
`amasamya/app/build/outputs/bundle/release/app-release.aab`

---

## 🔒 Privacy & Data Safety

* **100% Offline & Local**: All layout hierarchy parsing, color contrast sampling, report compilation, and speech recognition execute strictly on your local device.
* **Zero Network Analytics**: AMASAMYA contains **no analytics SDKs, no trackers, and no remote server uploads**.
* **Accessibility Service Transparency**: The Accessibility Service API is exclusively used to read layout node boundaries, text content, and contrast parameters for auditing.

---

## 🤝 Contributing & License

Contributions are welcome! Please feel free to submit issues, feature requests, or pull requests.

Distributed under the **Apache License 2.0**. See `LICENSE` for more information.

---
*Built with ❤️ for accessible Android applications.*

# ♿ AMASAMYA — User Manual & Auditor Handbook

Welcome to the **AMASAMYA User Manual**. AMASAMYA is a blind-first, offline-first mobile accessibility auditing utility for Android. This guide provides step-by-step instructions for accessibility auditors, QA testers, and developers.

---

## 📖 Table of Contents
1. [Prerequisites & System Permissions](#1-prerequisites--system-permissions)
2. [First-Time Onboarding](#2-first-time-onboarding)
3. [Running Accessibility Audits](#3-running-accessibility-audits)
4. [Real-Time Diagnostic Tools](#4-real-time-diagnostic-tools)
5. [Configuring Compliance Standards](#5-configuring-compliance-standards)
6. [Applying One-Click Remediation Fixes](#6-applying-one-click-remediation-fixes)
7. [Hands-Free Voice Commands](#7-hands-free-voice-commands)
8. [Guided TalkBack Simulator Mode](#8-guided-talkback-simulator-mode)
9. [Exporting Reports](#9-exporting-reports)
10. [ADB Command Line Bridge (Port 8080)](#10-adb-command-line-bridge-port-8080)
11. [Troubleshooting Matrix](#11-troubleshooting-matrix)
12. [Data Privacy & Offline Security](#12-data-privacy--offline-security)

---

## 1. Prerequisites & System Permissions

* **Android Version**: Android 7.0 (API 24) or higher (Optimized for Android 14/15+).
* **Accessibility Service API**: Required to inspect screen node boundaries, text labels, and color contrast.
* **Microphone Permission**: Optional, required only if **Hands-Free Voice Commands** are enabled.

---

## 2. First-Time Onboarding

1. Launch **AMASAMYA** from your home screen.
2. Read the initial **Accessibility Permission Disclosure**.
3. Tap **Enable Service** to open Android Accessibility Settings.
4. Locate **AMASAMYA** under Installed Apps / Downloaded Services and toggle it **ON**.
5. Return to AMASAMYA. The status banner will display **Accessibility Service is Active** in green.

---

## 3. Running Accessibility Audits

### Method A: Floating Scan Button (Recommended)
1. Open the target Android app you wish to test.
2. Tap the floating **SCAN** overlay button on your screen.
3. AMASAMYA will capture the screen hierarchy and analyze nodes for compliance violations.

### Method B: Recording Audit Sessions
1. On the AMASAMYA Dashboard, enter a session title (e.g. *"Checkout Screen Audit"*).
2. Tap **Start New Session**.
3. Switch to the target application and perform normal TalkBack swipe navigation or screen interactions.
4. Tap **Scan Current Screen Now** or double-tap the floating button to capture screens.
5. Return to AMASAMYA and tap **Stop & Save Session**.

### Method C: Quick Status Notification Control
* Pull down your Android notification shade.
* Use the quick notification action buttons (**"Scan Screen"**, **"Hide Button" / "Show Button"**, **"Stop Session"**) without switching apps.

---

## 4. Real-Time Diagnostic Tools

Navigate to **Settings** -> **Real-Time Diagnostics Tools** to toggle dynamic overlays:

| Diagnostic Tool | Visual Indicator | Functionality |
| :--- | :--- | :--- |
| **Live Speech Captions** | Bottom visual text bubble | Renders real-time text captions for TalkBack spoken announcements for sighted auditors. |
| **Live Focus Trail** | Cyan vector path lines | Draws connecting path lines between consecutive focus points to trace TalkBack reading order. |
| **Touch Target Mapper** | Green / Yellow / Red rects | Highlights clickable views based on size compliance (Green >= 48dp, Yellow >= 24dp, Red < 24dp). |
| **Focus Loop Detector** | Warning banner + Haptic pulse | Detects repetitive focus traps and emits haptic warning pulses when navigation loops. |
| **Contrast Drift Scanner** | Warning text + Haptic pulse | Takes real-time dynamic screenshots on focus, computes color contrast ratios, and warns if below target standards. |

---

## 5. Configuring Compliance Standards

In **Settings** -> **Compliance Standard System**, select your target auditing standard:

* **WCAG 2.2** (Default W3C Standard): Level A, AA, or AAA.
* **Section 508**: US Federal Government Software & ICT standard.
* **EN 301 549**: European Union Accessibility Act standard (Mandates 44dp target sizes).

You can also customize **User Personas** (*Developer, Tester, Designer, Product Owner, General User*) to adjust the language of reported violations.

---

## 6. Applying One-Click Remediation Fixes

1. Open **Reports** and select any saved audit session.
2. Tap an issue card to expand technical details (class name, bounds, text content, WCAG criterion).
3. Under **Code Suggestions**, select your framework:
   * **Jetpack Compose Tab**: Copyable Kotlin modifier snippets (`contentDescription`, `defaultMinSize`, color hex values).
   * **Android XML Tab**: Copyable layout XML attributes (`android:contentDescription`, `android:minWidth`, `android:textColor`).
4. Tap **Copy Jira/GitHub Bug** to copy a pre-formatted markdown bug ticket ready for project management tracking.

---

## 7. Hands-Free Voice Commands

When **Hands-Free Voice Commands** is toggled ON in Settings, say any of the following commands:

* 🗣️ *"Scan screen"* / *"Scan"* — Executes a screen scan.
* 🗣️ *"Start session"* / *"Start recording"* — Begins an audit session.
* 🗣️ *"Stop session"* / *"Stop recording"* — Saves the session.
* 🗣️ *"Read summary"* / *"Status"* — Speaks session issue metrics via TTS.

---

## 8. Guided TalkBack Simulator Mode

For developers testing screen reader navigation without turning on system-wide TalkBack gestures:

1. Enable **Guided Screen Reader Simulator** in Settings.
2. A bottom control bar and cyan focus ring will appear over any active app.
3. Tap **PREV (◀)** or **NEXT (▶)** to move focus sequentially.
4. Tap **ACTIVATE** to trigger clicks. AMASAMYA speaks node descriptions aloud while showing the visual focus box.

---

## 9. Exporting Reports

From the **Report Details** screen, choose your export format:

* 🌐 **HTML Report**: Full dark-mode interactive HTML file with embedded SVG focus maps.
* 📄 **PDF Document**: Structured executive summary document.
* 📝 **Markdown File**: Text report for documentation.
* 🎨 **SVG Focus Path Map**: Vector diagram representing focus reading order.

All exported files are saved to your device's Documents folder and can be shared via standard Android sharing options.

---

## 10. ADB Command Line Bridge (Port 8080)

AMASAMYA includes a local ADB server for automated CLI extraction:

1. Enable **ADB Report Server** in Settings.
2. Connect your device via USB / ADB and set up port forwarding:
   ```bash
   adb forward tcp:8080 tcp:8080
   ```
3. Fetch the latest audit results via curl or HTTP:
   ```bash
   curl http://localhost:8080/report
   ```

---

## 11. Troubleshooting Matrix

| Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| **Floating button not visible** | Display over other apps permission off | Open Android Settings -> Apps -> Special App Access -> Display Over Other Apps -> Enable AMASAMYA. |
| **Voice commands not responding** | Microphone permission missing | Open App Info -> Permissions -> Allow Microphone access. |
| **Overlay drawing lag** | Battery saver mode active | Exclude AMASAMYA from battery optimization settings. |

---

## 12. Data Privacy & Offline Security

AMASAMYA operates **100% offline**:
* Zero internet permission required for auditing engines.
* No telemetry, trackers, or remote server uploads.
* All reports, screenshots, and database files remain encrypted and private on your device.

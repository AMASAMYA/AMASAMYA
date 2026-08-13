package com.example.amasamya.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "amasamya_settings"
        private const val KEY_WCAG_LEVEL = "wcag_level"
        private const val KEY_AUDIO_FEEDBACK = "audio_feedback"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        private const val KEY_REPORT_PERSONA = "report_persona"
        private const val KEY_FLOATING_BUTTON = "floating_button"
        private const val KEY_ADB_SERVER = "adb_server"
        private const val KEY_SHOW_ONBOARDING = "show_onboarding"
        private const val KEY_LIVE_CAPTIONS = "live_captions"
        private const val KEY_LIVE_FOCUS_TRAIL = "live_focus_trail"
        private const val KEY_TOUCH_TARGET_MAPPER = "touch_target_mapper"
        private const val KEY_FOCUS_TRAP_DETECTOR = "focus_trap_detector"
        private const val KEY_CONTRAST_DRIFT_SCANNER = "contrast_drift_scanner"
        private const val KEY_LAST_RUN_VERSION_CODE = "last_run_version_code"
        private const val KEY_COMPLIANCE_STANDARD = "compliance_standard"
        private const val KEY_MIN_TOUCH_TARGET_DP = "min_touch_target_dp"
        private const val KEY_VOICE_COMMANDS = "voice_commands"
        private const val KEY_SIMULATOR_MODE = "simulator_mode"

        const val LEVEL_A = "A"
        const val LEVEL_AA = "AA"
        const val LEVEL_AAA = "AAA"

        const val STANDARD_WCAG_2_2 = "WCAG 2.2"
        const val STANDARD_SECTION_508 = "Section 508"
        const val STANDARD_EN_301_549 = "EN 301 549"
        const val STANDARD_GIGW_3_0 = "GIGW 3.0"
        const val STANDARD_IS_17802 = "IS 17802"
        const val STANDARD_INDIA_NATIONAL = "India National Baseline"

        const val PERSONA_DEVELOPER = "Developer"
        const val PERSONA_TESTER = "Tester"
        const val PERSONA_DESIGNER = "Designer"
        const val PERSONA_PRODUCT_OWNER = "Product Owner"
        const val PERSONA_GENERAL_USER = "General User"
        const val KEY_CVD_SIMULATION = "cvd_simulation"

        const val CVD_NONE = "None"
        const val CVD_PROTANOPIA = "Protanopia"
        const val CVD_DEUTERANOPIA = "Deuteranopia"
        const val CVD_TRITANOPIA = "Tritanopia"
        const val CVD_MONOCHROMACY = "Monochromacy"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var cvdSimulationMode: String
        get() = prefs.getString(KEY_CVD_SIMULATION, CVD_NONE) ?: CVD_NONE
        set(value) {
            prefs.edit().putString(KEY_CVD_SIMULATION, value).apply()
        }

    var wcagLevel: String
        get() = prefs.getString(KEY_WCAG_LEVEL, LEVEL_AA) ?: LEVEL_AA
        set(value) {
            prefs.edit().putString(KEY_WCAG_LEVEL, value).apply()
        }

    var isAudioFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_FEEDBACK, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUDIO_FEEDBACK, value).apply()
        }

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) {
            prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()
        }

    var reportPersona: String
        get() = prefs.getString(KEY_REPORT_PERSONA, PERSONA_GENERAL_USER) ?: PERSONA_GENERAL_USER
        set(value) {
            prefs.edit().putString(KEY_REPORT_PERSONA, value).apply()
        }

    var isFloatingButtonEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BUTTON, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FLOATING_BUTTON, value).apply()
        }

    var isAdbServerEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADB_SERVER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ADB_SERVER, value).apply()
        }

    var showOnboarding: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ONBOARDING, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_ONBOARDING, value).apply()
        }

    var isLiveCaptionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_LIVE_CAPTIONS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LIVE_CAPTIONS, value).apply()
        }

    var isLiveFocusTrailEnabled: Boolean
        get() = prefs.getBoolean(KEY_LIVE_FOCUS_TRAIL, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LIVE_FOCUS_TRAIL, value).apply()
        }

    var isTouchTargetMapperEnabled: Boolean
        get() = prefs.getBoolean(KEY_TOUCH_TARGET_MAPPER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TOUCH_TARGET_MAPPER, value).apply()
        }

    var isFocusTrapDetectorEnabled: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_TRAP_DETECTOR, false)
        set(value) {
            prefs.edit().putBoolean(KEY_FOCUS_TRAP_DETECTOR, value).apply()
        }

    var isContrastDriftScannerEnabled: Boolean
        get() = prefs.getBoolean(KEY_CONTRAST_DRIFT_SCANNER, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CONTRAST_DRIFT_SCANNER, value).apply()
        }

    var lastRunVersionCode: Int
        get() = prefs.getInt(KEY_LAST_RUN_VERSION_CODE, -1)
        set(value) {
            prefs.edit().putInt(KEY_LAST_RUN_VERSION_CODE, value).apply()
        }

    var complianceStandard: String
        get() = prefs.getString(KEY_COMPLIANCE_STANDARD, STANDARD_WCAG_2_2) ?: STANDARD_WCAG_2_2
        set(value) {
            prefs.edit().putString(KEY_COMPLIANCE_STANDARD, value).apply()
        }

    var minTouchTargetDp: Int
        get() = prefs.getInt(KEY_MIN_TOUCH_TARGET_DP, 48)
        set(value) {
            prefs.edit().putInt(KEY_MIN_TOUCH_TARGET_DP, value).apply()
        }

    var isVoiceCommandsEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_COMMANDS, false)
        set(value) {
            prefs.edit().putBoolean(KEY_VOICE_COMMANDS, value).apply()
        }

    var isSimulatorModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SIMULATOR_MODE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SIMULATOR_MODE, value).apply()
        }
}

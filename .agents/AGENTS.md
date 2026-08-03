# Project Rules - AMASAMYA

The following rules apply to all development and maintenance tasks within this project:

## 1. Play Store Release Process
*   **Action**: Every time a new production build (`.aab` bundle) is compiled, the agent MUST provide clear, step-by-step instructions explaining how to upload the package to the Google Play Console Closed Testing track.
*   **Steps to Include**: Detail the upload location, track navigation steps in Play Console, and reminders for versioning.

## 2. Accessibility & Screen Reader Compliance
*   **Action**: Every newly implemented feature or visual control MUST be designed, coded, and verified to be fully accessible and usable with screen readers (such as TalkBack or Select-to-Speak). 
*   **Standards**:
    *   Purely decorative visual overlays must be hidden from screen readers using `importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO`.
    *   Overlay screens must use appropriate `WindowManager` flags (`FLAG_NOT_FOCUSABLE` and `FLAG_NOT_TOUCHABLE`) to allow gesture swipe-through to underlying elements.
    *   Interactive controls in UI screens must use merged semantics for clear announcements and correct role definitions.
    *   No compromise on accessibility is permitted.

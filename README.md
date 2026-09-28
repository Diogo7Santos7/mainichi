# Mynichi

A native Android habit and schedule organiser built with Kotlin, Jetpack Compose, and Material 3. Android 8.0 (API 26) or newer.

## Open in Android Studio

1. Choose **Open** and select this `mainichi` folder.
2. Let Gradle sync. Use **JDK 17** under Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK. Android Studio can download a JDK from this selector.
3. Install **Android SDK Platform 35** and **Build-Tools 35.0.0** through SDK Manager if requested.
4. Select an emulator or connected Android phone and press **Run**.

The project uses Android Gradle Plugin 8.9.2, Gradle 8.13, Kotlin 2.1.20, and Compose BOM 2025.04.01. These versions are pinned for reproducible builds. Internet access is required for the first Gradle sync.

## Using the app

Open the top-left drawer to switch between:

- **Daily routine:** habits such as stretching, brushing teeth, or skincare, repeated every day from their chosen start date.
- **Work & study:** dated tasks, with Work, Study, Chores, and Personal categories.
- **Appointments:** dated events with a time, duration, notes, and optional location.

Use the arrows to move between days, tap the date to open a calendar picker, or tap **Today**. Each section is sorted by start time. Use the add button to create a plan, tap a card to edit it, or use its menu to delete it. Tap the circle to mark it complete or undo completion. The progress card reflects the selected section and date.

The app starts empty. **Try a sample routine & tasks** optionally adds a small editable starter set. Appointments are left empty so there are no pretend commitments.

Daily habits keep completion history by calendar date; completing Monday never completes Tuesday. Today updates while the app is open and when it resumes. Habit edits apply to the entire recurring habit, including earlier dates. Deleting a habit removes its history after confirmation. Moving a dated plan to another date clears its completion state.

## Storage and scope

All plans are stored in an atomic JSON file in the app's private storage. Writes run off the UI thread and the UI only confirms changes after a successful save. Read failures preserve the saved file and show a retry action. No account, server, analytics, or internet permission is required by the app. Data survives restarts; uninstalling or clearing app storage removes it. Android backup is disabled.

This first version includes daily recurrence and individual dated plans. It does not include alarms, notification reminders, cloud sync, calendar integration, custom weekday recurrence, or overlapping-task detection. Times use the device's local calendar date and a 24-hour clock. Durations are descriptive and may extend past midnight.

## Code map

- `MainActivity.kt`: theme, drawer, timeline, day navigation, editor, and save coordination.
- `Task.kt`: models, recurrence, completion, chronological filtering, and optional sample data.
- `TaskStore.kt`: atomic local persistence.
- `TaskTest.kt`: unit tests for recurrence, day-isolated completion, dated plans, and chronological sorting.

## Command-line checks

With JDK 17 and the Android SDK configured (`ANDROID_HOME` or an untracked `local.properties`):

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

On macOS/Linux use `sh gradlew testDebugUnitTest assembleDebug lintDebug`.

The debug APK is generated in `app/build/outputs/apk/debug/`. It is for local testing, not a signed Play Store release.

### Verification for this delivery

- Kotlin compilation and `assembleDebug`: passed.
- `testDebugUnitTest`: 5 tests passed, 0 failures.
- `lintDebug`: 0 errors; 3 informational warnings that newer dependency versions are available.
- Device/emulator interaction testing: not performed. Use the manual checks below when you first run the app.

The supplied debug APK uses a temporary development signing key. Android Studio generates its own debug key, so uninstall the supplied APK before installing a locally built APK if Android reports a signature mismatch. Uninstalling clears local plans.

## Manual acceptance checks

1. Create two habits in reverse time order; verify chronological ordering.
2. Complete a habit, go to tomorrow, and verify it is incomplete. Return to the first day to see its saved completion.
3. Add a work task and an appointment on different dates; verify each appears only on its own date and section.
4. Restart the app and verify saved plans and completion history.
5. Edit and delete items, including cancelling a deletion.
6. Rotate the device while editing and check the form is preserved. Test a small screen, large system text, and TalkBack.
7. Leave today's view open across midnight and check it advances to the new date.

## References

- [Android Gradle Plugin 8.9 compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [Compose compiler plugin setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)

# Contributing to Vesper

Thank you for contributing to Vesper!

## Development Setup

1. Ensure **JDK 21**, **Android SDK 36**, and **Gradle 9.3.1** are installed (or open the project directly in Android Studio).
2. Copy `.env.example` only if testing optional custom environment keys; Vesper requires **no API keys** out of the box.
3. Run tests before opening a pull request:
   ```bash
   gradle :app:testDebugUnitTest
   gradle :app:lintDebug
   ```

## Coding Guidelines

- **Design System:** Follow the Geist / Material 3 tokens in `ui/theme/` (`Color.kt`, `Type.kt`, `Theme.kt`). Avoid hardcoded hex colors inside composables; read `MaterialTheme.colorScheme`.
- **Performance:** Keep scroll lists and sensor updates zero-allocation. Prefer `graphicsLayer { ... }` and `drawWithCache` for motion effects and honor `reducedMotionEnabled`.
- **Haptics:** Use `rememberLauncherHaptics()` with semantic `LauncherHapticEvent` values rather than adding `VIBRATE` permissions.
- **UI Test Tags:** Assign descriptive `snake_case` `Modifier.testTag(...)` identifiers to interactive elements and verify user flows with Robolectric Compose UI tests in `app/src/test/`.
- **Accessibility:** Ensure interactive touch targets meet `48.dp` minimum size and provide meaningful `contentDescription` strings.

## Updating Screenshots

If you update the visual UI of the Home Screen, App Drawer, or Settings, regenerate the README screenshots cleanly via Robolectric Native Graphics:

```bash
GENERATE_SCREENSHOTS=true gradle :app:testDebugUnitTest --tests "com.example.ScreenshotGeneratorTest"
```

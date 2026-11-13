# Changelog

All notable changes to **Vesper** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-13

### Added
- **Vercel / Geist Design System**: Crisp dark and light palettes, `1dp` hairline borders, `8dp`/`12dp` modal & card geometry, bundled `Inter` and `JetBrains Mono` typography, and five customizable accent themes (`Emerald`, `Mono`, `Purple`, `Blue`, `Orange`).
- **Synthwave Digital Clock Widget**: Multi-layer neon cyan/magenta glow readout, horizon spectrum beam, tap-to-clock integration, and battery-aware Reduced Motion support.
- **Gyroscope Sunset Parallax**: Multi-plane GPU `graphicsLayer` tilt parallax across the sunset backdrop, solar bloom, shoreline glow, and weather shader (`TYPE_GYROSCOPE` with `TYPE_GRAVITY` / `TYPE_ACCELEROMETER` fallback).
- **Adaptive Monochrome Outlines & Disk Cache**: Automatic perimeter outline extraction inside selectable frame shapes (`Rounded`, `Squircle`, `Circle`, `Teardrop`) persisted in `cacheDir/outline_icons/` for fast cold starts.
- **Command-K (`⌘K`) App Drawer**: Instant search, inline arithmetic calculator (`MathEvaluator`) with tap-to-copy, phone/URL quick actions, app shortcuts, web search fallback, and a height-adaptive `A–Z` alphabet scrubber rail.
- **Expandable Home Folders & Gestures**: Inline folders with long-press menus, configurable swipe-left / swipe-right / double-tap actions, and swipe-down notification shade expansion.
- **Focus Mode & Mindful Launch Pause**: Timed `15/25/45/60m` sessions, quiet-hours schedules (including overnight ranges), Essential Apps filtering, daily launch counts, mindful breathing pause dialog, and Today's Mindful Summary insights.
- **Live Open-Meteo Weather**: Zero-key weather & geocoding lookup with persistent offline JSON cache and time-of-day coastal palette transitions.
- **API-Aware Tactile Haptics**: Distinct `View.performHapticFeedback` triggers for drawer swipes, app launches, long-presses, `A–Z` scrubbing, toggles, and confirmations without `VIBRATE` permissions.
- **JSON Backup & Restore**: Export, copy, share, and import full launcher settings as portable JSON.
- **CI/CD & Semantic Versioning**: Automated GitHub Actions workflows for unit/UI tests, Android Lint, debug APKs, and tagged Release `.apk`/`.aab` + SHA-256 checksum publishing.

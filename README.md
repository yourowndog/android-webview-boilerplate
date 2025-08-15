# Daemon Portal

Daemon Portal is a single-activity WebView app written in Kotlin. It loads a configurable home URL in desktop mode and supports file uploads, microphone and camera access, and back navigation.

## Configuration

Set the `HOME_URL` value in [`app/build.gradle.kts`](app/build.gradle.kts). By default it points to:

```
https://chatgpt.com/g/g-XXXXXXXXXXXXXXX-the-daemon
```

## Build

This repository stores no Gradle wrapper JAR. CI uses the official [Gradle build action](https://github.com/gradle/gradle-build-action) to download Gradle and build the project.

Every push builds a Debug APK. Navigate to **Actions → Artifacts** and download `daemon-portal-debug-apk` to get the APK.

To build locally with a preinstalled Gradle 8.9 or newer:

```bash
gradle assembleDebug
```

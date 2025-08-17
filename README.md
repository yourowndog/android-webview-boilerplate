# Daemon Portal

Minimal Android WebView wrapper that opens the [Daemon GPT](https://chatgpt.com/g/g-68320ed4e74081919f11e7d6a993ee44-the-daemon) by default.
It runs in desktop mode, supports file uploads, camera and microphone access, back navigation, and login fallback.

## Start URL

The default start URL is:

```
https://chatgpt.com/g/g-68320ed4e74081919f11e7d6a993ee44-the-daemon
```

To change it, open the app menu (⋮) → **Set Portal URL** and enter any `https://` link.

## Build

This repository stores no binary artifacts. The Gradle wrapper JAR is generated in CI.
Every push or pull request runs [`android.yml`](.github/workflows/android.yml) and uploads a Debug APK.
Download it from **Actions → android → Artifacts**.

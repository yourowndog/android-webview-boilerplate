# Daemon Portal

Minimal Android WebView wrapper for the Daemon GPT. It opens the portal at default URL below and, if you are logged out, redirects through ChatGPT login before returning.

## Download

GitHub Actions workflow `build-android.yml` builds a debug APK for every push or pull request. Download it from **Actions → build-android → Artifacts**.

## Start URL

Default:

```
https://chatgpt.com/g/g-68320ed4e74081919f11e7d6a993ee44-the-daemon
```

To change it, open the app menu (⋮) → **Set Portal URL** and enter any `https://` link.

## Project layout

- `settings.gradle.kts`, `build.gradle.kts` – Gradle setup
- `app/src/main/AndroidManifest.xml` – manifest with `PortalActivity`
- `app/src/main/java/com/daemon/portal/PortalActivity.kt` – main activity
- `app/src/main/res` – minimal resources
- `.github/workflows` – CI (`build-android.yml`, `pr-guardrails.yml`)

## CI

- `build-android.yml` ensures the Gradle wrapper and builds the debug APK.
- `pr-guardrails.yml` blocks commits that introduce binary blobs.

## Contributing

Only text files are accepted. Let CI build and test changes; do not commit build outputs or binaries.

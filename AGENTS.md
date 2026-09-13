# Project Rules for LapWalker

## 1. Automatic Version Bumping (MANDATORY)
Whenever ANY feature, fix, or code modification is made in this project:
- **`versionCode`** must be incremented by `+1`.
- **`versionName`** must be bumped in `app/build.gradle`.
- Never build or release an APK without updating the version number first, because the app has an active GitHub in-app updater that flags any non-bumped build as outdated.
- Always overwrite `LapWalker.apk` in the root folder with the freshly compiled APK.

## 2. Mandatory Release Details on Completion (MANDATORY)
After completing ANY fix, feature, or change, always output:
1. **GitHub Release Link** (direct URL: `https://github.com/tahsinislamayan5-jpg/LapWalker-/releases/new`)
2. **Tag** (matching the new bumped `versionName`, e.g., `v2.6`)
3. **Release Title**
4. **Markdown Release Description** formatted and ready to copy-paste into GitHub.
5. **Path to `LapWalker.apk`** in the workspace root.

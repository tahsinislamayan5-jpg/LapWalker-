# Automatic Version Bump Rule

Whenever ANY changes, feature additions, UI modifications, or bug fixes are made to this codebase:

1. **Always Increment Version Before Build**:
   - In `app/build.gradle`:
     - Increment `versionCode` by `+1` (integer).
     - Bump `versionName` appropriately (e.g., `2.3` -> `2.4`, or patch `2.3.1`).
2. **Never Build Stale Versions**:
   - The in-app updater checks against GitHub releases (`releases/latest`).
   - If the version is not bumped, the installed app will flag itself as outdated.
3. **Always Copy Fresh APK**:
   - After running `assembleDebug` or `assembleRelease`, copy the compiled output directly to the workspace root as `LapWalker.apk`.

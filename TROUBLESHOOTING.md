# Troubleshooting Guide

## Build Issues

### Gradle Configuration Error
If you see: `Failed to notify project evaluation listener`

**Solution:**
```bash
# Clean and rebuild
cd WallScanner3D
./gradlew clean
./gradlew build
```

### Dependency Resolution Issues
If dependencies fail to download:

**Solution:**
1. Check internet connection
2. Clear Gradle cache:
   ```bash
   ./gradlew clean --refresh-dependencies
   ```
3. Sync project in Android Studio: File → Sync Project with Gradle Files

### Android SDK Not Found
If you see: `SDK location not found`

**Solution:**
1. Open Android Studio
2. File → Project Structure → SDK Location
3. Set Android SDK location
4. Or edit `local.properties`:
   ```
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
   ```

### Kotlin Version Mismatch
If you see Kotlin compatibility errors:

**Solution:**
Update `build.gradle.kts`:
```kotlin
plugins {
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
}
```

## Runtime Issues

### ARCore Not Available
**Error:** "AR Session failed"

**Solutions:**
1. Install ARCore from Play Store
2. Check device compatibility: https://developers.google.com/ar/devices
3. Update Google Play Services

### Camera Permission Denied
**Error:** App crashes on launch

**Solutions:**
1. Grant camera permission in Settings → Apps → Wall Scanner 3D
2. Reinstall app
3. Check AndroidManifest.xml has camera permission

### No Walls Detected
**Issue:** Scanning doesn't detect walls

**Solutions:**
1. Improve lighting (>300 lux)
2. Point at plain, solid-colored walls
3. Move 2-3 meters from wall
4. Move camera slowly
5. Ensure ARCore is tracking (look for plane indicators)

### App Crashes During Scanning
**Issue:** App force closes while scanning

**Solutions:**
1. Clear app data: Settings → Apps → Wall Scanner 3D → Clear Data
2. Restart device
3. Check available RAM (need 2GB+)
4. Reduce FRAME_SKIP in MainActivity.kt

## Performance Issues

### Slow Scanning
**Issue:** App is laggy or slow

**Solutions:**
1. Close other apps
2. Increase FRAME_SKIP value:
   ```kotlin
   private val FRAME_SKIP = 10 // Process every 10th frame
   ```
3. Disable element detection temporarily
4. Use a newer device

### High Battery Drain
**Issue:** Battery drains quickly

**Solutions:**
1. Reduce scanning time
2. Lower screen brightness
3. Close app when not in use
4. Increase FRAME_SKIP value

## Export Issues

### Export Fails
**Issue:** Cannot export model

**Solutions:**
1. Grant storage permission (Android 9 and below)
2. Check available storage space
3. Try different export format
4. Check logs for specific error

### Exported File Not Found
**Issue:** Can't find exported file

**Solution:**
Files are saved to:
```
/Android/data/com.wallscanner.pro/files/
```

Access via:
1. File manager app
2. Connect to PC via USB
3. Android Studio Device File Explorer

## Development Issues

### Build Fails in Android Studio
**Solutions:**
1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.gradle` and `.idea` folders
3. Reimport project
4. Update Android Studio to latest version

### SceneView Errors
If you see SceneView-related errors:

**Solution:**
Check version compatibility:
```kotlin
implementation("io.github.sceneview:arsceneview:0.10.0")
```

### TensorFlow Lite Errors
If TFLite model fails to load:

**Solution:**
1. Model file is optional
2. App will use heuristic classification
3. To add model: place at `app/src/main/assets/wall_elements_model.tflite`

## Getting Help

### Check Logs
```bash
adb logcat | grep WallScanner
```

### Enable Debug Mode
In `MainActivity.kt`:
```kotlin
private val DEBUG = true
```

### Report Issues
Include:
1. Device model and Android version
2. Error message or crash log
3. Steps to reproduce
4. Screenshots if applicable

### Contact
- GitHub Issues: [Create Issue]
- Email: support@wallscanner3d.com

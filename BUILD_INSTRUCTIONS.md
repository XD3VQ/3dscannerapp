# Build Instructions

## Quick Build (Recommended)

### Using Android Studio
1. **Open Project**
   ```
   File → Open → Select WallScanner3D folder
   ```

2. **Wait for Gradle Sync**
   - Android Studio will automatically sync
   - This may take 2-5 minutes on first run
   - Downloads all dependencies

3. **Connect Device**
   - Enable USB Debugging on your Android device
   - Connect via USB
   - Accept debugging prompt on device

4. **Run**
   - Click green "Run" button (▶)
   - Or press Shift+F10
   - Select your device
   - App will install and launch

## Command Line Build

### Windows
```cmd
cd WallScanner3D
gradlew.bat assembleDebug
gradlew.bat installDebug
```

### Mac/Linux
```bash
cd WallScanner3D
./gradlew assembleDebug
./gradlew installDebug
```

## Build Variants

### Debug Build (Development)
```bash
./gradlew assembleDebug
```
- Includes debugging symbols
- Larger APK size (~60 MB)
- Not optimized

### Release Build (Production)
```bash
./gradlew assembleRelease
```
- Optimized and minified
- Smaller APK size (~40 MB)
- Requires signing key

## Troubleshooting Build Issues

### Issue: Gradle sync fails
**Solution:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Issue: SDK not found
**Solution:**
Create/edit `local.properties`:
```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### Issue: Out of memory
**Solution:**
Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m
```

### Issue: Dependency download fails
**Solution:**
1. Check internet connection
2. Try different network
3. Clear Gradle cache:
   ```bash
   rm -rf ~/.gradle/caches/
   ```

## First Time Setup

### 1. Install Prerequisites
- **Android Studio**: Download from https://developer.android.com/studio
- **JDK 17**: Included with Android Studio
- **Android SDK**: Install via Android Studio SDK Manager

### 2. Configure SDK
```
Android Studio → Settings → Appearance & Behavior → System Settings → Android SDK
```
Install:
- Android 13.0 (API 33)
- Android 8.0 (API 26)
- Android SDK Build-Tools
- Android SDK Platform-Tools

### 3. Setup Device
Enable Developer Options:
1. Settings → About Phone
2. Tap "Build Number" 7 times
3. Settings → Developer Options
4. Enable "USB Debugging"

### 4. Verify ARCore
- Install ARCore from Play Store
- Check device compatibility: https://developers.google.com/ar/devices

## Build Output Locations

### Debug APK
```
WallScanner3D/app/build/outputs/apk/debug/app-debug.apk
```

### Release APK
```
WallScanner3D/app/build/outputs/apk/release/app-release.apk
```

## Install APK Manually

### Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via File Transfer
1. Copy APK to device
2. Open file manager on device
3. Tap APK file
4. Allow "Install from Unknown Sources"
5. Install

## Build Configuration

### Minimum Requirements
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 13 (API 33)
- **Compile SDK**: Android 13 (API 33)

### Dependencies
All dependencies are automatically downloaded by Gradle:
- ARCore 1.40.0
- TensorFlow Lite 2.13.0
- ML Kit 17.0.1
- SceneView 0.10.0
- CameraX 1.3.0

### Build Time
- **First build**: 5-10 minutes (downloads dependencies)
- **Incremental builds**: 30-60 seconds
- **Clean builds**: 2-3 minutes

## Performance Tips

### Faster Builds
1. Enable Gradle daemon:
   ```properties
   org.gradle.daemon=true
   ```

2. Enable parallel builds:
   ```properties
   org.gradle.parallel=true
   ```

3. Enable build cache:
   ```properties
   org.gradle.caching=true
   ```

### Reduce APK Size
1. Enable ProGuard (already configured)
2. Use APK splits for different architectures
3. Remove unused resources

## Signing Release Build

### Generate Keystore
```bash
keytool -genkey -v -keystore wallscanner.keystore -alias wallscanner -keyalg RSA -keysize 2048 -validity 10000
```

### Configure Signing
Edit `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("wallscanner.keystore")
            storePassword = "your_password"
            keyAlias = "wallscanner"
            keyPassword = "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Build Signed APK
```bash
./gradlew assembleRelease
```

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run on Device
```bash
./gradlew connectedAndroidTest
```

### Check Code Quality
```bash
./gradlew lint
```

## Common Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug on device
./gradlew installDebug

# Uninstall from device
./gradlew uninstallDebug

# List all tasks
./gradlew tasks

# Build with stacktrace
./gradlew build --stacktrace

# Build with debug info
./gradlew build --debug
```

## Next Steps

After successful build:
1. Read QUICKSTART.md for usage guide
2. Check TROUBLESHOOTING.md if issues occur
3. See README.md for full documentation

## Support

If build fails:
1. Check TROUBLESHOOTING.md
2. Run with `--stacktrace` flag
3. Check Android Studio Build Output
4. Create GitHub issue with error log

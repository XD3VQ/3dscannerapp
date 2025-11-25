# Quick Start Guide - Wall Scanner 3D Pro

## 🎯 What This App Does

Wall Scanner 3D Pro uses your phone's camera and AI to:
1. **Scan walls** in real-time using AR
2. **Detect elements** like outlets, switches, windows, and doors automatically
3. **Build 3D models** of complete rooms
4. **Apply textures** to visualize different materials
5. **Export models** for use in other 3D software

## 📱 Quick Setup (5 minutes)

### Step 1: Check Device Compatibility
- Your device must support ARCore
- Check here: https://developers.google.com/ar/devices
- Common compatible devices: Pixel 3+, Samsung S8+, OnePlus 6+

### Step 2: Build the App
```bash
# Open in Android Studio
File → Open → Select WallScanner3D folder

# Or build from command line
cd WallScanner3D
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Step 3: Grant Permissions
- Camera: Required for AR scanning
- Storage: Required for exporting models

## 🎬 First Scan (2 minutes)

### Scan Your First Wall

1. **Launch the app**
   - You'll see the AR camera view

2. **Point at a wall**
   - Hold phone vertically
   - Stand 2-3 meters from wall
   - Ensure good lighting

3. **Tap the scan button** (blue circle)
   - Status changes to "Scanning wall 1..."

4. **Move slowly across the wall**
   - Pan left to right
   - Keep wall in frame
   - App detects outlets, switches automatically

5. **Tap scan button again to pause**
   - Wall is now saved
   - Elements are highlighted

### Scan Adjacent Walls

6. **Move to next wall**
   - Turn 90 degrees
   - Point at adjacent wall

7. **Resume scanning**
   - Tap scan button
   - App automatically detects connection

8. **Complete the room**
   - Scan 3-4 walls
   - App shows "Room complete!" when enclosed

## 🎨 Customize & Export

### Apply Textures
```
1. Tap "Textures" button
2. Choose from 12 materials
3. Texture applies to last scanned wall
```

### View 3D Model
```
1. Tap "View 3D" button
2. Rotate with slider
3. Toggle X-ray mode
4. Show element labels
```

### Export Model
```
1. Tap "Export" button
2. Choose format:
   - OBJ: For Blender, Maya, 3ds Max
   - JSON: For web applications
   - GLTF: For web 3D viewers
   - PLY: For point cloud software
3. File saved to: /Android/data/com.wallscanner.pro/files/
```

## 💡 Tips for Best Results

### Lighting
- ✅ Bright, even lighting (natural daylight best)
- ✅ 300+ lux recommended
- ❌ Avoid direct sunlight causing glare
- ❌ Avoid very dark rooms

### Wall Conditions
- ✅ Plain, solid-colored walls
- ✅ Clean, uncluttered surfaces
- ❌ Highly reflective surfaces (mirrors, glass)
- ❌ Very dark or black walls

### Scanning Technique
- ✅ Move slowly and steadily
- ✅ Keep wall in center of frame
- ✅ Maintain 2-3 meter distance
- ❌ Don't move too fast
- ❌ Don't get too close (<1 meter)

### Element Detection
- ✅ Outlets: Best detected at 0.3-0.5m height
- ✅ Switches: Best detected at 1.2-1.5m height
- ✅ Windows: Ensure full window is visible
- ✅ Doors: Scan from side to capture full frame

## 🐛 Troubleshooting

### "AR Session Failed"
- Restart app
- Ensure ARCore is installed (Play Store)
- Check device compatibility

### "No Walls Detected"
- Improve lighting
- Move closer to wall (2-3 meters)
- Ensure wall is plain and visible
- Try different wall

### "Elements Not Detected"
- Move slower across wall
- Ensure elements are visible
- Check lighting on elements
- Elements may be too small/far

### "Room Not Complete"
- Scan at least 3 walls
- Ensure walls are adjacent
- Walls must share corners
- Try rescanning connections

## 📊 Understanding the UI

### Top Bar
- **Status Text**: Current scanning state
- **Walls**: Number of walls detected
- **Elements**: Total outlets, switches, etc.

### Bottom Controls
- **Scan Button (FAB)**: Start/stop scanning
- **Textures**: Apply materials to walls
- **View 3D**: Open 3D model viewer
- **Export**: Save model to file
- **Reset**: Clear all and start over

### Visual Indicators
- **Crosshair**: Aim at wall center
- **Blue overlay**: Wall detected
- **Green boxes**: Elements detected
- **Progress bar**: Processing

## 🎓 Advanced Usage

### Custom TFLite Model
```
1. Train your own element detection model
2. Export as TFLite format
3. Place at: app/src/main/assets/wall_elements_model.tflite
4. Rebuild app
```

### Adjust Detection Sensitivity
```kotlin
// In AdvancedWallDetector.kt
private val CONFIDENCE_THRESHOLD = 0.7f // Lower = more detections

// In ElementClassifier.kt
private fun isOutlet(...) {
    return size.width < 0.15f // Adjust size thresholds
}
```

### Export to Specific Software

**Blender**
```
1. Export as OBJ
2. Blender → File → Import → Wavefront (.obj)
3. Materials are preserved
```

**Unity**
```
1. Export as GLTF
2. Unity → Assets → Import Package
3. Drag GLTF into scene
```

**Web Viewer**
```
1. Export as GLTF
2. Use Three.js or Babylon.js
3. Load with GLTFLoader
```

## 📈 Performance Tips

### For Older Devices
```kotlin
// In MainActivity.kt
private val FRAME_SKIP = 10 // Process every 10th frame (default: 5)
```

### For Better Accuracy
```kotlin
private val FRAME_SKIP = 3 // Process more frames
```

### Memory Management
- Reset scanning after each room
- Export and clear before starting new room
- Close app completely if sluggish

## 🎯 Example Workflow

**Scanning a Bedroom**
```
1. Start at door wall → Scan (30 seconds)
2. Move to left wall → Scan (30 seconds)
3. Move to window wall → Scan (30 seconds)
4. Move to right wall → Scan (30 seconds)
5. App shows "Room complete!"
6. Apply wood texture to walls
7. Export as OBJ
8. Import into Blender for rendering
```

## 📞 Need Help?

- **Documentation**: See README.md
- **Issues**: GitHub Issues
- **Email**: support@wallscanner3d.com

## 🚀 Next Steps

1. ✅ Complete your first room scan
2. ✅ Try different textures
3. ✅ Export and view in 3D software
4. ✅ Scan multiple rooms
5. ✅ Experiment with custom models

Happy Scanning! 🎉

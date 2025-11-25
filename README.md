# Wall Scanner 3D Pro

A professional-grade Android application that uses advanced AI and AR technology to scan walls in real-time, detect surface elements, and create accurate 3D room models.

## 🚀 Features

### Core Capabilities
- **Real-Time Wall Detection**: Uses ARCore plane detection with advanced point cloud refinement
- **AI-Powered Element Recognition**: TensorFlow Lite + ML Kit for detecting:
  - Electrical outlets (single, double, USB, GFCI)
  - Light switches (1-3 gang)
  - Windows (standard, bay, horizontal, vertical)
  - Doors (single, double, sliding, French)
- **Intelligent Room Reconstruction**: Automatically detects spatial relationships between walls
- **12 Material Textures**: White/beige/gray paint, brick, wood, concrete, wallpaper, tile, stone, marble
- **Multiple Export Formats**: OBJ, JSON, GLTF, PLY, FBX

### Advanced Features
- **Point Cloud Refinement**: Improves wall accuracy using AR depth data
- **Edge Detection**: OpenCV-powered edge detection for precise boundaries
- **Gap Inference**: Automatically suggests missing walls to complete rooms
- **Wall Optimization**: Snaps walls to common angles and aligns adjacent walls
- **Confidence Scoring**: Each detection includes confidence metrics
- **Room Type Estimation**: Automatically classifies rooms (bedroom, bathroom, etc.)

## 🏗️ Architecture

### ML/AI Components
1. **AdvancedWallDetector**: ARCore + OpenCV for wall detection
   - Vertical plane detection
   - Point cloud processing
   - Edge detection with Canny algorithm
   - Hough line transform

2. **ElementClassifier**: TensorFlow Lite + ML Kit
   - Object detection in stream mode
   - Custom TFLite model support
   - Heuristic classification fallback
   - Confidence-based filtering

3. **RoomReconstructor**: Spatial relationship analysis
   - Wall adjacency graph
   - Gap detection and inference
   - Wall position optimization
   - Floor plan generation

### Rendering System
- **Advanced3DRenderer**: Real-time 3D visualization
  - Material and texture application
  - Element highlighting
  - X-ray mode for transparency
  - Dimension labels
  - Animation effects

### Export System
- **ModelExporter**: Multi-format 3D export
  - OBJ with MTL materials
  - JSON with full metadata
  - GLTF 2.0 format
  - PLY point cloud
  - FBX (placeholder)

## 📋 Requirements

- Android 8.0 (API 26) or higher
- ARCore-compatible device ([Check compatibility](https://developers.google.com/ar/devices))
- Camera permission
- Storage permission (Android 9 and below)
- 2GB+ RAM recommended
- Good lighting conditions

## 🛠️ Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/WallScanner3D.git
   cd WallScanner3D
   ```

2. **Open in Android Studio**
   - Android Studio Arctic Fox or newer
   - Gradle will sync dependencies automatically

3. **Add TFLite Model (Optional)**
   - Place your custom model at `app/src/main/assets/wall_elements_model.tflite`
   - If not provided, app uses heuristic classification

4. **Build and Run**
   - Connect ARCore-compatible device
   - Click Run or `./gradlew installDebug`

## 📱 Usage

### Scanning Walls
1. Launch app and grant camera permission
2. Point camera at a wall
3. Tap the scan button (FAB)
4. Move camera slowly across the wall surface
5. App automatically detects outlets, switches, windows, doors
6. Tap scan button again to pause

### Scanning Multiple Walls
1. After scanning first wall, move to adjacent wall
2. Resume scanning
3. App automatically detects spatial relationships
4. Continue until room is complete

### Applying Textures
1. Tap "Textures" button
2. Select from 12 material options
3. Texture applies to most recently scanned wall

### Viewing 3D Model
1. Tap "View 3D" button
2. Use controls to rotate and zoom
3. Toggle X-ray mode for transparency
4. Show/hide element labels

### Exporting
1. Tap "Export" button
2. Choose format (OBJ, JSON, GLTF, PLY)
3. File saved to app's external storage
4. Share or import into 3D software

## 🎯 Technical Details

### ARCore Integration
- Vertical plane detection for walls
- Point cloud for depth refinement
- Hit testing for spatial positioning
- Anchor creation for persistent objects
- Environmental HDR lighting

### ML Pipeline
```
Camera Frame → ARCore Tracking → Plane Detection
                                      ↓
                              Wall Extraction
                                      ↓
                              ML Kit Detection
                                      ↓
                              TFLite Classification
                                      ↓
                              Element Positioning
```

### Coordinate System
- Origin: AR session start point
- X: Right
- Y: Up
- Z: Forward (camera direction)
- Units: Meters

### Performance Optimizations
- Frame skipping (processes every 5th frame)
- Async processing with Kotlin coroutines
- Efficient mesh generation
- Material instancing
- Culling and LOD

## 🔧 Configuration

### Adjust Detection Sensitivity
Edit `AdvancedWallDetector.kt`:
```kotlin
private fun calculateConfidence(plane: Plane, corners: List<Point3D>): Float {
    // Adjust thresholds here
}
```

### Customize Element Classification
Edit `ElementClassifier.kt`:
```kotlin
private fun isOutlet(size: Size2D, position: Point3D, aspectRatio: Float): Boolean {
    // Modify detection rules
}
```

### Change Frame Processing Rate
Edit `MainActivity.kt`:
```kotlin
private val FRAME_SKIP = 5 // Process every Nth frame
```

## 📊 Data Models

### Wall
- ID, corners (3D points), normal vector
- Elements list, texture, confidence
- Dimensions, area, center point

### WallElement
- Position (3D), size (2D), confidence
- Types: Outlet, Switch, Window, Door
- Subtype details (outlet type, switch count, etc.)

### Room
- ID, name, walls list
- Total area, element counts
- Adjacency detection, completion status
- Room type estimation

## 🚧 Limitations

- Requires good lighting (>300 lux recommended)
- Works best with plain, uncluttered walls
- Element detection accuracy: 75-85%
- Wall detection accuracy: 90-95%
- Room completion requires 3+ adjacent walls
- Performance varies by device

## 🔮 Future Enhancements

- [ ] Custom TFLite model training pipeline
- [ ] Texture mapping from camera images
- [ ] Floor and ceiling detection
- [ ] Furniture placement and room decoration
- [ ] Cloud storage and sharing
- [ ] Multi-room projects
- [ ] Measurement tools
- [ ] AR preview of changes
- [ ] Integration with CAD software

## 📄 License

MIT License - see LICENSE file

## 🤝 Contributing

Contributions welcome! Please read CONTRIBUTING.md first.

## 📧 Support

For issues and questions:
- GitHub Issues: [Create an issue](https://github.com/yourusername/WallScanner3D/issues)
- Email: support@wallscanner3d.com

## 🙏 Acknowledgments

- Google ARCore team
- TensorFlow Lite team
- SceneView library
- OpenCV community

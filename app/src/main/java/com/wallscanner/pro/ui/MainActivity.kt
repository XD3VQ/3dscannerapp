package com.wallscanner.pro.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.*
import com.wallscanner.pro.R
import com.wallscanner.pro.export.ModelExporter
import com.wallscanner.pro.ml.AdvancedWallDetector
import com.wallscanner.pro.ml.ElementClassifier
import com.wallscanner.pro.model.*
import com.wallscanner.pro.reconstruction.RoomReconstructor
import com.wallscanner.pro.rendering.Advanced3DRenderer
import io.github.sceneview.ar.ArSceneView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var arSceneView: ArSceneView
    private lateinit var statusText: TextView
    private lateinit var wallCountText: TextView
    private lateinit var elementCountText: TextView
    private lateinit var scanButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var textureButton: Button
    private lateinit var view3dButton: Button
    private lateinit var exportButton: Button
    private lateinit var resetButton: Button
    private lateinit var progressBar: ProgressBar
    
    // Core Components
    private lateinit var wallDetector: AdvancedWallDetector
    private lateinit var elementClassifier: ElementClassifier
    private lateinit var renderer: Advanced3DRenderer
    private lateinit var roomReconstructor: RoomReconstructor
    private lateinit var modelExporter: ModelExporter
    
    // State
    private var currentRoom = Room(id = UUID.randomUUID().toString(), name = "My Room")
    private var isScanning = false
    private var currentWall: Wall? = null
    private val detectedWalls = mutableListOf<Wall>()
    private var frameCount = 0
    private val FRAME_SKIP = 5 // Process every 5th frame
    
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Check ARCore availability first
        if (!checkARCoreAvailability()) {
            return
        }
        
        try {
            initializeComponents()
            initializeViews()
            checkPermissions()
            setupARScene()
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun checkARCoreAvailability(): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            if (availability.isTransient) {
                // Still checking, allow to continue
                return true
            }
            
            when (availability) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    // ARCore is installed and supported
                    true
                }
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    // Request ARCore installation
                    try {
                        ArCoreApk.getInstance().requestInstall(this, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    Toast.makeText(
                        this,
                        "ARCore is not supported on this device",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    false
                }
                else -> {
                    // Unknown status, allow to continue
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't block on ARCore check errors, let it fail later with better error
            true
        }
    }
    
    private fun initializeComponents() {
        wallDetector = AdvancedWallDetector()
        elementClassifier = ElementClassifier(this)
        roomReconstructor = RoomReconstructor()
        modelExporter = ModelExporter(this)
    }
    
    private fun initializeViews() {
        arSceneView = findViewById(R.id.arSceneView)
        statusText = findViewById(R.id.statusText)
        wallCountText = findViewById(R.id.wallCountText)
        elementCountText = findViewById(R.id.elementCountText)
        scanButton = findViewById(R.id.scanButton)
        textureButton = findViewById(R.id.textureButton)
        view3dButton = findViewById(R.id.view3dButton)
        exportButton = findViewById(R.id.exportButton)
        resetButton = findViewById(R.id.resetButton)
        progressBar = findViewById(R.id.progressBar)
        
        renderer = Advanced3DRenderer(this, arSceneView)
    }
    
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                CAMERA_PERMISSION_CODE
            )
        }
    }
    
    private fun setupARScene() {
        try {
            arSceneView.apply {
                planeRenderer.isEnabled = true
                planeRenderer.isVisible = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "AR Scene setup failed: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Process frames manually - simplified approach
        arSceneView.onFrame = fun(_) {
            try {
                val session = arSceneView.arSession ?: return
                
                // Show plane detection status
                val allPlanes = session.getAllTrackables(com.google.ar.core.Plane::class.java)
                val trackingPlanes = allPlanes.filter { 
                    it.trackingState == com.google.ar.core.TrackingState.TRACKING 
                }
                val verticalPlanes = trackingPlanes.filter { 
                    it.type == com.google.ar.core.Plane.Type.VERTICAL 
                }
                
                // Update status text with plane info
                if (!isScanning) {
                    if (verticalPlanes.isEmpty()) {
                        runOnUiThread {
                            statusText.text = "Point camera at a wall. Move slowly to detect surfaces."
                        }
                    } else {
                        runOnUiThread {
                            statusText.text = "${verticalPlanes.size} wall surface(s) detected. Tap SCAN to capture."
                        }
                    }
                    return
                }
                
                frameCount++
                if (frameCount % FRAME_SKIP == 0) {
                    // Process detected planes directly from session
                    if (verticalPlanes.isNotEmpty()) {
                        lifecycleScope.launch {
                            processPlanes(verticalPlanes, session)
                        }
                    } else {
                        runOnUiThread {
                            statusText.text = "Scanning... Move camera slowly across the wall"
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore frame processing errors
            }
        }
    }
    
    private fun setupListeners() {
        scanButton.setOnClickListener {
            toggleScanning()
        }
        
        textureButton.setOnClickListener {
            showTextureSelector()
        }
        
        view3dButton.setOnClickListener {
            view3DModel()
        }
        
        exportButton.setOnClickListener {
            showExportDialog()
        }
        
        resetButton.setOnClickListener {
            resetScanning()
        }
    }
    
    private fun toggleScanning() {
        isScanning = !isScanning
        
        if (isScanning) {
            scanButton.setImageResource(R.drawable.ic_scan)
            statusText.text = getString(R.string.scanning, currentRoom.walls.size + 1)
            Toast.makeText(this, "Move camera slowly across the wall", Toast.LENGTH_SHORT).show()
        } else {
            scanButton.setImageResource(R.drawable.ic_scan)
            statusText.text = "Scan paused. ${currentRoom.walls.size} wall(s) detected"
            
            if (currentRoom.isComplete()) {
                statusText.text = getString(R.string.room_complete, currentRoom.walls.size)
                Toast.makeText(this, "Room enclosure detected!", Toast.LENGTH_LONG).show()
                
                // Reconstruct room
                lifecycleScope.launch {
                    reconstructRoom()
                }
            }
        }
    }
    
    private suspend fun processPlanes(planes: Collection<com.google.ar.core.Plane>, session: com.google.ar.core.Session) {
        withContext(Dispatchers.Default) {
            try {
                // Create walls from detected planes
                val walls = planes.filter { it.type == com.google.ar.core.Plane.Type.VERTICAL }
                    .mapNotNull { plane ->
                        try {
                            createWallFromPlane(plane)
                        } catch (e: Exception) {
                            null
                        }
                    }
                
                if (walls.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        processDetectedWalls(walls)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun createWallFromPlane(plane: com.google.ar.core.Plane): Wall? {
        val centerPose = plane.centerPose
        val corners = mutableListOf<Point3D>()
        
        // Simple corner extraction
        val extentX = plane.extentX
        val extentZ = plane.extentZ
        
        corners.add(Point3D(centerPose.tx() - extentX/2, centerPose.ty(), centerPose.tz() - extentZ/2))
        corners.add(Point3D(centerPose.tx() + extentX/2, centerPose.ty(), centerPose.tz() - extentZ/2))
        corners.add(Point3D(centerPose.tx() + extentX/2, centerPose.ty() + 2.5f, centerPose.tz() - extentZ/2))
        corners.add(Point3D(centerPose.tx() - extentX/2, centerPose.ty() + 2.5f, centerPose.tz() - extentZ/2))
        
        val zAxis = centerPose.zAxis
        val normal = Vector3D(zAxis[0], zAxis[1], zAxis[2]).normalize()
        
        return Wall(
            id = java.util.UUID.randomUUID().toString(),
            corners = corners,
            normal = normal,
            confidence = 0.8f,
            planeId = plane.hashCode().toString()
        )
    }
    
    private fun processDetectedWalls(walls: List<Wall>) {
        try {
            for (wall in walls) {
                val isNewWall = !isWallAlreadyDetected(wall)
                
                if (isNewWall) {
                    currentRoom.addWall(wall)
                    detectedWalls.add(wall)
                    currentWall = wall
                    
                    // Render the wall immediately
                    try {
                        renderer.renderWall(wall, null)
                        Toast.makeText(this, "Wall ${currentRoom.walls.size} detected!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    updateUI()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun processFrame_OLD(frame: com.google.ar.core.Frame, session: com.google.ar.core.Session) {
        try {
            // Detect walls from AR frame
            val walls = wallDetector.detectWallsFromFrame(frame, session)
            
            if (walls.isNotEmpty()) {
                lifecycleScope.launch {
                    processDetectedWalls(walls, frame)
                }
            }
            
            // Update point cloud for refinement
            frame.acquirePointCloud().use { pointCloud ->
                currentWall?.let { wall ->
                    val refinedWall = wallDetector.refineWallWithPointCloud(wall, pointCloud)
                    currentWall = refinedWall
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun processDetectedWalls(walls: List<Wall>, frame: Frame) {
        withContext(Dispatchers.Default) {
            for (wall in walls) {
                // Check if this is a new wall
                val isNewWall = !isWallAlreadyDetected(wall)
                
                if (isNewWall) {
                    // Detect elements on the wall
                    val bitmap = captureFrameBitmap(frame)
                    val elements = elementClassifier.detectElements(
                        bitmap,
                        wall.getBounds(),
                        0f
                    )
                    
                    wall.elements.addAll(elements)
                    
                    // Add to room
                    currentRoom.addWall(wall)
                    detectedWalls.add(wall)
                    currentWall = wall
                    
                    // Render the wall
                    withContext(Dispatchers.Main) {
                        renderNewWall(wall, frame)
                        updateUI()
                    }
                }
            }
        }
    }
    
    private fun isWallAlreadyDetected(wall: Wall): Boolean {
        return detectedWalls.any { existingWall ->
            // Check if walls are similar based on position and normal
            val centerDistance = existingWall.getCenter().distanceTo(wall.getCenter())
            val normalSimilarity = existingWall.normal.dot(wall.normal)
            
            centerDistance < 0.5f && normalSimilarity > 0.9f
        }
    }
    
    private fun renderNewWall(wall: Wall, frame: Frame) {
        try {
            val hitResults = frame.hitTest(
                arSceneView.width / 2f,
                arSceneView.height / 2f
            )
            
            hitResults.firstOrNull()?.let { hit ->
                if (hit.trackable is Plane) {
                    val anchor = hit.createAnchor()
                    renderer.renderWall(wall, anchor)
                    renderer.animateWallAppearance(wall.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun captureFrameBitmap(frame: Frame): Bitmap {
        // Simplified - in production, properly capture AR camera frame
        return try {
            val image = frame.acquireCameraImage()
            val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
            image.close()
            bitmap
        } catch (e: Exception) {
            Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        }
    }
    
    private fun updateUI() {
        wallCountText.text = "Walls: ${currentRoom.walls.size}"
        elementCountText.text = "Elements: ${currentRoom.getTotalElements()}"
        
        if (currentRoom.walls.isNotEmpty()) {
            statusText.text = "Detected ${currentRoom.walls.size} wall(s)"
        }
        
        val elementsInLastWall = currentRoom.walls.lastOrNull()?.elements?.size ?: 0
        if (elementsInLastWall > 0) {
            statusText.text = getString(R.string.elements_detected, elementsInLastWall)
        }
    }
    
    private suspend fun reconstructRoom() {
        showProgress(true)
        
        withContext(Dispatchers.Default) {
            val reconstructedRoom = roomReconstructor.reconstructRoom(currentRoom.walls)
            currentRoom = reconstructedRoom
            
            withContext(Dispatchers.Main) {
                showProgress(false)
                Toast.makeText(
                    this@MainActivity,
                    "Room reconstructed: ${reconstructedRoom.walls.size} walls",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun showTextureSelector() {
        if (currentRoom.walls.isEmpty()) {
            Toast.makeText(this, "Scan a wall first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val textures = WallTexture.values()
        val textureNames = textures.map { it.displayName }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Select Wall Texture")
            .setItems(textureNames) { _, which ->
                applyTextureToLastWall(textures[which])
            }
            .show()
    }
    
    private fun applyTextureToLastWall(texture: WallTexture) {
        currentRoom.walls.lastOrNull()?.let { wall ->
            wall.texture = texture
            renderer.updateWallTexture(wall.id, texture)
            Toast.makeText(this, "Applied ${texture.displayName}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun view3DModel() {
        if (currentRoom.walls.isEmpty()) {
            Toast.makeText(this, "No walls to view", Toast.LENGTH_SHORT).show()
            return
        }
        
        val intent = Intent(this, ModelViewerActivity::class.java)
        // In production, pass room data via Intent or ViewModel
        startActivity(intent)
    }
    
    private fun showExportDialog() {
        if (currentRoom.walls.isEmpty()) {
            Toast.makeText(this, "No walls to export", Toast.LENGTH_SHORT).show()
            return
        }
        
        val formats = arrayOf("OBJ", "JSON", "GLTF", "PLY")
        
        AlertDialog.Builder(this)
            .setTitle("Export Model")
            .setItems(formats) { _, which ->
                val format = when (which) {
                    0 -> ExportFormat.OBJ
                    1 -> ExportFormat.JSON
                    2 -> ExportFormat.GLTF
                    3 -> ExportFormat.PLY
                    else -> ExportFormat.OBJ
                }
                exportModel(format)
            }
            .show()
    }
    
    private fun exportModel(format: ExportFormat) {
        showProgress(true)
        
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    modelExporter.exportRoom(currentRoom, format)
                }
                
                showProgress(false)
                Toast.makeText(
                    this@MainActivity,
                    "Exported to ${file.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                showProgress(false)
                Toast.makeText(
                    this@MainActivity,
                    "Export failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun resetScanning() {
        AlertDialog.Builder(this)
            .setTitle("Reset Scanning")
            .setMessage("This will clear all scanned walls. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                currentRoom = Room(id = UUID.randomUUID().toString(), name = "My Room")
                detectedWalls.clear()
                currentWall = null
                isScanning = false
                renderer.clear()
                updateUI()
                statusText.text = getString(R.string.point_at_wall)
                Toast.makeText(this, "Scanning reset", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        arSceneView.onPause(this)
    }
    
    override fun onResume() {
        super.onResume()
        try {
            arSceneView.onResume(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "ARCore session failed. Use a physical device with ARCore support.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        elementClassifier.release()
        renderer.clear()
        arSceneView.destroy()
    }
}

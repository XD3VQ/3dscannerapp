package com.wallscanner.pro.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.wallscanner.pro.model.*

class ElementClassifier(private val context: Context) {
    
    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }
    
    suspend fun detectElements(
        bitmap: Bitmap,
        wallBounds: RectF,
        wallDepth: Float
    ): List<WallElement> {
        return try {
            detectWithMLKit(bitmap, wallBounds, wallDepth)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun detectWithMLKit(
        bitmap: Bitmap,
        wallBounds: RectF,
        wallDepth: Float
    ): List<WallElement> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val elements = mutableListOf<WallElement>()
        
        try {
            val detectedObjects = Tasks.await(objectDetector.process(image))
            
            for (obj in detectedObjects) {
                val boundingBox = obj.boundingBox
                
                val centerX = (boundingBox.centerX() / bitmap.width) * wallBounds.width() + wallBounds.left
                val centerY = (boundingBox.centerY() / bitmap.height) * wallBounds.height() + wallBounds.top
                
                val position = Point3D(centerX, centerY, wallDepth)
                
                val width = (boundingBox.width() / bitmap.width.toFloat()) * wallBounds.width()
                val height = (boundingBox.height() / bitmap.height.toFloat()) * wallBounds.height()
                val size = Size2D(width, height)
                
                val element = classifyElement(position, size, boundingBox, obj.labels)
                element?.let { elements.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return elements
    }
    
    private fun classifyElement(
        position: Point3D,
        size: Size2D,
        boundingBox: android.graphics.Rect,
        labels: List<com.google.mlkit.vision.objects.DetectedObject.Label>
    ): WallElement? {
        val aspectRatio = size.width / size.height
        
        return when {
            isOutlet(size, position, aspectRatio) -> {
                WallElement.Outlet(position, size, OutletType.SINGLE, 0.8f)
            }
            isSwitch(size, position, aspectRatio) -> {
                WallElement.Switch(position, size, 1, 0.75f)
            }
            isWindow(size, position, aspectRatio) -> {
                WallElement.Window(position, size, WindowType.STANDARD, 0.85f)
            }
            isDoor(size, position, aspectRatio) -> {
                WallElement.Door(position, size, DoorType.SINGLE, 0.9f)
            }
            else -> null
        }
    }
    
    private fun isOutlet(size: Size2D, position: Point3D, aspectRatio: Float): Boolean {
        return size.width < 0.15f && size.height < 0.15f && position.y < 0.6f
    }
    
    private fun isSwitch(size: Size2D, position: Point3D, aspectRatio: Float): Boolean {
        return size.width < 0.2f && size.height < 0.25f && position.y > 1.0f && position.y < 1.6f
    }
    
    private fun isWindow(size: Size2D, position: Point3D, aspectRatio: Float): Boolean {
        return size.width > 0.4f && size.height > 0.4f && position.y > 0.8f
    }
    
    private fun isDoor(size: Size2D, position: Point3D, aspectRatio: Float): Boolean {
        return size.height > 1.8f && size.width > 0.7f && size.width < 1.2f && position.y < 1.0f
    }
    
    fun release() {
        objectDetector.close()
    }
}

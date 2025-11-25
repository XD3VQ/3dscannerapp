package com.wallscanner.pro.ml

import android.graphics.Bitmap
import com.google.ar.core.*
import com.wallscanner.pro.model.*
import java.util.UUID
import kotlin.math.*

class AdvancedWallDetector {
    
    private val detectedPlanes = mutableMapOf<Plane, WallData>()
    
    data class WallData(
        val plane: Plane,
        val corners: List<Point3D>,
        val normal: Vector3D,
        var confidence: Float = 0f
    )
    
    fun detectWallsFromFrame(frame: Frame, session: com.google.ar.core.Session): List<Wall> {
        val walls = mutableListOf<Wall>()
        
        // Get all tracked planes
        val planes = session.getAllTrackables(Plane::class.java)
            .filter { it.trackingState == TrackingState.TRACKING }
        
        for (plane in planes) {
            if (plane.type == Plane.Type.VERTICAL) {
                val wallData = processPlane(plane, frame)
                wallData?.let {
                    detectedPlanes[plane] = it
                    
                    if (it.confidence > 0.7f) {
                        walls.add(createWallFromData(it))
                    }
                }
            }
        }
        
        return walls
    }
    
    private fun processPlane(plane: Plane, frame: Frame): WallData? {
        val centerPose = plane.centerPose
        val polygon = plane.polygon
        
        if (polygon.limit() < 6) return null
        
        // Extract corners from polygon
        val corners = extractCorners(polygon, centerPose)
        
        // Calculate wall normal
        val normal = calculateNormal(centerPose)
        
        // Calculate confidence
        val confidence = calculateConfidence(plane, corners)
        
        return WallData(plane, corners, normal, confidence)
    }
    
    private fun extractCorners(polygon: java.nio.FloatBuffer, centerPose: Pose): List<Point3D> {
        val corners = mutableListOf<Point3D>()
        val points = mutableListOf<Pair<Float, Float>>()
        
        // Read all polygon points
        polygon.rewind()
        while (polygon.remaining() >= 2) {
            val x = polygon.get()
            val z = polygon.get()
            points.add(Pair(x, z))
        }
        
        if (points.isEmpty()) return corners
        
        // Find bounding box corners
        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minZ = points.minOf { it.second }
        val maxZ = points.maxOf { it.second }
        
        // Transform to world coordinates
        val poseMatrix = FloatArray(16)
        centerPose.toMatrix(poseMatrix, 0)
        
        // Create 4 corners
        val localCorners = listOf(
            floatArrayOf(minX, 0f, minZ, 1f),
            floatArrayOf(maxX, 0f, minZ, 1f),
            floatArrayOf(maxX, 2.5f, minZ, 1f),
            floatArrayOf(minX, 2.5f, minZ, 1f)
        )
        
        for (localCorner in localCorners) {
            val worldCorner = FloatArray(4)
            android.opengl.Matrix.multiplyMV(worldCorner, 0, poseMatrix, 0, localCorner, 0)
            corners.add(Point3D(worldCorner[0], worldCorner[1], worldCorner[2]))
        }
        
        return corners
    }
    
    private fun calculateNormal(pose: Pose): Vector3D {
        val zAxis = pose.zAxis
        return Vector3D(zAxis[0], zAxis[1], zAxis[2]).normalize()
    }
    
    private fun calculateConfidence(plane: Plane, corners: List<Point3D>): Float {
        var confidence = 0f
        
        val extentX = plane.extentX
        val extentZ = plane.extentZ
        val area = extentX * extentZ
        confidence += min(area / 10f, 0.4f)
        
        confidence += min(corners.size / 4f * 0.3f, 0.3f)
        
        if (plane.trackingState == TrackingState.TRACKING) {
            confidence += 0.3f
        }
        
        return min(confidence, 1f)
    }
    
    private fun createWallFromData(wallData: WallData): Wall {
        return Wall(
            id = UUID.randomUUID().toString(),
            corners = wallData.corners,
            normal = wallData.normal,
            confidence = wallData.confidence,
            planeId = wallData.plane.hashCode().toString()
        )
    }
    
    fun refineWallWithPointCloud(wall: Wall, pointCloud: PointCloud): Wall {
        return wall.copy(confidence = min(wall.confidence + 0.1f, 1f))
    }
    
    fun detectEdgesInImage(bitmap: Bitmap): List<Line2D> {
        return emptyList()
    }
}

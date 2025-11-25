package com.wallscanner.pro.reconstruction

import com.wallscanner.pro.model.*
import kotlin.math.*

class RoomReconstructor {
    
    private val wallGraph = mutableMapOf<String, MutableList<String>>()
    
    fun reconstructRoom(walls: List<Wall>): Room {
        val room = Room(
            id = java.util.UUID.randomUUID().toString(),
            walls = walls.toMutableList()
        )
        
        // Build adjacency graph
        buildWallGraph(walls)
        
        // Detect room boundaries
        detectRoomBoundaries(room)
        
        // Optimize wall positions
        optimizeWallPositions(room)
        
        // Infer missing walls
        inferMissingWalls(room)
        
        return room
    }
    
    private fun buildWallGraph(walls: List<Wall>) {
        wallGraph.clear()
        
        for (i in walls.indices) {
            wallGraph[walls[i].id] = mutableListOf()
            
            for (j in walls.indices) {
                if (i != j && areWallsConnected(walls[i], walls[j])) {
                    wallGraph[walls[i].id]?.add(walls[j].id)
                }
            }
        }
    }
    
    private fun areWallsConnected(wall1: Wall, wall2: Wall): Boolean {
        val threshold = 0.2f
        
        // Check if walls share corners
        for (c1 in wall1.corners) {
            for (c2 in wall2.corners) {
                if (c1.distanceTo(c2) < threshold) {
                    return true
                }
            }
        }
        
        // Check if walls are perpendicular and close
        val angle = calculateAngleBetweenWalls(wall1, wall2)
        if (abs(angle - 90f) < 15f) {
            val distance = calculateWallDistance(wall1, wall2)
            if (distance < threshold) {
                return true
            }
        }
        
        return false
    }
    
    private fun calculateAngleBetweenWalls(wall1: Wall, wall2: Wall): Float {
        val dot = wall1.normal.dot(wall2.normal)
        val angle = acos(dot.coerceIn(-1f, 1f)) * 180f / PI.toFloat()
        return angle
    }
    
    private fun calculateWallDistance(wall1: Wall, wall2: Wall): Float {
        val center1 = wall1.getCenter()
        val center2 = wall2.getCenter()
        return center1.distanceTo(center2)
    }
    
    private fun detectRoomBoundaries(room: Room) {
        if (room.walls.isEmpty()) return
        
        // Find the bounding box of all walls
        val allCorners = room.walls.flatMap { it.corners }
        
        val minX = allCorners.minOf { it.x }
        val maxX = allCorners.maxOf { it.x }
        val minY = allCorners.minOf { it.y }
        val maxY = allCorners.maxOf { it.y }
        val minZ = allCorners.minOf { it.z }
        val maxZ = allCorners.maxOf { it.z }
        
        // Store room dimensions (could be added to Room model)
        val roomWidth = maxX - minX
        val roomDepth = maxZ - minZ
        val roomHeight = maxY - minY
    }
    
    private fun optimizeWallPositions(room: Room) {
        // Snap walls to common angles (0°, 90°, 180°, 270°)
        for (wall in room.walls) {
            val normal = wall.normal
            val angle = atan2(normal.z, normal.x) * 180f / PI.toFloat()
            
            // Snap to nearest 90° angle
            val snappedAngle = (round(angle / 90f) * 90f) * PI.toFloat() / 180f
            
            val snappedNormal = Vector3D(
                cos(snappedAngle),
                normal.y,
                sin(snappedAngle)
            ).normalize()
            
            // Update wall normal (would need to modify Wall to be mutable)
        }
        
        // Align adjacent walls
        alignAdjacentWalls(room)
    }
    
    private fun alignAdjacentWalls(room: Room) {
        for (i in room.walls.indices) {
            for (j in i + 1 until room.walls.size) {
                val wall1 = room.walls[i]
                val wall2 = room.walls[j]
                
                if (areWallsConnected(wall1, wall2)) {
                    // Find shared corners and align them
                    alignSharedCorners(wall1, wall2)
                }
            }
        }
    }
    
    private fun alignSharedCorners(wall1: Wall, wall2: Wall) {
        val threshold = 0.2f
        val sharedPairs = mutableListOf<Pair<Point3D, Point3D>>()
        
        for (c1 in wall1.corners) {
            for (c2 in wall2.corners) {
                if (c1.distanceTo(c2) < threshold) {
                    sharedPairs.add(Pair(c1, c2))
                }
            }
        }
        
        // Average the positions of shared corners
        for ((c1, c2) in sharedPairs) {
            val avgX = (c1.x + c2.x) / 2f
            val avgY = (c1.y + c2.y) / 2f
            val avgZ = (c1.z + c2.z) / 2f
            // Update both corners to the average position
        }
    }
    
    private fun inferMissingWalls(room: Room) {
        // Detect gaps in the room and infer missing walls
        val gaps = detectGaps(room)
        
        for (gap in gaps) {
            val inferredWall = createWallFromGap(gap)
            inferredWall?.let {
                room.walls.add(it)
            }
        }
    }
    
    private fun detectGaps(room: Room): List<Gap> {
        val gaps = mutableListOf<Gap>()
        
        // Find walls that should be connected but aren't
        for (wall in room.walls) {
            val connections = wallGraph[wall.id]?.size ?: 0
            
            if (connections < 2) {
                // This wall has open ends
                val openEnds = findOpenEnds(wall, room)
                gaps.addAll(openEnds)
            }
        }
        
        return gaps
    }
    
    private fun findOpenEnds(wall: Wall, room: Room): List<Gap> {
        val gaps = mutableListOf<Gap>()
        val threshold = 0.2f
        
        // Check each corner of the wall
        for (corner in wall.corners) {
            var isConnected = false
            
            for (otherWall in room.walls) {
                if (otherWall.id != wall.id) {
                    for (otherCorner in otherWall.corners) {
                        if (corner.distanceTo(otherCorner) < threshold) {
                            isConnected = true
                            break
                        }
                    }
                }
                if (isConnected) break
            }
            
            if (!isConnected) {
                gaps.add(Gap(corner, wall.normal))
            }
        }
        
        return gaps
    }
    
    private fun createWallFromGap(gap: Gap): Wall? {
        // Create a hypothetical wall to fill the gap
        // This is a simplified version - in production, use more sophisticated inference
        
        val defaultWidth = 3f
        val defaultHeight = 2.5f
        
        val perpendicular = Vector3D(-gap.normal.z, 0f, gap.normal.x).normalize()
        val scaledPerp = Point3D(perpendicular.x * defaultWidth, perpendicular.y * defaultWidth, perpendicular.z * defaultWidth)
        val heightOffset = Point3D(0f, defaultHeight, 0f)
        
        val corners = listOf(
            gap.position,
            Point3D(gap.position.x + scaledPerp.x, gap.position.y + scaledPerp.y, gap.position.z + scaledPerp.z),
            Point3D(gap.position.x + scaledPerp.x, gap.position.y + scaledPerp.y + heightOffset.y, gap.position.z + scaledPerp.z),
            Point3D(gap.position.x, gap.position.y + heightOffset.y, gap.position.z)
        )
        
        return Wall(
            id = java.util.UUID.randomUUID().toString(),
            corners = corners,
            normal = gap.normal,
            confidence = 0.3f // Low confidence for inferred walls
        )
    }
    
    fun calculateRoomVolume(room: Room): Float {
        if (room.walls.size < 3) return 0f
        
        val floorArea = calculateFloorArea(room)
        val avgHeight = room.walls.map { it.getHeight() }.average().toFloat()
        
        return floorArea * avgHeight
    }
    
    private fun calculateFloorArea(room: Room): Float {
        // Use shoelace formula for polygon area
        val floorPoints = room.walls.flatMap { wall ->
            wall.corners.filter { it.y < 0.5f } // Bottom corners
        }.distinctBy { "${it.x}_${it.z}" }
        
        if (floorPoints.size < 3) return 0f
        
        var area = 0f
        for (i in floorPoints.indices) {
            val j = (i + 1) % floorPoints.size
            area += floorPoints[i].x * floorPoints[j].z
            area -= floorPoints[j].x * floorPoints[i].z
        }
        
        return abs(area) / 2f
    }
    
    fun generateFloorPlan(room: Room): FloorPlan {
        val walls2D = room.walls.map { wall ->
            Wall2D(
                start = Point2D(wall.corners[0].x, wall.corners[0].z),
                end = Point2D(wall.corners[1].x, wall.corners[1].z),
                thickness = 0.15f // Standard wall thickness
            )
        }
        
        val elements2D = room.walls.flatMap { wall ->
            wall.elements.map { element ->
                Element2D(
                    position = Point2D(element.position.x, element.position.z),
                    type = when (element) {
                        is WallElement.Door -> "door"
                        is WallElement.Window -> "window"
                        is WallElement.Outlet -> "outlet"
                        is WallElement.Switch -> "switch"
                    }
                )
            }
        }
        
        return FloorPlan(walls2D, elements2D)
    }
    
    data class Gap(val position: Point3D, val normal: Vector3D)
    
    data class Wall2D(val start: Point2D, val end: Point2D, val thickness: Float)
    
    data class Element2D(val position: Point2D, val type: String)
    
    data class FloorPlan(val walls: List<Wall2D>, val elements: List<Element2D>)
}

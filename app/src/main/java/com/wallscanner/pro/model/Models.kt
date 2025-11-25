package com.wallscanner.pro.model

import android.graphics.RectF
import kotlin.math.sqrt

// 3D Point
data class Point3D(val x: Float, val y: Float, val z: Float) {
    fun distanceTo(other: Point3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    fun add(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    fun subtract(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)
    fun scale(factor: Float) = Point3D(x * factor, y * factor, z * factor)
}

// 2D Point
data class Point2D(val x: Float, val y: Float) {
    fun distanceTo(other: Point2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}

// 3D Vector
data class Vector3D(val x: Float, val y: Float, val z: Float) {
    fun normalize(): Vector3D {
        val length = sqrt(x * x + y * y + z * z)
        return if (length > 0) Vector3D(x / length, y / length, z / length) else this
    }
    
    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3D): Vector3D {
        return Vector3D(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }
    
    fun length(): Float = sqrt(x * x + y * y + z * z)
}

// 2D Size
data class Size2D(val width: Float, val height: Float) {
    fun area(): Float = width * height
}

// 2D Line
data class Line2D(val start: Point2D, val end: Point2D) {
    fun length(): Float = start.distanceTo(end)
    fun angle(): Float = kotlin.math.atan2(end.y - start.y, end.x - start.x)
}

// Wall
data class Wall(
    val id: String,
    val corners: List<Point3D>,
    val normal: Vector3D,
    val elements: MutableList<WallElement> = mutableListOf(),
    var texture: WallTexture = WallTexture.WHITE_PAINT,
    var confidence: Float = 0f,
    val planeId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getArea(): Float {
        if (corners.size < 3) return 0f
        val width = corners[0].distanceTo(corners[1])
        val height = corners[0].distanceTo(corners[3])
        return width * height
    }
    
    fun getWidth(): Float {
        return if (corners.size >= 2) corners[0].distanceTo(corners[1]) else 0f
    }
    
    fun getHeight(): Float {
        return if (corners.size >= 4) corners[0].distanceTo(corners[3]) else 2.5f
    }
    
    fun getCenter(): Point3D {
        if (corners.isEmpty()) return Point3D(0f, 0f, 0f)
        val sum = corners.fold(Point3D(0f, 0f, 0f)) { acc, p -> acc.add(p) }
        return sum.scale(1f / corners.size)
    }
    
    fun getBounds(): RectF {
        if (corners.isEmpty()) return RectF()
        val minX = corners.minOf { it.x }
        val maxX = corners.maxOf { it.x }
        val minY = corners.minOf { it.y }
        val maxY = corners.maxOf { it.y }
        return RectF(minX, minY, maxX, maxY)
    }
}

// Wall Elements
sealed class WallElement {
    abstract val position: Point3D
    abstract val size: Size2D
    abstract val confidence: Float
    
    data class Outlet(
        override val position: Point3D,
        override val size: Size2D = Size2D(0.1f, 0.1f),
        val type: OutletType = OutletType.SINGLE,
        override val confidence: Float = 0.8f
    ) : WallElement()
    
    data class Switch(
        override val position: Point3D,
        override val size: Size2D = Size2D(0.08f, 0.12f),
        val switchCount: Int = 1,
        override val confidence: Float = 0.75f
    ) : WallElement()
    
    data class Window(
        override val position: Point3D,
        override val size: Size2D,
        val windowType: WindowType = WindowType.STANDARD,
        override val confidence: Float = 0.85f
    ) : WallElement()
    
    data class Door(
        override val position: Point3D,
        override val size: Size2D,
        val doorType: DoorType = DoorType.SINGLE,
        override val confidence: Float = 0.9f
    ) : WallElement()
}

// Element Types
enum class OutletType {
    SINGLE, DOUBLE, USB, GFCI
}

enum class WindowType {
    STANDARD, HORIZONTAL, VERTICAL, BAY, SKYLIGHT
}

enum class DoorType {
    SINGLE, DOUBLE, SLIDING, FRENCH
}

// Wall Textures
enum class WallTexture(
    val displayName: String,
    val colorHex: String,
    val roughness: Float
) {
    WHITE_PAINT("White Paint", "#FFFFFF", 0.3f),
    BEIGE_PAINT("Beige Paint", "#F5F5DC", 0.3f),
    GRAY_PAINT("Gray Paint", "#808080", 0.3f),
    BRICK("Red Brick", "#B22222", 0.8f),
    WHITE_BRICK("White Brick", "#F0F0F0", 0.7f),
    WOOD("Wood Panels", "#8B4513", 0.5f),
    DARK_WOOD("Dark Wood", "#3E2723", 0.5f),
    CONCRETE("Concrete", "#A9A9A9", 0.6f),
    WALLPAPER("Wallpaper", "#FFE4E1", 0.2f),
    TILE("Ceramic Tile", "#E0E0E0", 0.1f),
    STONE("Stone", "#696969", 0.9f),
    MARBLE("Marble", "#F8F8FF", 0.2f)
}

// Room
data class Room(
    val id: String,
    val walls: MutableList<Wall> = mutableListOf(),
    var name: String = "Untitled Room",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun addWall(wall: Wall) {
        // Check if wall already exists
        val exists = walls.any { it.planeId == wall.planeId }
        if (!exists) {
            walls.add(wall)
            detectAdjacentWalls()
        }
    }
    
    fun getTotalArea(): Float {
        return walls.sumOf { it.getArea().toDouble() }.toFloat()
    }
    
    fun getTotalElements(): Int {
        return walls.sumOf { it.elements.size }
    }
    
    fun getElementCount(type: Class<out WallElement>): Int {
        return walls.sumOf { wall ->
            wall.elements.count { type.isInstance(it) }
        }
    }
    
    private fun detectAdjacentWalls() {
        // Detect which walls are adjacent based on shared corners
        for (i in walls.indices) {
            for (j in i + 1 until walls.size) {
                if (areWallsAdjacent(walls[i], walls[j])) {
                    // Walls are adjacent - could store this relationship
                }
            }
        }
    }
    
    private fun areWallsAdjacent(wall1: Wall, wall2: Wall): Boolean {
        val threshold = 0.15f // 15cm tolerance
        var sharedCorners = 0
        
        for (corner1 in wall1.corners) {
            for (corner2 in wall2.corners) {
                if (corner1.distanceTo(corner2) < threshold) {
                    sharedCorners++
                }
            }
        }
        
        return sharedCorners >= 2 // Adjacent walls share at least 2 corners (an edge)
    }
    
    fun isComplete(): Boolean {
        // A complete room should have at least 3 walls forming an enclosure
        return walls.size >= 3 && checkEnclosure()
    }
    
    private fun checkEnclosure(): Boolean {
        if (walls.size < 3) return false
        
        val adjacencyCount = mutableMapOf<Int, Int>()
        for (i in walls.indices) {
            adjacencyCount[i] = 0
            for (j in walls.indices) {
                if (i != j && areWallsAdjacent(walls[i], walls[j])) {
                    adjacencyCount[i] = adjacencyCount[i]!! + 1
                }
            }
        }
        
        // Each wall should be adjacent to at least 2 other walls for a closed room
        return adjacencyCount.values.count { it >= 2 } >= 3
    }
    
    fun estimateRoomType(): RoomType {
        val area = getTotalArea()
        val doorCount = getElementCount(WallElement.Door::class.java)
        val windowCount = getElementCount(WallElement.Window::class.java)
        
        return when {
            area < 10f && doorCount == 1 && windowCount == 0 -> RoomType.BATHROOM
            area < 15f && doorCount == 1 -> RoomType.BEDROOM
            area > 30f && windowCount > 2 -> RoomType.LIVING_ROOM
            doorCount == 0 && windowCount == 0 -> RoomType.CLOSET
            else -> RoomType.GENERIC
        }
    }
}

enum class RoomType {
    LIVING_ROOM, BEDROOM, BATHROOM, KITCHEN, DINING_ROOM, CLOSET, HALLWAY, GENERIC
}

// Export Format
data class RoomExport(
    val room: Room,
    val format: ExportFormat,
    val includeTextures: Boolean = true,
    val includeElements: Boolean = true
)

enum class ExportFormat {
    OBJ, FBX, GLTF, JSON, PLY
}

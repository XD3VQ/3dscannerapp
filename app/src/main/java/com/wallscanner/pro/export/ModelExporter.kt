package com.wallscanner.pro.export

import android.content.Context
import com.google.gson.GsonBuilder
import com.wallscanner.pro.model.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class ModelExporter(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    
    fun exportRoom(room: Room, format: ExportFormat, includeTextures: Boolean = true): File {
        val timestamp = dateFormat.format(Date())
        val filename = "${room.name.replace(" ", "_")}_$timestamp"
        
        return when (format) {
            ExportFormat.OBJ -> exportToOBJ(room, filename, includeTextures)
            ExportFormat.JSON -> exportToJSON(room, filename)
            ExportFormat.GLTF -> exportToGLTF(room, filename)
            ExportFormat.PLY -> exportToPLY(room, filename)
            ExportFormat.FBX -> exportToFBX(room, filename)
        }
    }
    
    fun exportToOBJ(room: Room, filename: String, includeTextures: Boolean = true): File {
        val objFile = File(context.getExternalFilesDir(null), "$filename.obj")
        val mtlFile = File(context.getExternalFilesDir(null), "$filename.mtl")
        
        val objWriter = FileWriter(objFile)
        val mtlWriter = if (includeTextures) FileWriter(mtlFile) else null
        
        // OBJ Header
        objWriter.append("# Wall Scanner 3D Pro Export\n")
        objWriter.append("# Room: ${room.name}\n")
        objWriter.append("# Walls: ${room.walls.size}\n")
        objWriter.append("# Elements: ${room.getTotalElements()}\n")
        objWriter.append("# Export Date: ${Date()}\n\n")
        
        if (includeTextures) {
            objWriter.append("mtllib $filename.mtl\n\n")
            writeMTLFile(mtlWriter!!, room)
        }
        
        var vertexOffset = 1
        var normalOffset = 1
        var texCoordOffset = 1
        
        // Export each wall
        room.walls.forEachIndexed { wallIndex, wall ->
            objWriter.append("# Wall ${wallIndex + 1}\n")
            objWriter.append("o Wall_${wall.id}\n")
            
            if (includeTextures) {
                objWriter.append("usemtl ${wall.texture.name}\n")
            }
            
            // Write vertices
            wall.corners.forEach { corner ->
                objWriter.append("v ${corner.x} ${corner.y} ${corner.z}\n")
            }
            
            // Write normals
            objWriter.append("vn ${wall.normal.x} ${wall.normal.y} ${wall.normal.z}\n")
            
            // Write texture coordinates
            objWriter.append("vt 0.0 0.0\n")
            objWriter.append("vt 1.0 0.0\n")
            objWriter.append("vt 1.0 1.0\n")
            objWriter.append("vt 0.0 1.0\n")
            
            // Write faces (quad as two triangles)
            if (wall.corners.size >= 4) {
                objWriter.append("f $vertexOffset/$texCoordOffset/$normalOffset ")
                objWriter.append("${vertexOffset + 1}/${texCoordOffset + 1}/$normalOffset ")
                objWriter.append("${vertexOffset + 2}/${texCoordOffset + 2}/$normalOffset\n")
                
                objWriter.append("f $vertexOffset/$texCoordOffset/$normalOffset ")
                objWriter.append("${vertexOffset + 2}/${texCoordOffset + 2}/$normalOffset ")
                objWriter.append("${vertexOffset + 3}/${texCoordOffset + 3}/$normalOffset\n")
                
                vertexOffset += 4
                texCoordOffset += 4
            }
            normalOffset += 1
            
            objWriter.append("\n")
            
            // Export wall elements
            wall.elements.forEachIndexed { elemIndex, element ->
                exportElementToOBJ(objWriter, element, wallIndex, elemIndex, vertexOffset, normalOffset)
                vertexOffset += 4
                normalOffset += 1
            }
        }
        
        objWriter.close()
        mtlWriter?.close()
        
        return objFile
    }
    
    private fun writeMTLFile(writer: FileWriter, room: Room) {
        writer.append("# Material Library\n\n")
        
        val uniqueTextures = room.walls.map { it.texture }.distinct()
        
        uniqueTextures.forEach { texture ->
            writer.append("newmtl ${texture.name}\n")
            writer.append("Ka 1.0 1.0 1.0\n") // Ambient
            writer.append("Kd ${hexToRGB(texture.colorHex)}\n") // Diffuse
            writer.append("Ks 0.5 0.5 0.5\n") // Specular
            writer.append("Ns ${(1 - texture.roughness) * 100}\n") // Shininess
            writer.append("d 1.0\n") // Opacity
            writer.append("illum 2\n\n")
        }
    }
    
    private fun hexToRGB(hex: String): String {
        val color = android.graphics.Color.parseColor(hex)
        val r = android.graphics.Color.red(color) / 255f
        val g = android.graphics.Color.green(color) / 255f
        val b = android.graphics.Color.blue(color) / 255f
        return "$r $g $b"
    }
    
    private fun exportElementToOBJ(
        writer: FileWriter,
        element: WallElement,
        wallIndex: Int,
        elemIndex: Int,
        vertexOffset: Int,
        normalOffset: Int
    ) {
        val elementType = when (element) {
            is WallElement.Outlet -> "Outlet"
            is WallElement.Switch -> "Switch"
            is WallElement.Window -> "Window"
            is WallElement.Door -> "Door"
        }
        
        writer.append("# $elementType ${elemIndex + 1}\n")
        writer.append("o ${elementType}_${wallIndex}_${elemIndex}\n")
        
        // Create simple box for element
        val pos = element.position
        val size = element.size
        val hw = size.width / 2
        val hh = size.height / 2
        
        // 4 corners of element
        writer.append("v ${pos.x - hw} ${pos.y - hh} ${pos.z}\n")
        writer.append("v ${pos.x + hw} ${pos.y - hh} ${pos.z}\n")
        writer.append("v ${pos.x + hw} ${pos.y + hh} ${pos.z}\n")
        writer.append("v ${pos.x - hw} ${pos.y + hh} ${pos.z}\n")
        
        writer.append("vn 0.0 0.0 1.0\n")
        
        writer.append("f $vertexOffset//$normalOffset ")
        writer.append("${vertexOffset + 1}//$normalOffset ")
        writer.append("${vertexOffset + 2}//$normalOffset\n")
        
        writer.append("f $vertexOffset//$normalOffset ")
        writer.append("${vertexOffset + 2}//$normalOffset ")
        writer.append("${vertexOffset + 3}//$normalOffset\n\n")
    }
    
    fun exportToJSON(room: Room, filename: String): File {
        val file = File(context.getExternalFilesDir(null), "$filename.json")
        
        val gson = GsonBuilder().setPrettyPrinting().create()
        
        val exportData = mapOf(
            "metadata" to mapOf(
                "version" to "1.0",
                "generator" to "Wall Scanner 3D Pro",
                "exportDate" to Date().toString()
            ),
            "room" to mapOf(
                "id" to room.id,
                "name" to room.name,
                "type" to room.estimateRoomType().name,
                "totalArea" to room.getTotalArea(),
                "wallCount" to room.walls.size,
                "elementCount" to room.getTotalElements()
            ),
            "walls" to room.walls.map { wall ->
                mapOf(
                    "id" to wall.id,
                    "texture" to wall.texture.displayName,
                    "confidence" to wall.confidence,
                    "area" to wall.getArea(),
                    "dimensions" to mapOf(
                        "width" to wall.getWidth(),
                        "height" to wall.getHeight()
                    ),
                    "corners" to wall.corners.map { corner ->
                        mapOf("x" to corner.x, "y" to corner.y, "z" to corner.z)
                    },
                    "normal" to mapOf(
                        "x" to wall.normal.x,
                        "y" to wall.normal.y,
                        "z" to wall.normal.z
                    ),
                    "elements" to wall.elements.map { element ->
                        mapOf(
                            "type" to when (element) {
                                is WallElement.Outlet -> "outlet"
                                is WallElement.Switch -> "switch"
                                is WallElement.Window -> "window"
                                is WallElement.Door -> "door"
                            },
                            "position" to mapOf(
                                "x" to element.position.x,
                                "y" to element.position.y,
                                "z" to element.position.z
                            ),
                            "size" to mapOf(
                                "width" to element.size.width,
                                "height" to element.size.height
                            ),
                            "confidence" to element.confidence,
                            "details" to getElementDetails(element)
                        )
                    }
                )
            }
        )
        
        file.writeText(gson.toJson(exportData))
        return file
    }
    
    private fun getElementDetails(element: WallElement): Map<String, Any> {
        return when (element) {
            is WallElement.Outlet -> mapOf("outletType" to element.type.name)
            is WallElement.Switch -> mapOf("switchCount" to element.switchCount)
            is WallElement.Window -> mapOf("windowType" to element.windowType.name)
            is WallElement.Door -> mapOf("doorType" to element.doorType.name)
        }
    }
    
    fun exportToGLTF(room: Room, filename: String): File {
        val file = File(context.getExternalFilesDir(null), "$filename.gltf")
        
        // Simplified GLTF export
        val gltf = buildString {
            append("{\n")
            append("  \"asset\": {\"version\": \"2.0\", \"generator\": \"Wall Scanner 3D Pro\"},\n")
            append("  \"scene\": 0,\n")
            append("  \"scenes\": [{\"nodes\": [0]}],\n")
            append("  \"nodes\": [{\"name\": \"${room.name}\", \"children\": [")
            append(room.walls.indices.joinToString(",") { (it + 1).toString() })
            append("]}],\n")
            append("  \"meshes\": [\n")
            
            room.walls.forEachIndexed { index, wall ->
                append("    {\"name\": \"Wall_${index + 1}\"}")
                if (index < room.walls.size - 1) append(",")
                append("\n")
            }
            
            append("  ]\n")
            append("}\n")
        }
        
        file.writeText(gltf)
        return file
    }
    
    fun exportToPLY(room: Room, filename: String): File {
        val file = File(context.getExternalFilesDir(null), "$filename.ply")
        val writer = FileWriter(file)
        
        val totalVertices = room.walls.sumOf { it.corners.size }
        val totalFaces = room.walls.size * 2 // 2 triangles per wall
        
        writer.append("ply\n")
        writer.append("format ascii 1.0\n")
        writer.append("comment Wall Scanner 3D Pro Export\n")
        writer.append("element vertex $totalVertices\n")
        writer.append("property float x\n")
        writer.append("property float y\n")
        writer.append("property float z\n")
        writer.append("element face $totalFaces\n")
        writer.append("property list uchar int vertex_indices\n")
        writer.append("end_header\n")
        
        // Write vertices
        room.walls.forEach { wall ->
            wall.corners.forEach { corner ->
                writer.append("${corner.x} ${corner.y} ${corner.z}\n")
            }
        }
        
        // Write faces
        var offset = 0
        room.walls.forEach { wall ->
            if (wall.corners.size >= 4) {
                writer.append("3 $offset ${offset + 1} ${offset + 2}\n")
                writer.append("3 $offset ${offset + 2} ${offset + 3}\n")
                offset += 4
            }
        }
        
        writer.close()
        return file
    }
    
    fun exportToFBX(room: Room, filename: String): File {
        // FBX export is complex - this is a placeholder
        val file = File(context.getExternalFilesDir(null), "$filename.fbx")
        file.writeText("FBX export not yet implemented")
        return file
    }
}

package com.example.math3d

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.math.min

object ModelFileLoader {

    fun loadModelFromUri(context: Context, uri: Uri): Model3D? {
        val fileName = getFileName(context, uri) ?: "Imported 3D Model"
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val triangles = parseObjOrStl(reader, fileName)
                if (triangles.isNotEmpty()) {
                    val normalizedTriangles = normalizeAndCenterMesh(triangles)
                    Model3D(
                        name = fileName,
                        description = "${normalizedTriangles.size} polygons loaded from storage",
                        triangles = normalizedTriangles
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun parseObjOrStl(reader: BufferedReader, fileName: String): List<Triangle> {
        val lowerName = fileName.lowercase()
        return if (lowerName.endsWith(".stl")) {
            parseStlAscii(reader)
        } else {
            parseObj(reader)
        }
    }

    private fun parseObj(reader: BufferedReader): List<Triangle> {
        val vertices = mutableListOf<Vec3>()
        val triangles = mutableListOf<Triangle>()

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachLine

            val parts = line.split("\\s+".toRegex())
            when (parts.getOrNull(0)) {
                "v" -> {
                    if (parts.size >= 4) {
                        val x = parts[1].toFloatOrNull() ?: 0f
                        val y = parts[2].toFloatOrNull() ?: 0f
                        val z = parts[3].toFloatOrNull() ?: 0f
                        vertices.add(Vec3(x, y, z))
                    }
                }
                "f" -> {
                    // Faces can be v or v/vt or v/vt/vn or v//vn
                    val faceIndices = mutableListOf<Int>()
                    for (i in 1 until parts.size) {
                        val token = parts[i].trim()
                        if (token.isNotEmpty()) {
                            val vStr = token.split("/")[0]
                            val idx = vStr.toIntOrNull()
                            if (idx != null) {
                                // 1-based index (positive) or negative (relative to end)
                                val realIdx = if (idx > 0) idx - 1 else vertices.size + idx
                                if (realIdx in vertices.indices) {
                                    faceIndices.add(realIdx)
                                }
                            }
                        }
                    }

                    // Triangulate polygon fan
                    if (faceIndices.size >= 3) {
                        val v0 = vertices[faceIndices[0]]
                        for (i in 1 until faceIndices.size - 1) {
                            val v1 = vertices[faceIndices[i]]
                            val v2 = vertices[faceIndices[i + 1]]
                            val norm = (v1 - v0).cross(v2 - v0).normalize()
                            triangles.add(Triangle(v0, v1, v2, norm))
                        }
                    }
                }
            }
        }
        return triangles
    }

    private fun parseStlAscii(reader: BufferedReader): List<Triangle> {
        val triangles = mutableListOf<Triangle>()
        val currentVertices = mutableListOf<Vec3>()
        var currentNormal = Vec3(0f, 1f, 0f)

        reader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("facet normal", ignoreCase = true)) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 5) {
                    val nx = parts[2].toFloatOrNull() ?: 0f
                    val ny = parts[3].toFloatOrNull() ?: 0f
                    val nz = parts[4].toFloatOrNull() ?: 0f
                    currentNormal = Vec3(nx, ny, nz).normalize()
                }
                currentVertices.clear()
            } else if (line.startsWith("vertex", ignoreCase = true)) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull() ?: 0f
                    val y = parts[2].toFloatOrNull() ?: 0f
                    val z = parts[3].toFloatOrNull() ?: 0f
                    currentVertices.add(Vec3(x, y, z))
                }
            } else if (line.startsWith("endfacet", ignoreCase = true)) {
                if (currentVertices.size == 3) {
                    val v1 = currentVertices[0]
                    val v2 = currentVertices[1]
                    val v3 = currentVertices[2]
                    val calcNorm = (v2 - v1).cross(v3 - v1).normalize()
                    val norm = if (calcNorm.length() > 0.001f) calcNorm else currentNormal
                    triangles.add(Triangle(v1, v2, v3, norm))
                }
                currentVertices.clear()
            }
        }
        return triangles
    }

    private fun normalizeAndCenterMesh(triangles: List<Triangle>): List<Triangle> {
        if (triangles.isEmpty()) return triangles

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        fun expand(v: Vec3) {
            minX = min(minX, v.x)
            minY = min(minY, v.y)
            minZ = min(minZ, v.z)
            maxX = max(maxX, v.x)
            maxY = max(maxY, v.y)
            maxZ = max(maxZ, v.z)
        }

        for (t in triangles) {
            expand(t.v1)
            expand(t.v2)
            expand(t.v3)
        }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val centerZ = (minZ + maxZ) / 2f

        val sizeX = maxX - minX
        val sizeY = maxY - minY
        val sizeZ = maxZ - minZ
        val maxDim = max(sizeX, max(sizeY, sizeZ))
        val targetSize = 2.0f
        val scale = if (maxDim > 0.0001f) targetSize / maxDim else 1.0f

        fun transformVertex(v: Vec3): Vec3 {
            return Vec3(
                x = (v.x - centerX) * scale,
                y = (v.y - centerY) * scale,
                z = (v.z - centerZ) * scale
            )
        }

        return triangles.map { t ->
            val v1 = transformVertex(t.v1)
            val v2 = transformVertex(t.v2)
            val v3 = transformVertex(t.v3)
            val norm = (v2 - v1).cross(v3 - v1).normalize()
            Triangle(v1, v2, v3, norm)
        }
    }
}

package com.example.math3d

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min

object ModelFileLoader {

    fun loadModelFromUri(context: Context, uri: Uri): Model3D? {
        val rawName = getFileName(context, uri) ?: "Imported 3D Model"
        val displayName = formatCleanName(rawName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val triangles = parseModelBytes(bytes, rawName)
                if (triangles.isNotEmpty()) {
                    val normalized = normalizeAndCenterMesh(triangles)
                    Model3D(
                        name = displayName,
                        description = "${normalized.size} polygons loaded (${getFileFormatLabel(rawName)})",
                        triangles = normalized,
                        fileUri = uri
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

    private fun formatCleanName(raw: String): String {
        val ext = raw.substringAfterLast('.', "").uppercase()
        val base = raw.substringBeforeLast('.')
        return if (base.length > 20 || base.startsWith("SDOC", ignoreCase = true) || base.contains("-")) {
            val formatTag = if (ext.isNotEmpty()) " [$ext]" else ""
            "3D Model$formatTag"
        } else {
            raw
        }
    }

    private fun getFileFormatLabel(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".glb") -> "GLB Binary Format"
            lower.endsWith(".gltf") -> "GLTF 2.0 Format"
            lower.endsWith(".usdz") -> "USDZ Apple AR Format"
            lower.endsWith(".usda") -> "USDA Universal Scene"
            lower.endsWith(".obj") -> "Wavefront OBJ"
            lower.endsWith(".stl") -> "Stereolithography STL"
            lower.endsWith(".ply") -> "Polygon PLY"
            else -> "3D Spatial Mesh"
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

    fun parseModelBytes(bytes: ByteArray, fileName: String): List<Triangle> {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".glb") || isGlbHeader(bytes) -> parseGlb(bytes)
            lowerName.endsWith(".gltf") || isGltfJson(bytes) -> parseGltf(String(bytes, Charsets.UTF_8))
            lowerName.endsWith(".usdz") || isZipArchive(bytes) -> parseUsdz(bytes)
            lowerName.endsWith(".usda") || lowerName.endsWith(".usd") -> parseUsda(String(bytes, Charsets.UTF_8))
            lowerName.endsWith(".stl") -> parseStl(bytes)
            lowerName.endsWith(".ply") -> parsePly(BufferedReader(InputStreamReader(ByteArrayInputStream(bytes))))
            else -> {
                // Default to OBJ parser
                val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes)))
                val objTriangles = parseObj(reader)
                if (objTriangles.isNotEmpty()) {
                    objTriangles
                } else {
                    // Fallback attempt: Try STL / GLTF
                    parseStl(bytes)
                }
            }
        }
    }

    // =========================================================================
    // 1. GLB (glTF 2.0 Binary) Parser
    // =========================================================================
    private fun isGlbHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val magic = (bytes[0].toInt() and 0xFF) or
                ((bytes[1].toInt() and 0xFF) shl 8) or
                ((bytes[2].toInt() and 0xFF) shl 16) or
                ((bytes[3].toInt() and 0xFF) shl 24)
        return magic == 0x46546C67 // "glTF" in ASCII little endian
    }

    private fun parseGlb(bytes: ByteArray): List<Triangle> {
        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.int
            if (magic != 0x46546C67) return emptyList()
            val version = buffer.int
            val totalLength = buffer.int

            var jsonString: String? = null
            var binaryBuffer: ByteArray? = null

            while (buffer.remaining() >= 8) {
                val chunkLength = buffer.int
                val chunkType = buffer.int
                if (chunkLength < 0 || chunkLength > buffer.remaining()) break

                val chunkData = ByteArray(chunkLength)
                buffer.get(chunkData)

                when (chunkType) {
                    0x4E4F534A -> { // "JSON"
                        jsonString = String(chunkData, Charsets.UTF_8).trim()
                    }
                    0x004E4942 -> { // "BIN\0"
                        binaryBuffer = chunkData
                    }
                }
            }

            if (jsonString != null) {
                return parseGltfJsonData(jsonString, binaryBuffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    // =========================================================================
    // 2. GLTF 2.0 (JSON + Data Buffers) Parser
    // =========================================================================
    private fun isGltfJson(bytes: ByteArray): Boolean {
        val sample = String(bytes.take(min(bytes.size, 200)).toByteArray(), Charsets.UTF_8)
        return sample.contains("\"asset\"") || sample.contains("\"meshes\"") || sample.contains("\"scene\"")
    }

    private fun parseGltf(jsonString: String): List<Triangle> {
        try {
            val root = JSONObject(jsonString)
            var binaryBuffer: ByteArray? = null

            // Check if buffer contains embedded Base64 data URI
            if (root.has("buffers")) {
                val buffers = root.getJSONArray("buffers")
                if (buffers.length() > 0) {
                    val firstBuf = buffers.getJSONObject(0)
                    if (firstBuf.has("uri")) {
                        val uriStr = firstBuf.getString("uri")
                        if (uriStr.startsWith("data:") && uriStr.contains("base64,")) {
                            val base64Data = uriStr.substringAfter("base64,")
                            binaryBuffer = Base64.decode(base64Data, Base64.DEFAULT)
                        }
                    }
                }
            }

            return parseGltfJsonData(jsonString, binaryBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun parseGltfJsonData(jsonString: String, binaryPayload: ByteArray?): List<Triangle> {
        val triangles = mutableListOf<Triangle>()
        try {
            val root = JSONObject(jsonString)
            val meshes = root.optJSONArray("meshes") ?: return emptyList()
            val accessors = root.optJSONArray("accessors") ?: return emptyList()
            val bufferViews = root.optJSONArray("bufferViews") ?: return emptyList()

            // Resolve buffers
            val buffersList = mutableListOf<ByteArray>()
            if (binaryPayload != null) {
                buffersList.add(binaryPayload)
            } else if (root.has("buffers")) {
                val bArray = root.getJSONArray("buffers")
                for (i in 0 until bArray.length()) {
                    val bObj = bArray.getJSONObject(i)
                    if (bObj.has("uri")) {
                        val uriStr = bObj.getString("uri")
                        if (uriStr.startsWith("data:") && uriStr.contains("base64,")) {
                            val b64 = uriStr.substringAfter("base64,")
                            buffersList.add(Base64.decode(b64, Base64.DEFAULT))
                        }
                    }
                }
            }

            val materials = root.optJSONArray("materials")

            for (m in 0 until meshes.length()) {
                val mesh = meshes.getJSONObject(m)
                val primitives = mesh.optJSONArray("primitives") ?: continue

                for (p in 0 until primitives.length()) {
                    val prim = primitives.getJSONObject(p)
                    val attributes = prim.optJSONObject("attributes") ?: continue
                    if (!attributes.has("POSITION")) continue

                    // Parse Material Color if available
                    var primColor = 0L
                    if (prim.has("material") && materials != null) {
                        val matIdx = prim.getInt("material")
                        if (matIdx in 0 until materials.length()) {
                            val matObj = materials.optJSONObject(matIdx)
                            val pbr = matObj?.optJSONObject("pbrMetallicRoughness")
                            if (pbr != null && pbr.has("baseColorFactor")) {
                                val bcf = pbr.getJSONArray("baseColorFactor")
                                val r = (bcf.optDouble(0, 1.0) * 255.0).toInt().coerceIn(0, 255)
                                val g = (bcf.optDouble(1, 1.0) * 255.0).toInt().coerceIn(0, 255)
                                val b = (bcf.optDouble(2, 1.0) * 255.0).toInt().coerceIn(0, 255)
                                val a = (bcf.optDouble(3, 1.0) * 255.0).toInt().coerceIn(0, 255)
                                primColor = ((a.toLong() and 0xFF) shl 24) or
                                        ((r.toLong() and 0xFF) shl 16) or
                                        ((g.toLong() and 0xFF) shl 8) or
                                        (b.toLong() and 0xFF)
                            }
                        }
                    }

                    val posAccessorIdx = attributes.getInt("POSITION")
                    val posAccessor = accessors.getJSONObject(posAccessorIdx)
                    val posBufferViewIdx = posAccessor.getInt("bufferView")
                    val posBufferView = bufferViews.getJSONObject(posBufferViewIdx)

                    val posBufferIdx = posBufferView.optInt("buffer", 0)
                    val rawBuffer = buffersList.getOrNull(posBufferIdx) ?: continue

                    val posByteOffset = posBufferView.optInt("byteOffset", 0) + posAccessor.optInt("byteOffset", 0)
                    val posCount = posAccessor.getInt("count")

                    // Read positions (Vec3)
                    val vertices = mutableListOf<Vec3>()
                    val byteBuf = ByteBuffer.wrap(rawBuffer).order(ByteOrder.LITTLE_ENDIAN)
                    byteBuf.position(posByteOffset)

                    val byteStride = posBufferView.optInt("byteStride", 12)
                    for (v in 0 until posCount) {
                        val vx = byteBuf.getFloat()
                        val vy = byteBuf.getFloat()
                        val vz = byteBuf.getFloat()
                        vertices.add(Vec3(vx, vy, vz))

                        val skip = byteStride - 12
                        if (skip > 0 && byteBuf.remaining() >= skip) {
                            byteBuf.position(byteBuf.position() + skip)
                        }
                    }

                    // Read indices if available
                    if (prim.has("indices")) {
                        val idxAccessorIdx = prim.getInt("indices")
                        val idxAccessor = accessors.getJSONObject(idxAccessorIdx)
                        val idxBufferViewIdx = idxAccessor.getInt("bufferView")
                        val idxBufferView = bufferViews.getJSONObject(idxBufferViewIdx)

                        val idxBufferIdx = idxBufferView.optInt("buffer", 0)
                        val idxRawBuffer = buffersList.getOrNull(idxBufferIdx) ?: rawBuffer

                        val idxByteOffset = idxBufferView.optInt("byteOffset", 0) + idxAccessor.optInt("byteOffset", 0)
                        val idxCount = idxAccessor.getInt("count")
                        val componentType = idxAccessor.getInt("componentType")

                        val idxBuf = ByteBuffer.wrap(idxRawBuffer).order(ByteOrder.LITTLE_ENDIAN)
                        idxBuf.position(idxByteOffset)

                        val indices = mutableListOf<Int>()
                        for (i in 0 until idxCount) {
                            val indexVal = when (componentType) {
                                5121 -> idxBuf.get().toInt() and 0xFF // UNSIGNED_BYTE
                                5123 -> idxBuf.short.toInt() and 0xFFFF // UNSIGNED_SHORT
                                5125 -> idxBuf.int // UNSIGNED_INT
                                else -> idxBuf.short.toInt() and 0xFFFF
                            }
                            indices.add(indexVal)
                        }

                        // Triangulate by 3s
                        for (i in 0 until indices.size - 2 step 3) {
                            val i0 = indices[i]
                            val i1 = indices[i + 1]
                            val i2 = indices[i + 2]
                            if (i0 in vertices.indices && i1 in vertices.indices && i2 in vertices.indices) {
                                val v1 = vertices[i0]
                                val v2 = vertices[i1]
                                val v3 = vertices[i2]
                                val norm = (v2 - v1).cross(v3 - v1).normalize()
                                triangles.add(Triangle(v1, v2, v3, norm, color = primColor))
                            }
                        }
                    } else {
                        // Non-indexed vertices (every 3 vertices form a triangle)
                        for (i in 0 until vertices.size - 2 step 3) {
                            val v1 = vertices[i]
                            val v2 = vertices[i + 1]
                            val v3 = vertices[i + 2]
                            val norm = (v2 - v1).cross(v3 - v1).normalize()
                            triangles.add(Triangle(v1, v2, v3, norm, color = primColor))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return triangles
    }

    // =========================================================================
    // 3. USDZ (Apple AR / Universal Scene Description Zip) Parser
    // =========================================================================
    private fun isZipArchive(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
    }

    private fun parseUsdz(bytes: ByteArray): List<Triangle> {
        try {
            val zis = ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name.lowercase()
                if (entryName.endsWith(".usda") || entryName.endsWith(".usd") || entryName.endsWith(".usdc")) {
                    val entryBytes = zis.readBytes()
                    val textContent = String(entryBytes, Charsets.UTF_8)
                    val parsed = parseUsda(textContent)
                    if (parsed.isNotEmpty()) {
                        return parsed
                    }
                    // If USDC binary, scan float arrays
                    val binaryUsdParsed = parseBinaryUsdHeuristic(entryBytes)
                    if (binaryUsdParsed.isNotEmpty()) {
                        return binaryUsdParsed
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun parseUsda(content: String): List<Triangle> {
        val triangles = mutableListOf<Triangle>()
        try {
            val lines = content.lines()
            val points = mutableListOf<Vec3>()
            val faceIndices = mutableListOf<Int>()
            val faceCounts = mutableListOf<Int>()

            var readingPoints = false
            var readingIndices = false
            var readingCounts = false

            for (line in lines) {
                val trimmed = line.trim()

                // Check sections
                if (trimmed.contains("point3f[] points") || trimmed.contains("float3[] points")) {
                    readingPoints = true
                    readingIndices = false
                    readingCounts = false
                } else if (trimmed.contains("int[] faceVertexIndices")) {
                    readingIndices = true
                    readingPoints = false
                    readingCounts = false
                } else if (trimmed.contains("int[] faceVertexCounts")) {
                    readingCounts = true
                    readingPoints = false
                    readingIndices = false
                }

                if (readingPoints) {
                    val pointRegex = "\\((-?\\d+\\.?\\d*(?:[eE][-+]?\\d+)?),\\s*(-?\\d+\\.?\\d*(?:[eE][-+]?\\d+)?),\\s*(-?\\d+\\.?\\d*(?:[eE][-+]?\\d+)?)\\)".toRegex()
                    val matches = pointRegex.findAll(trimmed)
                    for (m in matches) {
                        val x = m.groupValues[1].toFloatOrNull() ?: 0f
                        val y = m.groupValues[2].toFloatOrNull() ?: 0f
                        val z = m.groupValues[3].toFloatOrNull() ?: 0f
                        points.add(Vec3(x, y, z))
                    }
                    if (trimmed.endsWith("]") || trimmed.endsWith(");")) {
                        readingPoints = false
                    }
                }

                if (readingIndices) {
                    val numRegex = "\\b(\\d+)\\b".toRegex()
                    val matches = numRegex.findAll(trimmed)
                    for (m in matches) {
                        val idx = m.groupValues[1].toIntOrNull()
                        if (idx != null) faceIndices.add(idx)
                    }
                    if (trimmed.endsWith("]") || trimmed.endsWith(");")) {
                        readingIndices = false
                    }
                }

                if (readingCounts) {
                    val numRegex = "\\b(\\d+)\\b".toRegex()
                    val matches = numRegex.findAll(trimmed)
                    for (m in matches) {
                        val cnt = m.groupValues[1].toIntOrNull()
                        if (cnt != null) faceCounts.add(cnt)
                    }
                    if (trimmed.endsWith("]") || trimmed.endsWith(");")) {
                        readingCounts = false
                    }
                }
            }

            // Construct Triangles from Face Counts & Indices
            if (points.isNotEmpty() && faceIndices.isNotEmpty()) {
                var currentIndex = 0
                val counts = if (faceCounts.isNotEmpty()) faceCounts else List(faceIndices.size / 3) { 3 }

                for (count in counts) {
                    if (currentIndex + count <= faceIndices.size) {
                        val polyIndices = faceIndices.subList(currentIndex, currentIndex + count)
                        if (polyIndices.size >= 3) {
                            val v0 = points.getOrNull(polyIndices[0])
                            for (i in 1 until polyIndices.size - 1) {
                                val v1 = points.getOrNull(polyIndices[i])
                                val v2 = points.getOrNull(polyIndices[i + 1])
                                if (v0 != null && v1 != null && v2 != null) {
                                    val norm = (v1 - v0).cross(v2 - v0).normalize()
                                    triangles.add(Triangle(v0, v1, v2, norm))
                                }
                            }
                        }
                        currentIndex += count
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return triangles
    }

    private fun parseBinaryUsdHeuristic(bytes: ByteArray): List<Triangle> {
        // Fallback for binary USDC mesh data extraction
        val triangles = mutableListOf<Triangle>()
        try {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val points = mutableListOf<Vec3>()

            // Scan consecutive float triplets
            var consecutiveFloats = 0
            while (buf.remaining() >= 12 && points.size < 5000) {
                val f1 = buf.float
                val f2 = buf.float
                val f3 = buf.float

                val isReasonableFloat = { f: Float -> !f.isNaN() && !f.isInfinite() && f in -1000f..1000f && f != 0f }
                if (isReasonableFloat(f1) && isReasonableFloat(f2) && isReasonableFloat(f3)) {
                    points.add(Vec3(f1, f2, f3))
                }
            }

            for (i in 0 until points.size - 2 step 3) {
                val v1 = points[i]
                val v2 = points[i + 1]
                val v3 = points[i + 2]
                val norm = (v2 - v1).cross(v3 - v1).normalize()
                triangles.add(Triangle(v1, v2, v3, norm))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return triangles
    }

    // =========================================================================
    // 4. Wavefront OBJ Parser
    // =========================================================================
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
                    val faceIndices = mutableListOf<Int>()
                    for (i in 1 until parts.size) {
                        val token = parts[i].trim()
                        if (token.isNotEmpty()) {
                            val vStr = token.split("/")[0]
                            val idx = vStr.toIntOrNull()
                            if (idx != null) {
                                val realIdx = if (idx > 0) idx - 1 else vertices.size + idx
                                if (realIdx in vertices.indices) {
                                    faceIndices.add(realIdx)
                                }
                            }
                        }
                    }

                    // Polygon fan triangulation
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

    // =========================================================================
    // 5. STL (ASCII & Binary) Parser
    // =========================================================================
    private fun parseStl(bytes: ByteArray): List<Triangle> {
        val header = String(bytes.take(min(bytes.size, 80)).toByteArray(), Charsets.US_ASCII).lowercase()
        return if (header.startsWith("solid") && !isBinaryStl(bytes)) {
            val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes)))
            parseStlAscii(reader)
        } else {
            parseStlBinary(bytes)
        }
    }

    private fun isBinaryStl(bytes: ByteArray): Boolean {
        if (bytes.size < 84) return false
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(80)
        val numTriangles = buffer.int
        val expectedSize = 84 + (numTriangles * 50)
        return bytes.size == expectedSize
    }

    private fun parseStlBinary(bytes: ByteArray): List<Triangle> {
        val triangles = mutableListOf<Triangle>()
        try {
            if (bytes.size < 84) return emptyList()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(80)
            val numTriangles = buffer.int

            for (i in 0 until numTriangles) {
                if (buffer.remaining() < 50) break
                val nx = buffer.float
                val ny = buffer.float
                val nz = buffer.float
                val normal = Vec3(nx, ny, nz)

                val v1 = Vec3(buffer.float, buffer.float, buffer.float)
                val v2 = Vec3(buffer.float, buffer.float, buffer.float)
                val v3 = Vec3(buffer.float, buffer.float, buffer.float)
                val attributeByteCount = buffer.short

                val calcNorm = (v2 - v1).cross(v3 - v1).normalize()
                val finalNorm = if (calcNorm.length() > 0.001f) calcNorm else normal.normalize()
                triangles.add(Triangle(v1, v2, v3, finalNorm))
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    // =========================================================================
    // 6. PLY (Polygon File Format) Parser
    // =========================================================================
    private fun parsePly(reader: BufferedReader): List<Triangle> {
        val triangles = mutableListOf<Triangle>()
        try {
            var vertexCount = 0
            var faceCount = 0
            var isHeader = true
            val vertices = mutableListOf<Vec3>()

            val lines = reader.readLines()
            var lineIdx = 0

            while (lineIdx < lines.size && isHeader) {
                val line = lines[lineIdx].trim()
                if (line.startsWith("element vertex")) {
                    vertexCount = line.split("\\s+".toRegex()).getOrNull(2)?.toIntOrNull() ?: 0
                } else if (line.startsWith("element face")) {
                    faceCount = line.split("\\s+".toRegex()).getOrNull(2)?.toIntOrNull() ?: 0
                } else if (line == "end_header") {
                    isHeader = false
                }
                lineIdx++
            }

            for (v in 0 until vertexCount) {
                if (lineIdx >= lines.size) break
                val parts = lines[lineIdx].trim().split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val x = parts[0].toFloatOrNull() ?: 0f
                    val y = parts[1].toFloatOrNull() ?: 0f
                    val z = parts[2].toFloatOrNull() ?: 0f
                    vertices.add(Vec3(x, y, z))
                }
                lineIdx++
            }

            for (f in 0 until faceCount) {
                if (lineIdx >= lines.size) break
                val parts = lines[lineIdx].trim().split("\\s+".toRegex())
                if (parts.size >= 4) {
                    val count = parts[0].toIntOrNull() ?: 0
                    val indices = (1..count).mapNotNull { parts.getOrNull(it)?.toIntOrNull() }
                    if (indices.size >= 3) {
                        val v0 = vertices[indices[0]]
                        for (i in 1 until indices.size - 1) {
                            val v1 = vertices[indices[i]]
                            val v2 = vertices[indices[i + 1]]
                            val norm = (v1 - v0).cross(v2 - v0).normalize()
                            triangles.add(Triangle(v0, v1, v2, norm))
                        }
                    }
                }
                lineIdx++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return triangles
    }

    // =========================================================================
    // Normalization & Center Mesh
    // =========================================================================
    fun normalizeAndCenterMesh(triangles: List<Triangle>): List<Triangle> {
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
        val targetSize = 2.2f
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

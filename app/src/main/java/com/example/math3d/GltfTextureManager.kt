package com.example.math3d

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parses textures, images, samplers, and materials from GLTF/GLB files
 * and provides pixel sampling based on UV texture coordinates.
 */
class GltfTextureManager(
    private val root: JSONObject,
    private val buffersList: List<ByteArray>,
    private val modelDir: java.io.File? = null
) {
    private val imagesList = mutableListOf<Bitmap?>()
    private val texturesList = mutableListOf<Int>() // maps textureIndex -> imageIndex

    init {
        loadImages()
        loadTextures()
    }

    private fun loadImages() {
        val images = root.optJSONArray("images") ?: return
        val bufferViews = root.optJSONArray("bufferViews")

        for (i in 0 until images.length()) {
            val imgObj = images.optJSONObject(i) ?: continue
            var bmp: Bitmap? = null

            try {
                if (imgObj.has("bufferView") && bufferViews != null) {
                    val bvIdx = imgObj.getInt("bufferView")
                    if (bvIdx in 0 until bufferViews.length()) {
                        val bv = bufferViews.getJSONObject(bvIdx)
                        val bufIdx = bv.optInt("buffer", 0)
                        val rawBuf = buffersList.getOrNull(bufIdx)
                        if (rawBuf != null) {
                            val byteOffset = bv.optInt("byteOffset", 0)
                            val byteLength = bv.getInt("byteLength")
                            if (byteOffset + byteLength <= rawBuf.size) {
                                bmp = BitmapFactory.decodeByteArray(rawBuf, byteOffset, byteLength)
                            }
                        }
                    }
                } else if (imgObj.has("uri")) {
                    val uriStr = imgObj.getString("uri")
                    if (uriStr.startsWith("data:") && uriStr.contains("base64,")) {
                        val b64 = uriStr.substringAfter("base64,")
                        val imgBytes = Base64.decode(b64, Base64.DEFAULT)
                        bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                    } else if (modelDir != null) {
                        val cleanRelPath = uriStr.replace('\\', '/')
                        val externalFile = java.io.File(modelDir, cleanRelPath)
                        if (externalFile.exists() && externalFile.canRead()) {
                            bmp = BitmapFactory.decodeFile(externalFile.absolutePath)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            imagesList.add(bmp)
        }
    }

    private fun loadTextures() {
        val textures = root.optJSONArray("textures") ?: return
        for (i in 0 until textures.length()) {
            val texObj = textures.optJSONObject(i)
            val source = texObj?.optInt("source", -1) ?: -1
            texturesList.add(source)
        }
    }

    /**
     * Resolves the baseColorTexture bitmap for a material index (PBR Metallic-Roughness).
     */
    fun getBaseColorTexture(matIdx: Int): Bitmap? {
        val materials = root.optJSONArray("materials") ?: return null
        if (matIdx !in 0 until materials.length()) return null
        val matObj = materials.optJSONObject(matIdx) ?: return null
        val pbr = matObj.optJSONObject("pbrMetallicRoughness") ?: return null
        val texIdx = pbr.optJSONObject("baseColorTexture")?.optInt("index", -1) ?: -1
        return resolveTextureBitmap(texIdx)
    }

    /**
     * Resolves the diffuseTexture bitmap (from KHR_materials_pbrSpecularGlossiness or legacy diffuse).
     */
    fun getDiffuseTexture(matIdx: Int): Bitmap? {
        val materials = root.optJSONArray("materials") ?: return null
        if (matIdx !in 0 until materials.length()) return null
        val matObj = materials.optJSONObject(matIdx) ?: return null
        val specGloss = matObj.optJSONObject("extensions")?.optJSONObject("KHR_materials_pbrSpecularGlossiness")
        val texIdx = specGloss?.optJSONObject("diffuseTexture")?.optInt("index", -1) ?: -1
        return resolveTextureBitmap(texIdx)
    }

    /**
     * Resolves the emissiveTexture bitmap for a material index.
     */
    fun getEmissiveTexture(matIdx: Int): Bitmap? {
        val materials = root.optJSONArray("materials") ?: return null
        if (matIdx !in 0 until materials.length()) return null
        val matObj = materials.optJSONObject(matIdx) ?: return null
        val texIdx = matObj.optJSONObject("emissiveTexture")?.optInt("index", -1) ?: -1
        return resolveTextureBitmap(texIdx)
    }

    private fun resolveTextureBitmap(texIdx: Int): Bitmap? {
        if (texIdx != -1 && texIdx in texturesList.indices) {
            val imgIdx = texturesList[texIdx]
            if (imgIdx in imagesList.indices) {
                return imagesList[imgIdx]
            }
        }
        return null
    }

    /**
     * Resolves the full PBR material properties and bound textures for a material index.
     */
    fun getPbrMaterial(matIdx: Int): GltfPbrMaterial {
        val materials = root.optJSONArray("materials")
        if (materials == null || matIdx !in 0 until materials.length()) {
            return GltfPbrMaterial()
        }
        val matObj = materials.optJSONObject(matIdx) ?: return GltfPbrMaterial()

        val baseTex = getBaseColorTexture(matIdx)
        val diffTex = getDiffuseTexture(matIdx)
        val emissTex = getEmissiveTexture(matIdx)

        var baseColorFactor = 0L
        var metallic = 0.0f
        var roughness = 0.5f

        val pbr = matObj.optJSONObject("pbrMetallicRoughness")
        if (pbr != null) {
            metallic = pbr.optDouble("metallicFactor", 0.0).toFloat().coerceIn(0f, 1f)
            roughness = pbr.optDouble("roughnessFactor", 0.5).toFloat().coerceIn(0.04f, 1f)

            if (pbr.has("baseColorFactor")) {
                val bcf = pbr.getJSONArray("baseColorFactor")
                val r = (bcf.optDouble(0, 1.0) * 255.0).toInt().coerceIn(0, 255)
                val g = (bcf.optDouble(1, 1.0) * 255.0).toInt().coerceIn(0, 255)
                val b = (bcf.optDouble(2, 1.0) * 255.0).toInt().coerceIn(0, 255)
                val a = (bcf.optDouble(3, 1.0) * 255.0).toInt().coerceIn(0, 255)
                baseColorFactor = ((a.toLong() and 0xFF) shl 24) or
                        ((r.toLong() and 0xFF) shl 16) or
                        ((g.toLong() and 0xFF) shl 8) or
                        (b.toLong() and 0xFF)
            }
        }

        var diffuseFactor = 0L
        val specGloss = matObj.optJSONObject("extensions")?.optJSONObject("KHR_materials_pbrSpecularGlossiness")
        if (specGloss?.has("diffuseFactor") == true) {
            val df = specGloss.getJSONArray("diffuseFactor")
            val r = (df.optDouble(0, 1.0) * 255.0).toInt().coerceIn(0, 255)
            val g = (df.optDouble(1, 1.0) * 255.0).toInt().coerceIn(0, 255)
            val b = (df.optDouble(2, 1.0) * 255.0).toInt().coerceIn(0, 255)
            val a = (df.optDouble(3, 1.0) * 255.0).toInt().coerceIn(0, 255)
            diffuseFactor = ((a.toLong() and 0xFF) shl 24) or
                    ((r.toLong() and 0xFF) shl 16) or
                    ((g.toLong() and 0xFF) shl 8) or
                    (b.toLong() and 0xFF)
        }

        var emissiveFactor = 0L
        if (matObj.has("emissiveFactor")) {
            val ef = matObj.getJSONArray("emissiveFactor")
            val r = (ef.optDouble(0, 0.0) * 255.0).toInt().coerceIn(0, 255)
            val g = (ef.optDouble(1, 0.0) * 255.0).toInt().coerceIn(0, 255)
            val b = (ef.optDouble(2, 0.0) * 255.0).toInt().coerceIn(0, 255)
            if (r > 0 || g > 0 || b > 0) {
                emissiveFactor = (0xFFL shl 24) or
                        ((r.toLong() and 0xFF) shl 16) or
                        ((g.toLong() and 0xFF) shl 8) or
                        (b.toLong() and 0xFF)
            }
        }

        return GltfPbrMaterial(
            baseColorTexture = baseTex,
            diffuseTexture = diffTex,
            emissiveTexture = emissTex,
            baseColorFactor = baseColorFactor,
            diffuseFactor = diffuseFactor,
            emissiveFactor = emissiveFactor,
            metallic = metallic,
            roughness = roughness
        )
    }

    /**
     * Resolves the bitmap for a material index, checking:
     * 1. pbrMetallicRoughness.baseColorTexture
     * 2. extensions.KHR_materials_pbrSpecularGlossiness.diffuseTexture
     * 3. emissiveTexture
     */
    fun getTextureBitmapForMaterial(matIdx: Int): Bitmap? {
        return getBaseColorTexture(matIdx)
            ?: getDiffuseTexture(matIdx)
            ?: getEmissiveTexture(matIdx)
    }

    companion object {
        /**
         * Bilinearly or nearest-neighbor samples a bitmap using normalized UV coordinates (0..1)
         */
        fun sampleTexture(bitmap: Bitmap, u: Float, v: Float): Long {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return 0L

            // Wrap UV coordinates to [0..1]
            var nu = u % 1.0f
            if (nu < 0f) nu += 1.0f
            var nv = v % 1.0f
            if (nv < 0f) nv += 1.0f

            // In glTF, UV (0,0) is top-left
            val px = (nu * (w - 1)).toInt().coerceIn(0, w - 1)
            val py = (nv * (h - 1)).toInt().coerceIn(0, h - 1)

            val pixel = bitmap.getPixel(px, py)
            val a = (pixel ushr 24) and 0xFF
            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF

            return ((a.toLong() and 0xFF) shl 24) or
                    ((r.toLong() and 0xFF) shl 16) or
                    ((g.toLong() and 0xFF) shl 8) or
                    (b.toLong() and 0xFF)
        }
    }
}

/**
 * Encapsulates extracted PBR texture maps and material coefficients.
 */
data class GltfPbrMaterial(
    val baseColorTexture: Bitmap? = null,
    val diffuseTexture: Bitmap? = null,
    val emissiveTexture: Bitmap? = null,
    val baseColorFactor: Long = 0L,
    val diffuseFactor: Long = 0L,
    val emissiveFactor: Long = 0L,
    val metallic: Float = 0.0f,
    val roughness: Float = 0.5f
) {
    fun sampleBaseOrDiffuseColor(u: Float, v: Float, fallbackVertexColor: Long = 0L): Long {
        if (baseColorTexture != null) {
            val sampled = GltfTextureManager.sampleTexture(baseColorTexture, u, v)
            if (sampled != 0L) return multiplyColors(sampled, baseColorFactor)
        }
        if (diffuseTexture != null) {
            val sampled = GltfTextureManager.sampleTexture(diffuseTexture, u, v)
            if (sampled != 0L) return multiplyColors(sampled, diffuseFactor)
        }
        if (baseColorFactor != 0L) return baseColorFactor
        if (diffuseFactor != 0L) return diffuseFactor
        return fallbackVertexColor
    }

    fun sampleEmissiveColor(u: Float, v: Float): Long {
        if (emissiveTexture != null) {
            val sampled = GltfTextureManager.sampleTexture(emissiveTexture, u, v)
            if (sampled != 0L) return sampled
        }
        return emissiveFactor
    }

    private fun multiplyColors(c1: Long, c2: Long): Long {
        if (c2 == 0L) return c1
        val a1 = ((c1 ushr 24) and 0xFF).toFloat() / 255f
        val r1 = ((c1 ushr 16) and 0xFF).toFloat() / 255f
        val g1 = ((c1 ushr 8) and 0xFF).toFloat() / 255f
        val b1 = (c1 and 0xFF).toFloat() / 255f

        val a2 = ((c2 ushr 24) and 0xFF).toFloat() / 255f
        val r2 = ((c2 ushr 16) and 0xFF).toFloat() / 255f
        val g2 = ((c2 ushr 8) and 0xFF).toFloat() / 255f
        val b2 = (c2 and 0xFF).toFloat() / 255f

        val a = ((a1 * a2) * 255f).toInt().coerceIn(0, 255)
        val r = ((r1 * r2) * 255f).toInt().coerceIn(0, 255)
        val g = ((g1 * g2) * 255f).toInt().coerceIn(0, 255)
        val b = ((b1 * b2) * 255f).toInt().coerceIn(0, 255)

        return ((a.toLong() and 0xFF) shl 24) or
                ((r.toLong() and 0xFF) shl 16) or
                ((g.toLong() and 0xFF) shl 8) or
                (b.toLong() and 0xFF)
    }
}


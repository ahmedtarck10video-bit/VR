package com.example.math3d

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(v: Vec3) = Vec3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3) = Vec3(x - v.x, y - v.y, z - v.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vec3(x / s, y / s, z / s)

    fun dot(v: Vec3): Float = x * v.x + y * v.y + z * v.z
    fun cross(v: Vec3): Vec3 = Vec3(
        y * v.z - z * v.y,
        z * v.x - x * v.z,
        x * v.y - y * v.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSq(): Float = x * x + y * y + z * z
    fun normalize(): Vec3 {
        val l = length()
        return if (l > 1e-6f) this / l else Vec3(0f, 0f, 0f)
    }

    fun rotateX(angleRad: Float): Vec3 {
        val c = cos(angleRad)
        val s = sin(angleRad)
        return Vec3(x, y * c - z * s, y * s + z * c)
    }

    fun rotateY(angleRad: Float): Vec3 {
        val c = cos(angleRad)
        val s = sin(angleRad)
        return Vec3(x * c + z * s, y, -x * s + z * c)
    }

    fun rotateZ(angleRad: Float): Vec3 {
        val c = cos(angleRad)
        val s = sin(angleRad)
        return Vec3(x * c - y * s, x * s + y * c, z)
    }
}

data class Triangle(
    val v1: Vec3,
    val v2: Vec3,
    val v3: Vec3,
    val normal: Vec3,
    val color: Long = 0L, // Base / Diffuse color (ARGB)
    val emissiveColor: Long = 0L, // Emissive / Glow color (ARGB)
    val metallic: Float = 0.0f,
    val roughness: Float = 0.5f,
    val u1: Float = 0f, val v1Coord: Float = 0f,
    val u2: Float = 0f, val v2Coord: Float = 0f,
    val u3: Float = 0f, val v3Coord: Float = 0f
) {
    val center: Vec3 get() = Vec3((v1.x + v2.x + v3.x) / 3f, (v1.y + v2.y + v3.y) / 3f, (v1.z + v2.z + v3.z) / 3f)
}

data class Model3D(
    val name: String,
    val description: String,
    val triangles: List<Triangle> = emptyList(),
    val fileUri: android.net.Uri? = null,
    val localFilePath: String? = null,
    val isGlbOrGltf: Boolean = false,
    val realWorldWidthMeters: Float = 0.5f,
    val realWorldHeightMeters: Float = 0.5f,
    val realWorldDepthMeters: Float = 0.5f
) {
    val trianglesCount: Int get() = triangles.size
    val verticesCount: Int get() = triangles.size * 3
}

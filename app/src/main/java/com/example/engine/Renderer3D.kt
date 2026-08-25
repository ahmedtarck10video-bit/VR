package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.math3d.Model3D
import com.example.math3d.Triangle
import com.example.math3d.Vec3
import kotlin.math.max
import kotlin.math.min

class Renderer3D {

    private val lightDir = Vec3(0.5f, 1.0f, -0.8f).normalize()

    fun render(
        drawScope: DrawScope,
        model: Model3D,
        rotX: Float,
        rotY: Float,
        rotZ: Float,
        scale: Float,
        panX: Float,
        panY: Float,
        distance: Float = 4.0f,
        wireframe: Boolean = false,
        primaryColor: Color = Color(0xFF00E5FF),
        drawShadow: Boolean = true
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerX = width / 2f + panX
        val centerY = height / 2f + panY
        val fov = 450f * scale

        // Transform & project all triangles
        val transformed = model.triangles.mapNotNull { tri ->
            // Rotate vertices
            val v1 = tri.v1.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)
            val v2 = tri.v2.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)
            val v3 = tri.v3.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)

            val norm = (v2 - v1).cross(v3 - v1).normalize()

            // World offset
            val zOffset = distance
            val wv1 = Vec3(v1.x, v1.y, v1.z + zOffset)
            val wv2 = Vec3(v2.x, v2.y, v2.z + zOffset)
            val wv3 = Vec3(v3.x, v3.y, v3.z + zOffset)

            // Backface culling
            val viewDir = Vec3(0f, 0f, 1f)
            if (norm.dot(viewDir) <= 0.05f && !wireframe) {
                return@mapNotNull null
            }

            // Project 3D -> 2D
            val p1 = project(wv1, centerX, centerY, fov)
            val p2 = project(wv2, centerX, centerY, fov)
            val p3 = project(wv3, centerX, centerY, fov)

            val avgZ = (wv1.z + wv2.z + wv3.z) / 3f
            val diffuse = max(0.15f, norm.dot(lightDir))

            ProjectedTriangle(p1, p2, p3, avgZ, diffuse)
        }

        // Sort Painter's Algorithm (furthest to nearest)
        val sorted = transformed.sortedByDescending { it.avgZ }

        // Optional Ground Shadow
        if (drawShadow) {
            val shadowY = centerY + 180f * scale
            drawScope.drawOval(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(centerX - 100f * scale, shadowY - 20f * scale),
                size = androidx.compose.ui.geometry.Size(200f * scale, 40f * scale)
            )
        }

        // Draw triangles
        for (tri in sorted) {
            val path = Path().apply {
                moveTo(tri.p1.x, tri.p1.y)
                lineTo(tri.p2.x, tri.p2.y)
                lineTo(tri.p3.x, tri.p3.y)
                close()
            }

            if (wireframe) {
                drawScope.drawPath(
                    path = path,
                    color = primaryColor.copy(alpha = 0.85f),
                    style = Stroke(width = 2f)
                )
            } else {
                val shadeFactor = tri.diffuse
                val faceColor = Color(
                    red = min(1f, primaryColor.red * shadeFactor + 0.1f),
                    green = min(1f, primaryColor.green * shadeFactor + 0.1f),
                    blue = min(1f, primaryColor.blue * shadeFactor + 0.1f),
                    alpha = 0.92f
                )

                drawScope.drawPath(path = path, color = faceColor)

                // Subtle edge stroke for crisp aesthetic
                drawScope.drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.25f),
                    style = Stroke(width = 1f)
                )
            }
        }
    }

    private fun project(v: Vec3, centerX: Float, centerY: Float, fov: Float): Offset {
        val z = max(0.1f, v.z)
        val x = centerX + (v.x / z) * fov
        val y = centerY - (v.y / z) * fov // Y is inverted in 2D canvas
        return Offset(x, y)
    }

    private data class ProjectedTriangle(
        val p1: Offset,
        val p2: Offset,
        val p3: Offset,
        val avgZ: Float,
        val diffuse: Float
    )
}

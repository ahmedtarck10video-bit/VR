package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

    private val lightDir = Vec3(0.6f, 1.2f, -0.8f).normalize()
    private val secondaryLightDir = Vec3(-0.4f, -0.2f, -0.6f).normalize()

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
        drawShadow: Boolean = true,
        drawFloorGrid: Boolean = false
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerX = width / 2f + panX
        // Ground model slightly lower naturally on screen
        val centerY = height / 2f + panY
        val fov = 450f * scale

        // Optional Floor Spatial Anchor Grid / Reticle
        if (drawFloorGrid) {
            val groundY = centerY + 160f * scale
            val gridRadius = 140f * scale
            // Outer spatial circle
            drawScope.drawOval(
                color = primaryColor.copy(alpha = 0.25f),
                topLeft = Offset(centerX - gridRadius, groundY - gridRadius * 0.35f),
                size = androidx.compose.ui.geometry.Size(gridRadius * 2f, gridRadius * 0.7f),
                style = Stroke(width = 1.5f)
            )
            // Inner ring
            drawScope.drawOval(
                color = primaryColor.copy(alpha = 0.4f),
                topLeft = Offset(centerX - gridRadius * 0.5f, groundY - gridRadius * 0.175f),
                size = androidx.compose.ui.geometry.Size(gridRadius, gridRadius * 0.35f),
                style = Stroke(width = 1.5f)
            )
            // Crosshairs
            drawScope.drawLine(
                color = primaryColor.copy(alpha = 0.3f),
                start = Offset(centerX - gridRadius * 1.1f, groundY),
                end = Offset(centerX + gridRadius * 1.1f, groundY),
                strokeWidth = 1f
            )
            drawScope.drawLine(
                color = primaryColor.copy(alpha = 0.3f),
                start = Offset(centerX, groundY - gridRadius * 0.4f),
                end = Offset(centerX, groundY + gridRadius * 0.4f),
                strokeWidth = 1f
            )
        }

        // Optional Ground Realistic Soft Contact Shadow
        if (drawShadow) {
            val shadowY = centerY + 155f * scale
            val shadowW = 210f * scale
            val shadowH = 48f * scale

            // Soft ambient shadow
            drawScope.drawOval(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(centerX - shadowW / 2f, shadowY - shadowH / 2f),
                size = androidx.compose.ui.geometry.Size(shadowW, shadowH)
            )
            // Core contact shadow
            drawScope.drawOval(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(centerX - shadowW * 0.3f, shadowY - shadowH * 0.3f),
                size = androidx.compose.ui.geometry.Size(shadowW * 0.6f, shadowH * 0.6f)
            )
        }

        // Transform & project all triangles
        val transformed = model.triangles.mapNotNull { tri ->
            // Rotate vertices in 3D
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
            val dotView = norm.dot(viewDir)
            if (dotView <= 0.02f && !wireframe) {
                return@mapNotNull null
            }

            // Project 3D -> 2D
            val p1 = project(wv1, centerX, centerY, fov)
            val p2 = project(wv2, centerX, centerY, fov)
            val p3 = project(wv3, centerX, centerY, fov)

            val avgZ = (wv1.z + wv2.z + wv3.z) / 3f
            val mainDiffuse = max(0.12f, norm.dot(lightDir))
            val secDiffuse = max(0.0f, norm.dot(secondaryLightDir)) * 0.25f
            val totalDiffuse = min(1.0f, mainDiffuse + secDiffuse)

            ProjectedTriangle(p1, p2, p3, avgZ, totalDiffuse, dotView)
        }

        // Sort Painter's Algorithm (furthest to nearest)
        val sorted = transformed.sortedByDescending { it.avgZ }

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
                    style = Stroke(width = 1.6f)
                )
            } else {
                val shadeFactor = tri.diffuse
                val faceColor = Color(
                    red = min(1f, primaryColor.red * shadeFactor + 0.08f),
                    green = min(1f, primaryColor.green * shadeFactor + 0.08f),
                    blue = min(1f, primaryColor.blue * shadeFactor + 0.08f),
                    alpha = 0.95f
                )

                drawScope.drawPath(path = path, color = faceColor)

                // Crisp edge stroke for polygon fidelity
                drawScope.drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.18f),
                    style = Stroke(width = 0.8f)
                )
            }
        }
    }

    private fun project(v: Vec3, centerX: Float, centerY: Float, fov: Float): Offset {
        val z = max(0.1f, v.z)
        val x = centerX + (v.x / z) * fov
        val y = centerY - (v.y / z) * fov
        return Offset(x, y)
    }

    private data class ProjectedTriangle(
        val p1: Offset,
        val p2: Offset,
        val p3: Offset,
        val avgZ: Float,
        val diffuse: Float,
        val dotView: Float
    )
}

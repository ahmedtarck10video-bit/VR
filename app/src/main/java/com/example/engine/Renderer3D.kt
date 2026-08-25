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
import kotlin.math.pow

class Renderer3D {

    private val keyLightDir = Vec3(0.5f, 1.2f, -0.8f).normalize()
    private val fillLightDir = Vec3(-0.6f, 0.4f, -0.5f).normalize()
    private val viewDir = Vec3(0f, 0f, 1f)

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
        primaryColor: Color = Color(0xFFE2DCD4),
        drawShadow: Boolean = true,
        drawFloorGrid: Boolean = false
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerX = width / 2f + panX
        val centerY = height / 2f + panY
        val fov = 460f * scale

        // Floor Shadow
        if (drawShadow) {
            val shadowY = centerY + 155f * scale
            val shadowW = 220f * scale
            val shadowH = 45f * scale

            // Ambient ground shadow
            drawScope.drawOval(
                color = Color(0x35000000),
                topLeft = Offset(centerX - shadowW / 2f, shadowY - shadowH / 2f),
                size = androidx.compose.ui.geometry.Size(shadowW, shadowH)
            )
            // Core contact occlusion shadow
            drawScope.drawOval(
                color = Color(0x60000000),
                topLeft = Offset(centerX - shadowW * 0.3f, shadowY - shadowH * 0.28f),
                size = androidx.compose.ui.geometry.Size(shadowW * 0.6f, shadowH * 0.56f)
            )
        }

        // Floor Spatial Reticle
        if (drawFloorGrid) {
            val groundY = centerY + 160f * scale
            val gridRadius = 140f * scale
            drawScope.drawOval(
                color = Color(0x401A73E8),
                topLeft = Offset(centerX - gridRadius, groundY - gridRadius * 0.35f),
                size = androidx.compose.ui.geometry.Size(gridRadius * 2f, gridRadius * 0.7f),
                style = Stroke(width = 1.5f)
            )
            drawScope.drawOval(
                color = Color(0x701A73E8),
                topLeft = Offset(centerX - gridRadius * 0.5f, groundY - gridRadius * 0.175f),
                size = androidx.compose.ui.geometry.Size(gridRadius, gridRadius * 0.35f),
                style = Stroke(width = 1.5f)
            )
            drawScope.drawLine(
                color = Color(0x501A73E8),
                start = Offset(centerX - gridRadius * 1.1f, groundY),
                end = Offset(centerX + gridRadius * 1.1f, groundY),
                strokeWidth = 1f
            )
            drawScope.drawLine(
                color = Color(0x501A73E8),
                start = Offset(centerX, groundY - gridRadius * 0.4f),
                end = Offset(centerX, groundY + gridRadius * 0.4f),
                strokeWidth = 1f
            )
        }

        // Transform & project all triangles
        val halfKey = (keyLightDir + viewDir).normalize()
        val transformed = model.triangles.mapNotNull { tri ->
            val v1 = tri.v1.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)
            val v2 = tri.v2.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)
            val v3 = tri.v3.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)

            val norm = (v2 - v1).cross(v3 - v1).normalize()

            val zOffset = distance
            val wv1 = Vec3(v1.x, v1.y, v1.z + zOffset)
            val wv2 = Vec3(v2.x, v2.y, v2.z + zOffset)
            val wv3 = Vec3(v3.x, v3.y, v3.z + zOffset)

            // Backface culling
            val dotView = norm.dot(viewDir)
            if (dotView <= 0.01f && !wireframe) {
                return@mapNotNull null
            }

            val p1 = project(wv1, centerX, centerY, fov)
            val p2 = project(wv2, centerX, centerY, fov)
            val p3 = project(wv3, centerX, centerY, fov)
            val avgZ = (wv1.z + wv2.z + wv3.z) / 3f

            // Realistic PBR Blinn-Phong Shading
            val keyDiffuse = max(0.0f, norm.dot(keyLightDir))
            val fillDiffuse = max(0.0f, norm.dot(fillLightDir)) * 0.3f
            val ambient = 0.35f
            val diffuseTotal = min(1.0f, ambient + keyDiffuse * 0.65f + fillDiffuse)

            // Specular highlight
            val dotH = max(0.0f, norm.dot(halfKey))
            val spec = dotH.pow(16) * 0.28f

            // Base color resolution
            val baseColor = if (tri.color != 0L) {
                Color(tri.color)
            } else {
                primaryColor
            }

            ProjectedTriangle(p1, p2, p3, avgZ, diffuseTotal, spec, baseColor)
        }

        val sorted = transformed.sortedByDescending { it.avgZ }

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
                    color = tri.baseColor.copy(alpha = 0.9f),
                    style = Stroke(width = 1.2f)
                )
            } else {
                val d = tri.diffuse
                val s = tri.specular
                val c = tri.baseColor

                val r = min(1.0f, c.red * d + s).coerceAtLeast(0f)
                val g = min(1.0f, c.green * d + s).coerceAtLeast(0f)
                val b = min(1.0f, c.blue * d + s).coerceAtLeast(0f)

                val shadedColor = Color(r, g, b, 1.0f)
                drawScope.drawPath(path = path, color = shadedColor)

                // Subtle edge line for polygon separation and micro-depth
                drawScope.drawPath(
                    path = path,
                    color = Color.Black.copy(alpha = 0.08f),
                    style = Stroke(width = 0.6f)
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
        val specular: Float,
        val baseColor: Color
    )
}


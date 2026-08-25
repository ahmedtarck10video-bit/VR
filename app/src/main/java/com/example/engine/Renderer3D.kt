package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

    private val viewDir = Vec3(0f, 0f, 1f)

    companion object {
        fun colorFromArgbLong(c: Long): Color {
            if (c == 0L) return Color(0xFFE2E8F0)
            val a = ((c ushr 24) and 0xFF).toInt() / 255f
            val r = ((c ushr 16) and 0xFF).toInt() / 255f
            val g = ((c ushr 8) and 0xFF).toInt() / 255f
            val b = (c and 0xFF).toInt() / 255f
            return Color(r, g, b, if (a > 0.01f) a else 1f)
        }

        /** Converts sRGB [0..1] to Linear space */
        fun srgbToLinear(v: Float): Float {
            return if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
        }

        /** Converts Linear space [0..1] to sRGB with ACES-like highlight compression */
        fun linearToSrgb(v: Float): Float {
            // Filmic tone curve to prevent harsh clipping on HDR highlights
            val a = 2.51f
            val b = 0.03f
            val c = 2.43f
            val d = 0.59f
            val e = 0.14f
            val mapped = ((v * (a * v + b)) / (v * (c * v + d) + e)).coerceIn(0f, 1f)
            return if (mapped <= 0.0031308f) mapped * 12.92f else 1.055f * mapped.pow(1f / 2.4f) - 0.055f
        }
    }

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
        primaryColor: Color = Color(0xFFE2E8F0),
        drawShadow: Boolean = false,
        drawFloorGrid: Boolean = false,
        hdriPreset: HdriPreset = HdriPreset.STUDIO_PRO
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerX = width / 2f + panX
        val centerY = height / 2f + panY
        val fov = 460f * scale

        // Find lowest Y in world space to ground the shadow accurately
        var modelMinY = Float.MAX_VALUE
        var modelMaxY = -Float.MAX_VALUE

        // 1. Transform all triangles
        val transformed = model.triangles.mapNotNull { tri ->
            val v1 = tri.v1.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)
            val v2 = tri.v2.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)
            val v3 = tri.v3.rotateX(rotX).rotateY(rotY).rotateZ(rotZ)

            // Track min/max Y for grounding shadows
            val minY = min(v1.y, min(v2.y, v3.y))
            val maxY = max(v1.y, max(v2.y, v3.y))
            if (minY < modelMinY) modelMinY = minY
            if (maxY > modelMaxY) modelMaxY = maxY

            // Compute geometric face normal
            val edge1 = v2 - v1
            val edge2 = v3 - v1
            var norm = edge1.cross(edge2)
            norm = if (norm.lengthSq() > 1e-6f) norm.normalize() else Vec3(0f, 1f, 0f)

            val zOffset = distance
            val wv1 = Vec3(v1.x, v1.y, v1.z + zOffset)
            val wv2 = Vec3(v2.x, v2.y, v2.z + zOffset)
            val wv3 = Vec3(v3.x, v3.y, v3.z + zOffset)

            val p1 = project(wv1, centerX, centerY, fov)
            val p2 = project(wv2, centerX, centerY, fov)
            val p3 = project(wv3, centerX, centerY, fov)
            val avgZ = (wv1.z + wv2.z + wv3.z) / 3f

            // Double-sided lighting & outward normal correction
            val dotCam = norm.dot(viewDir)
            val effectiveNorm = if (dotCam >= 0f) norm else Vec3(-norm.x, -norm.y, -norm.z)

            // =================================================================
            // HDRi ENVIRONMENT LIGHTING (Sky Hemisphere + Sun + Fill + Reflection)
            // =================================================================
            val diffuseIrradiance = hdriPreset.computeDiffuseIrradiance(effectiveNorm)
            val specularRadiance = hdriPreset.computeSpecularRadiance(effectiveNorm, viewDir, roughness = 0.32f)

            // Base color resolution in Linear Space
            val baseColor = if (tri.color != 0L) {
                colorFromArgbLong(tri.color)
            } else {
                primaryColor
            }

            ProjectedTriangle(p1, p2, p3, avgZ, diffuseIrradiance, specularRadiance, baseColor)
        }

        // =========================================================================
        // REALISTIC MULTI-LAYERED SHADOWS (Ground Contact + Cast Shadow + Soft AO)
        // =========================================================================
        if (drawShadow) {
            val groundYOffset = if (modelMinY != Float.MAX_VALUE) {
                centerY - (modelMinY / distance) * fov + 8f * scale
            } else {
                centerY + 160f * scale
            }

            val shadowWidth = 240f * scale
            val shadowHeight = 65f * scale

            // 1. Soft Ambient Occlusion ground disc
            drawScope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x35000000), Color(0x15000000), Color.Transparent),
                    center = Offset(centerX, groundYOffset),
                    radius = shadowWidth * 0.75f
                ),
                topLeft = Offset(centerX - shadowWidth * 0.75f, groundYOffset - shadowHeight * 0.75f),
                size = Size(shadowWidth * 1.5f, shadowHeight * 1.5f)
            )

            // 2. Directional Cast Shadow
            val lightCastOffsetX = 22f * scale
            val lightCastOffsetY = 10f * scale
            drawScope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x50000000), Color(0x20000000), Color.Transparent),
                    center = Offset(centerX + lightCastOffsetX, groundYOffset + lightCastOffsetY),
                    radius = shadowWidth * 0.5f
                ),
                topLeft = Offset(centerX + lightCastOffsetX - shadowWidth * 0.5f, groundYOffset + lightCastOffsetY - shadowHeight * 0.5f),
                size = Size(shadowWidth, shadowHeight)
            )

            // 3. Dense Contact Shadow
            val coreWidth = shadowWidth * 0.55f
            val coreHeight = shadowHeight * 0.45f
            drawScope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x80000000), Color(0x40000000), Color.Transparent),
                    center = Offset(centerX, groundYOffset),
                    radius = coreWidth * 0.5f
                ),
                topLeft = Offset(centerX - coreWidth * 0.5f, groundYOffset - coreHeight * 0.5f),
                size = Size(coreWidth, coreHeight)
            )
        }

        // =========================================================================
        // Floor Spatial Reticle (for AR placement)
        // =========================================================================
        if (drawFloorGrid) {
            val groundY = centerY + 160f * scale
            val gridRadius = 140f * scale
            drawScope.drawOval(
                color = Color(0x401A73E8),
                topLeft = Offset(centerX - gridRadius, groundY - gridRadius * 0.35f),
                size = Size(gridRadius * 2f, gridRadius * 0.7f),
                style = Stroke(width = 1.5f)
            )
            drawScope.drawOval(
                color = Color(0x701A73E8),
                topLeft = Offset(centerX - gridRadius * 0.5f, groundY - gridRadius * 0.175f),
                size = Size(gridRadius, gridRadius * 0.35f),
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

        // Depth Sorting (Painter's Algorithm)
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
                val c = tri.baseColor
                // Convert sRGB to Linear Space
                val linR = srgbToLinear(c.red)
                val linG = srgbToLinear(c.green)
                val linB = srgbToLinear(c.blue)

                // Multiply Linear Base Color by Linear HDRi Irradiance + Specular Reflection
                val litR = linR * tri.diffuseIrradiance.x + tri.specularRadiance.x
                val litG = linG * tri.diffuseIrradiance.y + tri.specularRadiance.y
                val litB = linB * tri.diffuseIrradiance.z + tri.specularRadiance.z

                // Convert from Linear back to sRGB with Filmic Tone Mapping
                val outR = linearToSrgb(litR)
                val outG = linearToSrgb(litG)
                val outB = linearToSrgb(litB)

                val shadedColor = Color(outR, outG, outB, c.alpha)
                drawScope.drawPath(path = path, color = shadedColor)
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
        val diffuseIrradiance: Vec3,
        val specularRadiance: Vec3,
        val baseColor: Color
    )
}

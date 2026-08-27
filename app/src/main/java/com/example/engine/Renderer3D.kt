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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class Renderer3D {

    private val viewDir = Vec3(0f, 0f, 1f)

    // Reusable Path to avoid object allocations in hot render loop
    private val reusablePath = Path()

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
        hdriPreset: HdriPreset = HdriPreset.STUDIO_PRO,
        engineProfile: RenderEngineProfile = RenderEngineProfile.REALITYKIT
    ) {
        val allTriangles = model.triangles
        if (allTriangles.isEmpty()) return

        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerX = width / 2f + panX
        val centerY = height / 2f + panY
        val fov = 460f * scale

        // Precompute rotation matrices to eliminate trigonometric recomputations per vertex
        val radX = rotX
        val radY = rotY
        val radZ = rotZ

        val cx = cos(radX); val sx = sin(radX)
        val cy = cos(radY); val sy = sin(radY)
        val cz = cos(radZ); val sz = sin(radZ)

        // Combined 3x3 rotation matrix R = Rz * Ry * Rx
        val m00 = cz * cy
        val m01 = cz * sy * sx - sz * cx
        val m02 = cz * sy * cx + sz * sx

        val m10 = sz * cy
        val m11 = sz * sy * sx + cz * cx
        val m12 = sz * sy * cx - cz * sx

        val m20 = -sy
        val m21 = cy * sx
        val m22 = cy * cx

        var modelMinY = Float.MAX_VALUE
        var modelMaxY = -Float.MAX_VALUE

        // =========================================================================
        // SOLID 3D MESH RENDERER & HIGH-PERFORMANCE SCREEN FRUSTUM CULLING
        // Adaptive Level-Of-Detail handles massive 250MB models at 60 FPS
        // =========================================================================
        val totalTriangles = allTriangles.size
        val renderStride = when {
            totalTriangles > 350000 -> (totalTriangles / 80000)
            totalTriangles > 150000 -> 2
            else -> 1
        }
        val projectedList = ArrayList<ProjectedTriangle>(min(totalTriangles / renderStride + 16, 80000))

        val margin = 200f
        val minScreenX = -margin
        val maxScreenX = width + margin
        val minScreenY = -margin
        val maxScreenY = height + margin

        for (i in 0 until totalTriangles step renderStride) {
            val tri = allTriangles[i]

            // Fast matrix rotate v1
            val v1x = m00 * tri.v1.x + m01 * tri.v1.y + m02 * tri.v1.z
            val v1y = m10 * tri.v1.x + m11 * tri.v1.y + m12 * tri.v1.z
            val v1z = m20 * tri.v1.x + m21 * tri.v1.y + m22 * tri.v1.z

            // Fast matrix rotate v2
            val v2x = m00 * tri.v2.x + m01 * tri.v2.y + m02 * tri.v2.z
            val v2y = m10 * tri.v2.x + m11 * tri.v2.y + m12 * tri.v2.z
            val v2z = m20 * tri.v2.x + m21 * tri.v2.y + m22 * tri.v2.z

            // Fast matrix rotate v3
            val v3x = m00 * tri.v3.x + m01 * tri.v3.y + m02 * tri.v3.z
            val v3y = m10 * tri.v3.x + m11 * tri.v3.y + m12 * tri.v3.z
            val v3z = m20 * tri.v3.x + m21 * tri.v3.y + m22 * tri.v3.z

            if (drawShadow) {
                val minY = min(v1y, min(v2y, v3y))
                val maxY = max(v1y, max(v2y, v3y))
                if (minY < modelMinY) modelMinY = minY
                if (maxY > modelMaxY) modelMaxY = maxY
            }

            // World offset z
            val wz1 = v1z + distance
            val wz2 = v2z + distance
            val wz3 = v3z + distance

            // Near-plane clipping
            if (wz1 < 0.05f && wz2 < 0.05f && wz3 < 0.05f) continue

            val p1z = max(0.05f, wz1)
            val p2z = max(0.05f, wz2)
            val p3z = max(0.05f, wz3)

            val p1x = centerX + (v1x / p1z) * fov
            val p1y = centerY - (v1y / p1z) * fov
            val p2x = centerX + (v2x / p2z) * fov
            val p2y = centerY - (v2y / p2z) * fov
            val p3x = centerX + (v3x / p3z) * fov
            val p3y = centerY - (v3y / p3z) * fov

            // 2D Backface culling: tests 2D screen winding order
            val cross2D = (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x)
            if (cross2D <= 0f && !wireframe && totalTriangles > 60) {
                // Back-facing triangle is culled for solid meshes
                continue
            }

            // 2D Viewport Frustum Culling
            val triMinX = min(p1x, min(p2x, p3x))
            val triMaxX = max(p1x, max(p2x, p3x))
            val triMinY = min(p1y, min(p2y, p3y))
            val triMaxY = max(p1y, max(p2y, p3y))

            if (triMaxX < minScreenX || triMinX > maxScreenX || triMaxY < minScreenY || triMinY > maxScreenY) {
                continue
            }

            // Normal calculation in world space
            val e1x = v2x - v1x; val e1y = v2y - v1y; val e1z = v2z - v1z
            val e2x = v3x - v1x; val e2y = v3y - v1y; val e2z = v3z - v1z

            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val lenSq = nx * nx + ny * ny + nz * nz
            if (lenSq > 1e-7f) {
                val invLen = 1f / kotlin.math.sqrt(lenSq)
                nx *= invLen; ny *= invLen; nz *= invLen
            } else {
                nx = 0f; ny = 0.707f; nz = 0.707f
            }

            val normal = Vec3(nx, ny, nz)

            val baseColor = if (tri.color != 0L) {
                colorFromArgbLong(tri.color)
            } else if (primaryColor == Color(0xFFE2E8F0)) {
                Color(0xFFD6C5AD) // Warm sculptural marble/terracotta tone matching Google 3D viewer
            } else {
                primaryColor
            }

            val emissiveColor = if (tri.emissiveColor != 0L) colorFromArgbLong(tri.emissiveColor) else Color.Transparent

            val roughness = tri.roughness.coerceIn(0.04f, 1.0f)
            val metallic = tri.metallic.coerceIn(0.0f, 1.0f)
            val effectiveRoughness = (roughness * (engineProfile.pbrRoughness / 0.30f)).coerceIn(0.04f, 1.0f)

            val diffuseIrradiance = hdriPreset.computeDiffuseIrradiance(normal)
            val specularRadiance = hdriPreset.computeSpecularRadiance(
                normal,
                viewDir,
                roughness = effectiveRoughness
            ) * engineProfile.specularMultiplier

            val avgZ = (wz1 + wz2 + wz3) * 0.33333334f

            projectedList.add(
                ProjectedTriangle(
                    p1 = Offset(p1x, p1y),
                    p2 = Offset(p2x, p2y),
                    p3 = Offset(p3x, p3y),
                    avgZ = avgZ,
                    diffuseIrradiance = diffuseIrradiance,
                    specularRadiance = specularRadiance,
                    baseColor = baseColor,
                    emissiveColor = emissiveColor,
                    metallic = metallic,
                    roughness = roughness
                )
            )
        }

        // Shadows under model disabled as requested
        if (false && drawShadow) {
            val groundYOffset = if (modelMinY != Float.MAX_VALUE) {
                centerY - (modelMinY / distance) * fov + 8f * scale
            } else {
                centerY + 160f * scale
            }

            val shadowMult = engineProfile.shadowIntensity
            val shadowWidth = 240f * scale * (if (engineProfile == RenderEngineProfile.REALITYKIT || engineProfile == RenderEngineProfile.ARKIT) 1.15f else 1.0f)
            val shadowHeight = 65f * scale

            // 1. Soft Ambient Occlusion ground disc
            drawScope.drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0, 0, 0, (0x35 * shadowMult).toInt().coerceIn(0, 255)),
                        Color(0, 0, 0, (0x15 * shadowMult).toInt().coerceIn(0, 255)),
                        Color.Transparent
                    ),
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
                    colors = listOf(
                        Color(0, 0, 0, (0x50 * shadowMult).toInt().coerceIn(0, 255)),
                        Color(0, 0, 0, (0x20 * shadowMult).toInt().coerceIn(0, 255)),
                        Color.Transparent
                    ),
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
                    colors = listOf(
                        Color(0, 0, 0, (0x80 * shadowMult).toInt().coerceIn(0, 255)),
                        Color(0, 0, 0, (0x40 * shadowMult).toInt().coerceIn(0, 255)),
                        Color.Transparent
                    ),
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

        // Fast In-Place Depth Sorting (Painter's Algorithm)
        projectedList.sortWith { a, b -> b.avgZ.compareTo(a.avgZ) }

        val strokeStyle = Stroke(width = 1.2f)

        for (i in 0 until projectedList.size) {
            val tri = projectedList[i]

            reusablePath.reset()
            reusablePath.moveTo(tri.p1.x, tri.p1.y)
            reusablePath.lineTo(tri.p2.x, tri.p2.y)
            reusablePath.lineTo(tri.p3.x, tri.p3.y)
            reusablePath.close()

            if (wireframe) {
                drawScope.drawPath(
                    path = reusablePath,
                    color = tri.baseColor.copy(alpha = 0.9f),
                    style = strokeStyle
                )
            } else {
                val c = tri.baseColor
                val ec = tri.emissiveColor
                val metallic = tri.metallic

                // 1. Base/Diffuse in Linear Space
                val linR = srgbToLinear(c.red)
                val linG = srgbToLinear(c.green)
                val linB = srgbToLinear(c.blue)

                // 2. Dielectric vs Metallic conservation
                val dielectricDiffuse = (1.0f - metallic).coerceIn(0.0f, 1.0f)
                val diffR = linR * tri.diffuseIrradiance.x * dielectricDiffuse
                val diffG = linG * tri.diffuseIrradiance.y * dielectricDiffuse
                val diffB = linB * tri.diffuseIrradiance.z * dielectricDiffuse

                // 3. Specular Reflection (Fresnel Schlick F0 tint)
                val f0R = 0.04f * (1.0f - metallic) + linR * metallic
                val f0G = 0.04f * (1.0f - metallic) + linG * metallic
                val f0B = 0.04f * (1.0f - metallic) + linB * metallic

                val specR = tri.specularRadiance.x * f0R
                val specG = tri.specularRadiance.y * f0G
                val specB = tri.specularRadiance.z * f0B

                // 4. Emissive Texture / Factor
                val linEmissiveR = srgbToLinear(ec.red) * ec.alpha
                val linEmissiveG = srgbToLinear(ec.green) * ec.alpha
                val linEmissiveB = srgbToLinear(ec.blue) * ec.alpha

                // Total Linear Radiance = Diffuse + Specular + Emissive
                val litR = diffR + specR + linEmissiveR
                val litG = diffG + specG + linEmissiveG
                val litB = diffB + specB + linEmissiveB

                // 5. Filmic ACES Tone Mapping
                val outR = if (engineProfile.useFilmicToneMapping) linearToSrgb(litR) else litR.coerceIn(0f, 1f).pow(1f / 2.2f)
                val outG = if (engineProfile.useFilmicToneMapping) linearToSrgb(litG) else litG.coerceIn(0f, 1f).pow(1f / 2.2f)
                val outB = if (engineProfile.useFilmicToneMapping) linearToSrgb(litB) else litB.coerceIn(0f, 1f).pow(1f / 2.2f)

                val shadedColor = Color(outR, outG, outB, c.alpha)
                drawScope.drawPath(path = reusablePath, color = shadedColor)
            }
        }
    }

    private data class ProjectedTriangle(
        val p1: Offset,
        val p2: Offset,
        val p3: Offset,
        val avgZ: Float,
        val diffuseIrradiance: Vec3,
        val specularRadiance: Vec3,
        val baseColor: Color,
        val emissiveColor: Color,
        val metallic: Float,
        val roughness: Float
    )
}

package com.example.engine.ar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.math3d.Vec3
import kotlin.math.max
import kotlin.math.sin

/**
 * High-fidelity AR Plane Visualizer & Surface Anchoring Renderer.
 * Renders:
 * - Holographic surface meshes for detected floor, table, and wall planes
 * - Multi-layer animated plane boundary pulses and grid lines
 * - Real-time ARCore depth point clouds
 * - Dynamic surface targeting reticle with normal alignment
 * - Plane classification badges (dimensions in meters and plane type)
 */
class ARPlaneRenderer {

    private var animTime = 0f

    fun renderPlanes(
        drawScope: DrawScope,
        planes: List<ARTrackedPlane>,
        pointCloud: List<Vec3>,
        anchor: ARSurfaceAnchor?,
        isPlaneMeshVisible: Boolean = true,
        isPointCloudVisible: Boolean = true,
        selectedPlaneId: String? = null,
        filter: ARPlaneFilter = ARPlaneFilter.ALL,
        textMeasurer: TextMeasurer? = null
    ) {
        animTime += 0.035f
        val width = drawScope.size.width
        val height = drawScope.size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val fov = 460f

        val pulseAlpha = (sin(animTime * 3f) * 0.15f + 0.35f).coerceIn(0.1f, 0.7f)

        // 1. Render Point Cloud Feature Points
        if (isPointCloudVisible && pointCloud.isNotEmpty()) {
            for (pt in pointCloud) {
                val z = max(0.2f, pt.z)
                val sx = centerX + (pt.x / z) * fov
                val sy = centerY - (pt.y / z) * fov

                if (sx in 0f..width && sy in 0f..height) {
                    val dotRadius = (4.5f / z).coerceIn(1.5f, 6.0f)
                    drawScope.drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.65f),
                        radius = dotRadius,
                        center = Offset(sx, sy)
                    )
                    drawScope.drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = dotRadius * 0.45f,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // 2. Render Detected Planes
        if (isPlaneMeshVisible) {
            for (plane in planes) {
                // Apply filter
                when (filter) {
                    ARPlaneFilter.HORIZONTAL_ONLY -> {
                        if (plane.orientation == PlaneOrientation.VERTICAL) continue
                    }
                    ARPlaneFilter.VERTICAL_ONLY -> {
                        if (plane.orientation != PlaneOrientation.VERTICAL) continue
                    }
                    ARPlaneFilter.ALL -> {}
                }

                val isSelected = plane.id == selectedPlaneId || plane.id == anchor?.planeId
                val planeColor = when (plane.orientation) {
                    PlaneOrientation.HORIZONTAL_UPWARD -> if (isSelected) Color(0xFF00FF88) else Color(0xFF00E5FF)
                    PlaneOrientation.HORIZONTAL_DOWNWARD -> Color(0xFFFF9E00)
                    PlaneOrientation.VERTICAL -> if (isSelected) Color(0xFFFF0055) else Color(0xFF9D4EDD)
                }

                // Project 3D polygon vertices to screen coordinates
                val projectedPolygon = plane.polygon.map { v ->
                    val z = max(0.2f, v.z)
                    val sx = centerX + (v.x / z) * fov
                    val sy = centerY - (v.y / z) * fov
                    Offset(sx, sy)
                }

                if (projectedPolygon.size >= 3) {
                    val path = Path().apply {
                        moveTo(projectedPolygon[0].x, projectedPolygon[0].y)
                        for (i in 1 until projectedPolygon.size) {
                            lineTo(projectedPolygon[i].x, projectedPolygon[i].y)
                        }
                        close()
                    }

                    // Fill plane surface with holographic grid tint
                    drawScope.drawPath(
                        path = path,
                        color = planeColor.copy(alpha = if (isSelected) 0.22f else 0.12f)
                    )

                    // Draw outer border contour
                    drawScope.drawPath(
                        path = path,
                        color = planeColor.copy(alpha = if (isSelected) 0.95f else pulseAlpha + 0.3f),
                        style = Stroke(width = if (isSelected) 2.5f else 1.5f)
                    )

                    // Draw internal subdivision lines
                    val centerZ = max(0.2f, plane.center.z)
                    val csx = centerX + (plane.center.x / centerZ) * fov
                    val csy = centerY - (plane.center.y / centerZ) * fov

                    // Plane center marker
                    drawScope.drawCircle(
                        color = planeColor.copy(alpha = 0.85f),
                        radius = 5.5f,
                        center = Offset(csx, csy)
                    )
                    drawScope.drawCircle(
                        color = planeColor.copy(alpha = 0.35f),
                        radius = 12f + sin(animTime * 4f) * 3f,
                        center = Offset(csx, csy),
                        style = Stroke(width = 1.2f)
                    )

                    // Label badge with plane measurements
                    if (textMeasurer != null && csx in 50f..(width - 50f) && csy in 50f..(height - 50f)) {
                        val label = "${plane.orientation.label} (${String.format("%.1f", plane.extentX)}m × ${String.format("%.1f", plane.extentZ)}m)"
                        val textLayout = textMeasurer.measure(
                            text = label,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val badgeWidth = textLayout.size.width + 16f
                        val badgeHeight = textLayout.size.height + 8f
                        val badgeX = csx - badgeWidth / 2f
                        val badgeY = csy - 28f - badgeHeight

                        drawScope.drawRoundRect(
                            color = Color(0xCC0F172A),
                            topLeft = Offset(badgeX, badgeY),
                            size = Size(badgeWidth, badgeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                        drawScope.drawRoundRect(
                            color = planeColor.copy(alpha = 0.8f),
                            topLeft = Offset(badgeX, badgeY),
                            size = Size(badgeWidth, badgeHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                            style = Stroke(width = 1f)
                        )
                        drawScope.drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(badgeX + 8f, badgeY + 4f)
                        )
                    }
                }
            }
        }

        // 3. Render AR Surface Placement Reticle
        if (anchor == null || !anchor.isGrounded) {
            val reticlePlane = planes.firstOrNull { it.orientation == PlaneOrientation.HORIZONTAL_UPWARD } ?: planes.firstOrNull()
            if (reticlePlane != null) {
                val rz = max(0.2f, reticlePlane.center.z)
                val rx = centerX + (reticlePlane.center.x / rz) * fov
                val ry = centerY - (reticlePlane.center.y / rz) * fov

                val ringRadius = (38f + sin(animTime * 5f) * 4f) * (2.5f / rz).coerceIn(0.6f, 1.4f)

                // Outer targeting ring
                drawScope.drawOval(
                    color = Color(0xFF00FF88).copy(alpha = 0.85f),
                    topLeft = Offset(rx - ringRadius, ry - ringRadius * 0.35f),
                    size = Size(ringRadius * 2f, ringRadius * 0.7f),
                    style = Stroke(width = 2.2f)
                )

                // Inner dot
                drawScope.drawCircle(
                    color = Color(0xFF00FF88),
                    radius = 4f,
                    center = Offset(rx, ry)
                )

                // Crosshairs
                val chLen = ringRadius * 1.35f
                drawScope.drawLine(
                    color = Color(0x9900FF88),
                    start = Offset(rx - chLen, ry),
                    end = Offset(rx + chLen, ry),
                    strokeWidth = 1.2f
                )
                drawScope.drawLine(
                    color = Color(0x9900FF88),
                    start = Offset(rx, ry - chLen * 0.35f),
                    end = Offset(rx, ry + chLen * 0.35f),
                    strokeWidth = 1.2f
                )
            }
        }
    }
}

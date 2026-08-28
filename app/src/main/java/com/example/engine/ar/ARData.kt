package com.example.engine.ar

import com.example.math3d.Vec3

enum class PlaneOrientation(val label: String) {
    HORIZONTAL_UPWARD("Floor / Table"),
    HORIZONTAL_DOWNWARD("Ceiling"),
    VERTICAL("Wall")
}

enum class ARPlaneFilter(val label: String) {
    ALL("All Surfaces"),
    HORIZONTAL_ONLY("Floors & Tables"),
    VERTICAL_ONLY("Walls")
}

enum class ARPlacementMode(val label: String) {
    TAP_TO_PLACE("Tap Surface"),
    SURFACE_LOCKED("Surface Lock"),
    FREE_FLOAT("Free Float")
}

data class ARTrackedPlane(
    val id: String,
    val center: Vec3,
    val normal: Vec3,
    val extentX: Float, // width in meters
    val extentZ: Float, // depth in meters
    val polygon: List<Vec3>, // 3D boundary polygon points
    val orientation: PlaneOrientation,
    val confidence: Float = 0.95f,
    val isSelected: Boolean = false
) {
    val areaM2: Float get() = extentX * extentZ
}

data class ARSurfaceAnchor(
    val id: String = "anchor_primary",
    val planeId: String? = null,
    val position: Vec3 = Vec3(0f, -0.6f, 2.5f),
    val normal: Vec3 = Vec3(0f, 1f, 0f),
    val rotationY: Float = 0f,
    val scale: Float = 1.0f,
    val isGrounded: Boolean = true,
    val surfaceType: PlaneOrientation = PlaneOrientation.HORIZONTAL_UPWARD,
    val arcoreAnchor: com.google.ar.core.Anchor? = null
)

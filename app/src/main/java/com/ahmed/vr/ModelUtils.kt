package com.ahmed.vr

/**
 * ModelUtils contains helpers for computing an initial model scale based on model bounds
 * and placement distance. This is intentionally conservative — the renderer should still
 * allow users to scale/rotate the model after placement.
 */
object ModelUtils {
    /**
     * Compute a uniform scale factor to apply to a model whose largest dimension is [modelSizeMeters]
     * so that it occupies roughly [targetPortionOfDistance] of the distance between camera and anchor.
     *
     * Example: with targetPortionOfDistance = 0.2, a model placed 2m away will be scaled to ~0.4m on its largest axis.
     * If modelSizeMeters <= 0, function returns 1.0 (no-op).
     */
    fun computeUniformScale(modelSizeMeters: Float, distanceMeters: Float, targetPortionOfDistance: Float = 0.2f): Float {
        if (modelSizeMeters <= 0f || distanceMeters <= 0f) return 1.0f
        val targetSize = distanceMeters * targetPortionOfDistance
        return targetSize / modelSizeMeters
    }

    /**
     * Estimate distance from camera to anchor pose using anchor pose translation (assumes camera at origin in camera space).
     * This is a convenience when callers only have the anchor Pose.
     */
    fun estimateDistanceMeters(tx: Float, ty: Float, tz: Float): Float {
        val dx = tx
        val dy = ty
        val dz = tz
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}

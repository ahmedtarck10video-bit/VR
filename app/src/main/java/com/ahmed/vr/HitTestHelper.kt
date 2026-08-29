package com.ahmed.vr

import com.google.ar.core.*
import android.util.Log

/**
 * HitTestHelper implements a prioritized hit-test cascade and explicit hit result states.
 */
class HitTestHelper(private val session: Session) {
    enum class HitType { REAL_ARCORE_HIT, ESTIMATED_FALLBACK, NO_VALID_HIT }
    data class HitResultInfo(val hitResult: HitResult?, val type: HitType)

    fun performHitTest(frame: Frame, x: Float, y: Float): HitResultInfo {
        // 1) Plane hits (prefer plane polygon)
        val hits = frame.hitTest(x, y)
        var fallbackPoint: HitResult? = null
        for (h in hits) {
            val trackable = h.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(h.hitPose)) {
                // Real plane hit
                return HitResultInfo(h, HitType.REAL_ARCORE_HIT)
            }
            if (fallbackPoint == null && trackable is Point) {
                fallbackPoint = h
            }
        }

        // 2) Depth-based hit test (if device supports and ARCore provides it)
        try {
            val depthHits = frame.hitTest(x, y, /*useDepth*/ true)
            if (depthHits.isNotEmpty()) {
                return HitResultInfo(depthHits[0], HitType.REAL_ARCORE_HIT)
            }
        } catch (ex: UnsupportedOperationException) {
            // Depth not supported on this device or session; ignore and continue
            Log.d("HitTestHelper", "Depth hit test not supported: ${ex.message}")
        }

        // 3) Feature point fallback
        if (fallbackPoint != null) {
            return HitResultInfo(fallbackPoint, HitType.ESTIMATED_FALLBACK)
        }

        // 4) No valid hit
        return HitResultInfo(null, HitType.NO_VALID_HIT)
    }
}

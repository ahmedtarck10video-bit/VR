package com.ahmed.vr

import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

/**
 * AnchorManager manages created Anchors to avoid duplicates and leaks.
 * It performs simple proximity deduplication and ensures anchors are detached when cleared.
 */
class AnchorManager {
    private val anchors = CopyOnWriteArrayList<Anchor>()

    // Threshold in meters to consider two anchors "the same" (avoid duplicates)
    private val DUPLICATE_THRESHOLD_METERS = 0.05f // 5 cm

    fun add(candidate: Anchor): Anchor {
        // Check for nearby existing anchors and reuse if close enough to avoid duplicates
        val candidatePose = candidate.pose
        for (existing in anchors) {
            val d = distance(existing.pose, candidatePose)
            if (d <= DUPLICATE_THRESHOLD_METERS) {
                // The candidate is close to an existing anchor: prefer the existing one
                // Clean up the candidate to avoid leak
                try { candidate.detach() } catch (t: Throwable) { /* ignore */ }
                return existing
            }
        }
        anchors.add(candidate)
        return candidate
    }

    fun remove(anchor: Anchor) {
        try {
            anchor.detach()
        } catch (t: Throwable) {
            // best effort
        }
        anchors.remove(anchor)
    }

    fun clearAll() {
        for (a in anchors) {
            try { a.detach() } catch (t: Throwable) { /* best effort */ }
        }
        anchors.clear()
    }

    fun allAnchors(): List<Anchor> = anchors.toList()

    private fun distance(a: Pose, b: Pose): Float {
        val dx = a.tx() - b.tx()
        val dy = a.ty() - b.ty()
        val dz = a.tz() - b.tz()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

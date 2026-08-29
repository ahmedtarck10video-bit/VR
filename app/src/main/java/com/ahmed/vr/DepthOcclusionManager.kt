package com.ahmed.vr

import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Session

/**
 * DepthOcclusionManager performs capability checks and exposes hooks for integrating
 * ARCore depth with the renderer for occlusion. It will not enable depth if unsupported.
 *
 * Integration note: the project renderer (Filament/Sceneform) must consume the camera
 * and depth textures and apply them as occlusion materials. This class does not perform
 * Filament-specific binding; it only safely verifies capability and configures the session.
 */
class DepthOcclusionManager(private val session: Session) {
    companion object {
        private const val TAG = "DepthOcclusionManager"
    }

    var depthSupported: Boolean = false
        private set

    fun setupIfSupported() {
        try {
            // Query session capabilities
            depthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            if (!depthSupported) {
                Log.i(TAG, "Depth not supported on this device/session; occlusion will be disabled")
                // Ensure config does not request depth
                val cfg = session.config
                cfg.depthMode = Config.DepthMode.DISABLED
                session.configure(cfg)
                return
            }

            // If supported, ensure session config requests depth. SessionManager already configures depth when supported,
            // but we double-check here to be explicit.
            val cfg = session.config
            if (cfg.depthMode != Config.DepthMode.AUTOMATIC) {
                cfg.depthMode = Config.DepthMode.AUTOMATIC
                session.configure(cfg)
            }

            // Renderer integration point: application should bind depth texture to its compositor.
            Log.i(TAG, "Depth supported and enabled — renderer should bind depth texture for occlusion")
        } catch (t: Throwable) {
            depthSupported = false
            Log.w(TAG, "Depth occlusion setup failed: ${t.message}")
            try {
                val cfg = session.config
                cfg.depthMode = Config.DepthMode.DISABLED
                session.configure(cfg)
            } catch (_: Throwable) { /* best-effort */ }
        }
    }

    // Placeholder: provide a method to create an occlusion anchor if needed by renderer.
    fun createDepthAnchorAt(anchor: Anchor) {
        // No-op. Renderer-specific code should consume the session's depth image per-frame.
    }
}

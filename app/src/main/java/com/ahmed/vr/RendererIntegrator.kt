package com.ahmed.vr

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Session

/**
 * RendererIntegrator is an application-provided interface that receives per-frame callbacks
 * along with lifecycle events so the app's renderer (Filament/SceneView) can composite the
 * AR camera texture, optional depth texture, and virtual objects into the final image.
 *
 * Implement this interface inside your renderer module and assign it to ArRenderer.integrator.
 */
interface RendererIntegrator {
    /** Called each frame and given the ARCore Frame for extracting camera, light, and tracking info. */
    fun onFrame(frame: Frame)

    /** Called when the AR Session was configured or when capabilities changed (depth enabled/disabled). */
    fun onSessionConfigured(session: Session)

    /** Called when a new anchor was placed and the app wants a renderer to attach a virtual model. */
    fun onAnchorPlaced(anchor: Anchor, initialScale: Float)

    /** Called when an anchor was removed (application should remove rendered content attached to it). */
    fun onAnchorRemoved(anchor: Anchor)
}

package com.ahmed.vr

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Session

/**
 * ArRenderer: minimal GL camera-texture manager that binds ARCore camera texture and
 * advances frames on a periodic callback so that camera passthrough appears promptly after resume.
 * This class intentionally keeps rendering responsibilities minimal — integration with
 * Sceneform/Filament should use the same texture id for their background or external texture.
 */
class ArRenderer(private val sessionManager: SessionManager) {
    companion object {
        private const val TAG = "ArRenderer"
        private const val FRAME_INTERVAL_MS = 16L // ~60fps
    }

    private var cameraTextureId: Int = -1
    private var surfaceTexture: SurfaceTexture? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var running = false
    private val frameRunnable = object : Runnable {
        override fun run() {
            try {
                if (!running) return
                val session = sessionManager.session
                if (session != null && sessionManager.state == SessionManager.State.RUNNING) {
                    val frame: Frame = session.update()
                    // Ensure the SurfaceTexture is updated so the external texture contains the latest camera image
                    try {
                        surfaceTexture?.updateTexImage()
                    } catch (t: Throwable) {
                        Log.w(TAG, "updateTexImage failed: ${t.message}")
                    }
                    // TODO: feed frame to render pipeline (Filament/Sceneform integration point)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Frame update loop exception: ${t.message}")
            } finally {
                mainHandler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }
    }

    fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        // Create external texture if needed
        if (cameraTextureId == -1) {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            cameraTextureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

            // Use the provided SurfaceTexture if the app's view provided one; otherwise create our own
            surfaceTexture = surface
            // If we created our own we would assign: surfaceTexture = SurfaceTexture(cameraTextureId)

            // If session already running, set the camera texture name so ARCore outputs into this texture
            trySetSessionCameraTexture()
        }
    }

    fun onSessionResumed() {
        trySetSessionCameraTexture()
        startFrameLoop()
    }

    private fun trySetSessionCameraTexture() {
        val session = sessionManager.session
        if (session != null && cameraTextureId != -1 && sessionManager.state == SessionManager.State.RUNNING) {
            try {
                session.setCameraTextureName(cameraTextureId)
                Log.i(TAG, "Bound camera texture id $cameraTextureId to ARCore session")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to set camera texture name: ${t.message}")
            }
        }
    }

    private fun startFrameLoop() {
        if (running) return
        running = true
        mainHandler.removeCallbacks(frameRunnable)
        mainHandler.post(frameRunnable)
    }

    fun onPause() {
        running = false
        mainHandler.removeCallbacks(frameRunnable)
    }
}

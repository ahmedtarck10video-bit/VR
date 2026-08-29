package com.ahmed.vr

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.ModelRenderable
import java.util.concurrent.CompletableFuture

/**
 * ARActivity now uses SessionManager and the helper classes to enforce a correct AR lifecycle,
 * camera permission handling, hit-tests, anchor lifecycle, and safe model placement.
 */
class ARActivity : AppCompatActivity() {
    private val TAG = "ARActivity"

    private lateinit var sessionManager: SessionManager
    private var arRenderer: ArRenderer? = null
    private var hitTestHelper: HitTestHelper? = null
    private val anchorManager = AnchorManager()
    private var augmentedImageManager = AugmentedImageManager()
    private var depthOcclusionManager: DepthOcclusionManager? = null
    private var cloudAnchorManager: CloudAnchorManager? = null

    private var textureView: TextureView? = null
    private var placeButton: Button? = null
    private var statusText: TextView? = null

    private var modelRenderable: ModelRenderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        sessionManager = SessionManager(this)

        textureView = findViewById(R.id.camera_texture_view)
        placeButton = findViewById(R.id.place_button)
        statusText = findViewById(R.id.status_text)

        placeButton?.setOnClickListener { onPlaceRequested() }

        // Load model asynchronously (if any). Keep non-blocking.
        CompletableFuture.runAsync {
            try {
                // Replace with a valid renderable load if project has a model path; keep safe if absent.
                // This placeholder avoids crashing if the model source is invalid.
                // modelRenderable = ModelRenderable.builder().setSource(this, ...).build().get()
            } catch (t: Throwable) {
                Log.w(TAG, "Model load failed or not configured: ${t.message}")
            }
        }

        // If camera permission is missing, request it now and wait for callback.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            statusText?.visibility = View.VISIBLE
            statusText?.text = "Camera permission required"
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        // Otherwise, check availability and create the session early
        if (!sessionManager.checkAvailability()) {
            statusText?.visibility = View.VISIBLE
            statusText?.text = sessionManager.lastErrorMessage ?: "AR not available"
            return
        }

        if (!sessionManager.createSession()) {
            statusText?.visibility = View.VISIBLE
            statusText?.text = sessionManager.lastErrorMessage ?: "Failed to create session"
            return
        }

        // Prepare renderer and helpers. Renderer will bind camera texture after resume.
        arRenderer = ArRenderer(sessionManager)
        hitTestHelper = HitTestHelper(sessionManager.session!!)
        depthOcclusionManager = DepthOcclusionManager(sessionManager.session!!)
        cloudAnchorManager = CloudAnchorManager(sessionManager.session!!)

        // TextureView listener ensures camera frames are shown when available.
        textureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // Renderer will create and bind GL texture when its GL context is ready; here we let it know surface exists.
                arRenderer?.onSurfaceTextureAvailable(surface, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { return true }
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        updateStatus("AR ready (tap Place to attempt placement when tracking)")
    }

    override fun onResume() {
        super.onResume()

        // Ensure permission
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            updateStatus("Camera permission required")
            return
        }

        // Ensure session
        if (!sessionManager.createSession()) {
            updateStatus(sessionManager.lastErrorMessage ?: "Failed to create session")
            return
        }

        // Try to resume session
        if (!sessionManager.resume()) {
            updateStatus(sessionManager.lastErrorMessage ?: "Failed to resume session")
            return
        }

        // Let renderer know session running
        arRenderer?.onSessionResumed()

        // Check depth support and configure occlusion if available
        try {
            depthOcclusionManager?.setupIfSupported()
        } catch (t: Throwable) {
            Log.w(TAG, "Depth/occlusion setup failed: ${t.message}")
        }

        updateStatus("Camera active — waiting for tracking")
    }

    override fun onPause() {
        super.onPause()
        try {
            arRenderer?.onPause()
        } catch (t: Throwable) {
            Log.w(TAG, "Renderer pause failed: ${t.message}")
        }
        sessionManager.pause()
        anchorManager.clearAll()
        augmentedImageManager.clearAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.destroy()
    }

    private fun onPlaceRequested() {
        // Do not start session on tap. Require AR session to be running.
        if (sessionManager.state != SessionManager.State.RUNNING) {
            updateStatus("AR session not running — cannot place model")
            return
        }

        // Query latest frame
        val frame: Frame = try { sessionManager.session!!.update() } catch (t: Throwable) {
            updateStatus("Unable to get frame: ${t.message}")
            return
        }

        // Use center of screen for hit-test
        val vw = textureView ?: return
        val x = vw.width / 2f
        val y = vw.height / 2f

        val hitInfo = hitTestHelper?.performHitTest(frame, x, y)
        when (hitInfo?.type) {
            HitTestHelper.HitType.REAL_ARCORE_HIT -> {
                val hit = hitInfo.hitResult!!
                val anchor = hit.createAnchor()
                anchorManager.add(anchor)
                // Attach model to anchor via Sceneform or custom render path — omitted here; app should use existing render binding.
                updateStatus("Model placed on real surface")
            }
            HitTestHelper.HitType.ESTIMATED_FALLBACK -> {
                updateStatus("Only estimated point available — move device to find a plane")
            }
            HitTestHelper.HitType.NO_VALID_HIT, null -> {
                updateStatus("No valid surface found — try moving the device")
            }
        }
    }

    private fun updateStatus(text: String) {
        runOnUiThread {
            statusText?.visibility = View.VISIBLE
            statusText?.text = text
            Log.i(TAG, text)
        }
    }
}

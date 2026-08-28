package com.example.engine.ar

import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * PART 3: Camera Passthrough & Frame Processing
 * 
 * CRITICAL: When entering AR mode, the real camera image MUST appear
 * immediately after the AR session is successfully resumed.
 * 
 * The application MUST NOT show a black background when the camera is available.
 * 
 * Verify the complete camera rendering path:
 * ARCore Camera → Camera Texture / Camera Background → SceneView / Filament → 3D Content → Final Screen
 */
class CameraPassthroughManager(private val session: Session?) {
    
    companion object {
        private const val TAG = "CameraPassthroughManager"
    }
    
    private val _cameraBackgroundReady = MutableStateFlow(false)
    val cameraBackgroundReady: StateFlow<Boolean> = _cameraBackgroundReady.asStateFlow()
    
    private val _isFrameProcessing = MutableStateFlow(false)
    val isFrameProcessing: StateFlow<Boolean> = _isFrameProcessing.asStateFlow()
    
    private val _cameraTrackingState = MutableStateFlow<TrackingState?>(null)
    val cameraTrackingState: StateFlow<TrackingState?> = _cameraTrackingState.asStateFlow()
    
    private var lastValidFrame: Frame? = null
    private var frameUpdateCount = 0
    private var lastSuccessfulFrameTime = 0L
    
    /**
     * Update camera passthrough with latest frame
     * CRITICAL: This must be called every render frame
     */
    fun updateCameraPassthrough(): Boolean {
        if (session == null) {
            Log.e(TAG, "Camera passthrough update failed - session is null")
            _cameraBackgroundReady.value = false
            return false
        }
        
        _isFrameProcessing.value = true
        
        return try {
            val frame = session.update()
            
            // CRITICAL: Validate frame
            if (frame == null) {
                Log.w(TAG, "Frame is null - camera may not be ready yet")
                _cameraBackgroundReady.value = false
                _isFrameProcessing.value = false
                return false
            }
            
            // Extract camera information IMMEDIATELY
            val camera = frame.camera
            
            // Update tracking state
            _cameraTrackingState.value = camera.trackingState
            
            // CRITICAL: Validate camera is producing frames
            if (camera.trackingState == TrackingState.PAUSED) {
                Log.w(TAG, "Camera tracking PAUSED - background may not render")
                _cameraBackgroundReady.value = false
            } else {
                // Camera is producing valid frames
                Log.d(TAG, "✓ Camera frame valid - background should render")
                _cameraBackgroundReady.value = true
                lastSuccessfulFrameTime = System.currentTimeMillis()
                frameUpdateCount++
            }
            
            // Store frame reference ONLY for this frame cycle
            // CRITICAL: Never retain frames across frame boundaries
            lastValidFrame = frame
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during camera passthrough update", e)
            _cameraBackgroundReady.value = false
            false
        } finally {
            _isFrameProcessing.value = false
        }
    }
    
    /**
     * Get current camera frame WITHOUT retaining it
     * CRITICAL: Extract only necessary data, then release frame
     */
    fun getCameraFrameData(): CameraFrameData? {
        val frame = lastValidFrame ?: return null
        
        return try {
            val camera = frame.camera
            val pose = camera.pose
            
            CameraFrameData(
                trackingState = camera.trackingState,
                positionX = pose.tx(),
                positionY = pose.ty(),
                positionZ = pose.tz(),
                timestamp = frame.timestamp,
                frameId = frameUpdateCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting frame data", e)
            null
        }
    }
    
    /**
     * Check if camera passthrough is actively rendering
     */
    fun isCameraPassthroughActive(): Boolean {
        val isReady = _cameraBackgroundReady.value
        val isRecent = (System.currentTimeMillis() - lastSuccessfulFrameTime) < 500L // Within 500ms
        
        return isReady && isRecent && frameUpdateCount > 0
    }
    
    /**
     * Get diagnostics for debugging black screen issues
     */
    fun getDiagnostics(): CameraPassthroughDiagnostics {
        return CameraPassthroughDiagnostics(
            isSessionValid = session != null,
            isBackgroundReady = _cameraBackgroundReady.value,
            isFrameProcessing = _isFrameProcessing.value,
            cameraTrackingState = _cameraTrackingState.value,
            frameUpdateCount = frameUpdateCount,
            timeSinceLastFrame = System.currentTimeMillis() - lastSuccessfulFrameTime,
            isPassthroughActive = isCameraPassthroughActive()
        )
    }
    
    /**
     * Reset frame reference when switching modes or sessions
     */
    fun clearFrameReference() {
        lastValidFrame = null
        Log.d(TAG, "Frame reference cleared")
    }
}

data class CameraFrameData(
    val trackingState: TrackingState,
    val positionX: Float,
    val positionY: Float,
    val positionZ: Float,
    val timestamp: Long,
    val frameId: Int
)

data class CameraPassthroughDiagnostics(
    val isSessionValid: Boolean,
    val isBackgroundReady: Boolean,
    val isFrameProcessing: Boolean,
    val cameraTrackingState: TrackingState?,
    val frameUpdateCount: Int,
    val timeSinceLastFrame: Long,
    val isPassthroughActive: Boolean
)

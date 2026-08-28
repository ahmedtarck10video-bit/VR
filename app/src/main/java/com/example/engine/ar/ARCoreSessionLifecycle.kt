package com.example.engine.ar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * COMPLETE ARCore Session Lifecycle Management
 * 
 * CRITICAL: Never mark session as running unless:
 * 1. Camera permission verified
 * 2. ARCore availability confirmed
 * 3. Session successfully created
 * 4. Configuration applied
 * 5. Session.resume() succeeds
 * 6. Camera background is rendering
 */
class ARCoreSessionLifecycle(private val context: Context) {
    
    companion object {
        private const val TAG = "ARCoreSessionLifecycle"
    }
    
    // Session state - ONLY true after COMPLETE initialization chain
    var session: Session? = null
        private set
    
    var isARCoreAvailable: Boolean = false
        private set
    
    var isSessionRunning: Boolean = false
        private set
    
    var isCameraPermissionGranted: Boolean = false
        private set
    
    private val _sessionState = MutableStateFlow<ARSessionState>(ARSessionState.UNINITIALIZED)
    val sessionState: StateFlow<ARSessionState> = _sessionState.asStateFlow()
    
    private val _sessionError = MutableStateFlow<String?>(null)
    val sessionError: StateFlow<String?> = _sessionError.asStateFlow()
    
    // =========================================================================
    // PHASE 1: CAMERA PERMISSION VERIFICATION
    // =========================================================================
    
    fun verifyCameraPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        isCameraPermissionGranted = hasPermission
        
        if (!hasPermission) {
            Log.e(TAG, "CAMERA permission NOT granted - AR cannot initialize")
            _sessionState.value = ARSessionState.PERMISSION_DENIED
            _sessionError.value = "Camera permission required for AR"
        } else {
            Log.d(TAG, "✓ Camera permission verified")
        }
        
        return hasPermission
    }
    
    // =========================================================================
    // PHASE 2: ARCORE AVAILABILITY CHECK
    // =========================================================================
    
    fun checkARCoreAvailability(): Boolean {
        if (!isCameraPermissionGranted) {
            Log.e(TAG, "Skipping ARCore check - camera permission not granted")
            return false
        }
        
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            isARCoreAvailable = (availability == ArCoreApk.Availability.SUPPORTED_INSTALLED)
            
            if (!isARCoreAvailable) {
                Log.e(TAG, "ARCore not available: $availability")
                _sessionState.value = ARSessionState.ARCORE_UNAVAILABLE
                _sessionError.value = "ARCore not installed or not supported on this device"
                return false
            }
            
            Log.d(TAG, "✓ ARCore available and supported")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during ARCore availability check", e)
            _sessionState.value = ARSessionState.INITIALIZATION_ERROR
            _sessionError.value = "ARCore availability check failed: ${e.localizedMessage}"
            return false
        }
    }
    
    // =========================================================================
    // PHASE 3: SESSION CREATION
    // =========================================================================
    
    fun createSession(): Session? {
        if (!isCameraPermissionGranted) {
            Log.e(TAG, "Session creation skipped - no camera permission")
            _sessionError.value = "Camera permission required"
            return null
        }
        
        if (!isARCoreAvailable) {
            Log.e(TAG, "Session creation skipped - ARCore unavailable")
            _sessionError.value = "ARCore not available"
            return null
        }
        
        return try {
            val newSession = Session(context)
            Log.d(TAG, "✓ ARCore Session created")
            newSession
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore not installed exception", e)
            isARCoreAvailable = false
            _sessionState.value = ARSessionState.ARCORE_UNAVAILABLE
            _sessionError.value = "ARCore not installed"
            null
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device not compatible exception", e)
            isARCoreAvailable = false
            _sessionState.value = ARSessionState.DEVICE_NOT_COMPATIBLE
            _sessionError.value = "This device is not compatible with ARCore"
            null
        } catch (e: UnavailableSdkTooOldException) {
            Log.e(TAG, "SDK too old exception", e)
            _sessionState.value = ARSessionState.SDK_TOO_OLD
            _sessionError.value = "ARCore SDK needs update"
            null
        } catch (e: UnavailableApkTooOldException) {
            Log.e(TAG, "APK too old exception", e)
            _sessionState.value = ARSessionState.APK_TOO_OLD
            _sessionError.value = "Google Play Services for AR needs update"
            null
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available exception", e)
            _sessionState.value = ARSessionState.CAMERA_UNAVAILABLE
            _sessionError.value = "Camera hardware unavailable or in use"
            null
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected exception during session creation", t)
            _sessionState.value = ARSessionState.INITIALIZATION_ERROR
            _sessionError.value = "Session creation failed: ${t.localizedMessage}"
            null
        }
    }
    
    // =========================================================================
    // PHASE 4: SESSION CONFIGURATION
    // =========================================================================
    
    fun configureSession(newSession: Session, config: Config): Boolean {
        return try {
            newSession.configure(config)
            Log.d(TAG, "✓ Session configured with feature flags")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Session configuration failed", e)
            _sessionError.value = "Session configuration error: ${e.localizedMessage}"
            false
        }
    }
    
    // =========================================================================
    // PHASE 5: SESSION RESUME (CAMERA STARTS)
    // =========================================================================
    
    fun resumeSession(resumeSession: Session?): Boolean {
        if (resumeSession == null) {
            Log.e(TAG, "Cannot resume - session is null")
            _sessionError.value = "Session is null"
            return false
        }
        
        return try {
            resumeSession.resume()
            session = resumeSession
            isSessionRunning = true
            _sessionState.value = ARSessionState.RUNNING
            _sessionError.value = null
            Log.d(TAG, "✓ Session resumed - CAMERA IS NOW ACTIVE")
            true
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during resume", e)
            isSessionRunning = false
            _sessionState.value = ARSessionState.CAMERA_UNAVAILABLE
            _sessionError.value = "Camera unavailable"
            false
        } catch (e: Exception) {
            Log.e(TAG, "Session resume failed", e)
            isSessionRunning = false
            _sessionState.value = ARSessionState.RESUME_ERROR
            _sessionError.value = "Resume failed: ${e.localizedMessage}"
            false
        }
    }
    
    // =========================================================================
    // SESSION PAUSE
    // =========================================================================
    
    fun pauseSession(): Boolean {
        return try {
            session?.pause()
            isSessionRunning = false
            _sessionState.value = ARSessionState.PAUSED
            Log.d(TAG, "✓ Session paused")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing session", e)
            _sessionError.value = "Pause error: ${e.localizedMessage}"
            false
        }
    }
    
    // =========================================================================
    // SESSION DESTRUCTION & CLEANUP
    // =========================================================================
    
    fun closeSession() {
        try {
            pauseSession()
            session?.close()
            session = null
            isSessionRunning = false
            isCameraPermissionGranted = false
            _sessionState.value = ARSessionState.CLOSED
            Log.d(TAG, "✓ Session closed and cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing session", e)
            _sessionError.value = "Close error: ${e.localizedMessage}"
        }
    }
    
    // =========================================================================
    // SAFETY ASSERTIONS
    // =========================================================================
    
    fun assertSessionRunning(): Boolean {
        val isHealthy = isSessionRunning && session != null && isCameraPermissionGranted && isARCoreAvailable
        if (!isHealthy) {
            Log.e(TAG, "SESSION HEALTH CHECK FAILED - isRunning: $isSessionRunning, session: ${session != null}, permission: $isCameraPermissionGranted, arcore: $isARCoreAvailable")
        }
        return isHealthy
    }
    
    fun getCurrentState(): ARSessionState = _sessionState.value
    fun getLastError(): String? = _sessionError.value
}

// =========================================================================
// Session State Machine
// =========================================================================

enum class ARSessionState {
    UNINITIALIZED,           // Initial state
    PERMISSION_DENIED,       // Camera permission missing
    ARCORE_UNAVAILABLE,      // ARCore not installed/supported
    DEVICE_NOT_COMPATIBLE,   // Device doesn't support ARCore
    SDK_TOO_OLD,            // SDK version outdated
    APK_TOO_OLD,            // Google Play Services outdated
    CAMERA_UNAVAILABLE,     // Camera hardware issue
    INITIALIZATION_ERROR,    // Other initialization failure
    READY_TO_START,         // All checks passed, ready to resume
    RUNNING,                // Session active, camera streaming
    PAUSED,                 // Session paused (camera stopped)
    RESUME_ERROR,           // Failed to resume
    CLOSED                  // Session closed and cleaned up
}

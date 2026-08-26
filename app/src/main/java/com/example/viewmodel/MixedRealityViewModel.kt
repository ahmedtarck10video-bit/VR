package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.HdriPreset
import com.example.engine.RenderEngineProfile
import com.example.engine.SensorOrientation
import com.example.engine.SensorTracker
import com.example.engine.ar.ARCoreManager
import com.example.engine.ar.ARPlacementMode
import com.example.engine.ar.ARPlaneFilter
import com.example.engine.ar.ARSurfaceAnchor
import com.example.engine.ar.ARTrackedPlane
import com.example.engine.ar.PlaneOrientation
import com.example.math3d.Model3D
import com.example.math3d.ModelFileLoader
import com.example.math3d.Vec3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SpatialMode(val label: String) {
    MR("MR"),
    AR("AR"),
    OBJECT("Object")
}

enum class MRSubMode(val title: String, val desc: String) {
    STEREO_PASSTHROUGH("Stereo Dual Camera", "Dual-eye passthrough with IPD adjustment for VR/MR glasses"),
    SPATIAL_HOLO("Spatial Hologram", "Holographic projection with spatial grid and light rings"),
    SURFACE_ANCHOR("Surface Anchor", "Physics-grounded surface anchor with realistic shadow")
}

enum class AppTab {
    STUDIO_3D,
    AR_MODE,
    STEREO_VR
}

enum class SpatialAppId(val title: String) {
    STUDIO_3D("3D Studio"),
    AR_MODE("AR Space"),
    STEREO_VR("VR Headset"),
    GALLERY("Spatial Files"),
    NOTES("Spatial Notes"),
    SETTINGS("Settings")
}

enum class SpatialEnvironment(val displayName: String, val gradientColors: List<Color>) {
    HORIZON("Glass Horizon", listOf(Color(0xFF0F172A), Color(0xFF020617), Color(0xFF1E1B4B))),
    CYBER_VOID("Cyber Void", listOf(Color(0xFF0B0E14), Color(0xFF0D1B2A), Color(0xFF001220))),
    DEEP_SPACE("Deep Nebula", listOf(Color(0xFF19002E), Color(0xFF0B001A), Color(0xFF240046))),
    STUDIO("Pro Studio", listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155)))
}

data class WindowState(
    val appId: SpatialAppId,
    val isOpen: Boolean = true,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false
)

data class MRUiState(
    val currentMode: SpatialMode = SpatialMode.OBJECT,
    val mrSubMode: MRSubMode = MRSubMode.STEREO_PASSTHROUGH,
    val currentTab: AppTab = AppTab.STUDIO_3D,
    val activeAppId: SpatialAppId = SpatialAppId.STUDIO_3D,
    val openWindows: Map<SpatialAppId, WindowState> = mapOf(
        SpatialAppId.STUDIO_3D to WindowState(SpatialAppId.STUDIO_3D, isOpen = true)
    ),
    val isLauncherOpen: Boolean = false,
    val environment: SpatialEnvironment = SpatialEnvironment.HORIZON,
    val spatialAudioEnabled: Boolean = true,
    val currentModel: Model3D? = null,
    val loadedModelUri: Uri? = null,
    val selectedModelIndex: Int = 0,
    val models: List<Model3D> = emptyList(),
    val rotX: Float = 0.2f,
    val rotY: Float = 0.4f,
    val scale: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val isAutoSpin: Boolean = false,
    val isWireframe: Boolean = false,
    val isGyroEnabled: Boolean = true,
    val modelColor: Color = Color(0xFFE2DCD4),
    val sensorOrientation: SensorOrientation = SensorOrientation(),
    val arSurfaceDetected: Boolean = true,
    val arAnchorPlaced: Boolean = true,
    val ipdDistance: Float = 0.12f,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val showPhotoFlash: Boolean = false,
    val hdriPreset: HdriPreset = HdriPreset.STUDIO_PRO,
    val renderEngineProfile: RenderEngineProfile = RenderEngineProfile.SCENEVIEW,
    val isModelPickerOpen: Boolean = false,
    val isLoadingModel: Boolean = false,
    val notificationMessage: String? = null,

    // ARCore & AR Foundation Plane Detection State
    val detectedPlanes: List<ARTrackedPlane> = emptyList(),
    val pointCloud: List<Vec3> = emptyList(),
    val surfaceAnchor: ARSurfaceAnchor? = null,
    val isPlaneMeshVisible: Boolean = true,
    val isPointCloudVisible: Boolean = true,
    val planeFilter: ARPlaneFilter = ARPlaneFilter.ALL,
    val placementMode: ARPlacementMode = ARPlacementMode.TAP_TO_PLACE,
    val selectedPlaneId: String? = null,
    val arCoreStatus: String = "AR Surface Scanner Active",
    val lightIntensity: Float = 1.0f
)

class MixedRealityViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorTracker = SensorTracker(application)
    val arCoreManager = ARCoreManager(application)
    private var recordingJob: Job? = null

    private val _uiState = MutableStateFlow(MRUiState())
    val uiState: StateFlow<MRUiState> = _uiState.asStateFlow()

    init {
        sensorTracker.start()
        arCoreManager.start()

        _uiState.value = _uiState.value.copy(
            models = emptyList(),
            currentModel = null,
            selectedModelIndex = 0
        )

        // Observe sensors and update AR tracking
        viewModelScope.launch {
            sensorTracker.orientation.collect { orientation ->
                _uiState.value = _uiState.value.copy(sensorOrientation = orientation)
                arCoreManager.updateFrame(
                    pitch = orientation.pitch,
                    roll = orientation.roll,
                    yaw = orientation.yaw
                )
            }
        }

        // Collect ARCore detected planes
        viewModelScope.launch {
            arCoreManager.trackedPlanes.collect { planes ->
                _uiState.value = _uiState.value.copy(
                    detectedPlanes = planes,
                    arSurfaceDetected = planes.isNotEmpty()
                )
            }
        }

        // Collect ARCore point cloud
        viewModelScope.launch {
            arCoreManager.pointCloud.collect { points ->
                _uiState.value = _uiState.value.copy(pointCloud = points)
            }
        }

        // Collect ARCore tracking status
        viewModelScope.launch {
            arCoreManager.trackingStatus.collect { status ->
                _uiState.value = _uiState.value.copy(arCoreStatus = status)
            }
        }

        // Collect ARCore light estimation
        viewModelScope.launch {
            arCoreManager.lightIntensity.collect { intensity ->
                _uiState.value = _uiState.value.copy(lightIntensity = intensity)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorTracker.stop()
        arCoreManager.destroy()
    }

    fun setMode(mode: SpatialMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode)
        if (mode == SpatialMode.AR || mode == SpatialMode.MR) {
            arCoreManager.start()
        }
    }

    fun setMRSubMode(subMode: MRSubMode) {
        _uiState.value = _uiState.value.copy(mrSubMode = subMode)
        showNotification(subMode.title)
    }

    fun toggleGyro() {
        val newState = !_uiState.value.isGyroEnabled
        _uiState.value = _uiState.value.copy(isGyroEnabled = newState)
        showNotification(if (newState) "Gyroscope Enabled" else "Gyroscope Disabled")
    }

    fun calibrateGyro() {
        sensorTracker.calibrate()
        showNotification("Gyroscope Calibrated")
    }

    // =========================================================================
    // ARCORE PLANE HIT-TESTING & SURFACE PLACEMENT
    // =========================================================================

    /**
     * Hit tests normalized screen tap coordinates against physical detected planes.
     */
    fun onSurfaceTapped(screenNormX: Float, screenNormY: Float) {
        val hitResult = arCoreManager.hitTest(screenNormX, screenNormY)
        if (hitResult != null) {
            val (plane, hitPoint) = hitResult
            val anchor = ARSurfaceAnchor(
                id = "anchor_${System.currentTimeMillis()}",
                planeId = plane.id,
                position = hitPoint,
                normal = plane.normal,
                rotationY = _uiState.value.rotY,
                scale = _uiState.value.scale,
                isGrounded = true,
                surfaceType = plane.orientation
            )

            _uiState.value = _uiState.value.copy(
                surfaceAnchor = anchor,
                selectedPlaneId = plane.id,
                arAnchorPlaced = true,
                panX = 0f,
                panY = 0f
            )
            showNotification("Object Anchored to ${plane.orientation.label}")
        } else {
            showNotification("No surface detected at tap location")
        }
    }

    fun placeModelOnDetectedSurface() {
        val planes = _uiState.value.detectedPlanes
        val targetPlane = planes.firstOrNull { it.orientation == PlaneOrientation.HORIZONTAL_UPWARD }
            ?: planes.firstOrNull()

        if (targetPlane != null) {
            val anchor = ARSurfaceAnchor(
                id = "anchor_snapped",
                planeId = targetPlane.id,
                position = targetPlane.center,
                normal = targetPlane.normal,
                rotationY = _uiState.value.rotY,
                scale = _uiState.value.scale,
                isGrounded = true,
                surfaceType = targetPlane.orientation
            )
            _uiState.value = _uiState.value.copy(
                surfaceAnchor = anchor,
                selectedPlaneId = targetPlane.id,
                arAnchorPlaced = true,
                panX = 0f,
                panY = 0f
            )
            showNotification("Anchored to ${targetPlane.orientation.label}")
        } else {
            showNotification("Scanning for physical surface...")
        }
    }

    fun clearSurfaceAnchor() {
        _uiState.value = _uiState.value.copy(
            surfaceAnchor = null,
            selectedPlaneId = null,
            arAnchorPlaced = false
        )
        showNotification("Anchor Cleared")
    }

    fun togglePlaneMesh() {
        val newVal = !_uiState.value.isPlaneMeshVisible
        _uiState.value = _uiState.value.copy(isPlaneMeshVisible = newVal)
        showNotification(if (newVal) "Plane Meshes Visible" else "Plane Meshes Hidden")
    }

    fun togglePointCloud() {
        val newVal = !_uiState.value.isPointCloudVisible
        _uiState.value = _uiState.value.copy(isPointCloudVisible = newVal)
        showNotification(if (newVal) "Feature Points Visible" else "Feature Points Hidden")
    }

    fun setPlaneFilter(filter: ARPlaneFilter) {
        _uiState.value = _uiState.value.copy(planeFilter = filter)
        showNotification("Filter: ${filter.label}")
    }

    fun setPlacementMode(mode: ARPlacementMode) {
        _uiState.value = _uiState.value.copy(placementMode = mode)
        showNotification("Mode: ${mode.label}")
    }

    fun selectPlane(planeId: String) {
        _uiState.value = _uiState.value.copy(selectedPlaneId = planeId)
        val plane = _uiState.value.detectedPlanes.firstOrNull { it.id == planeId }
        if (plane != null) {
            showNotification("Selected: ${plane.orientation.label} (${String.format("%.1f", plane.extentX)}m × ${String.format("%.1f", plane.extentZ)}m)")
        }
    }

    fun loadModelFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingModel = true, loadedModelUri = uri)
            val model = withContext(Dispatchers.IO) {
                ModelFileLoader.loadModelFromUri(context, uri)
            }
            if (model != null && model.triangles.isNotEmpty()) {
                val updatedModels = listOf(model) + _uiState.value.models.filter { it.name != model.name }
                _uiState.value = _uiState.value.copy(
                    currentModel = model,
                    loadedModelUri = uri,
                    models = updatedModels,
                    selectedModelIndex = 0,
                    isLoadingModel = false,
                    rotX = 0.15f,
                    rotY = 0.35f,
                    scale = 1.0f,
                    panX = 0f,
                    panY = 0f
                )
                showNotification("Loaded: ${model.name} (${model.triangles.size} polygons)")
            } else {
                _uiState.value = _uiState.value.copy(isLoadingModel = false)
                showNotification("Could not parse 3D model. Supported: .glb, .gltf, .usdz, .obj, .stl")
            }
        }
    }

    fun openInGoogleSceneViewer(context: Context) {
        val uri = _uiState.value.loadedModelUri
        if (uri == null) {
            showNotification("Please load a 3D model first")
            return
        }
        try {
            val sceneViewerIntent = Intent(Intent.ACTION_VIEW)
            val intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                .appendQueryParameter("file", uri.toString())
                .appendQueryParameter("mode", "ar_preferred")
                .appendQueryParameter("resizable", "true")
                .build()
            sceneViewerIntent.data = intentUri
            sceneViewerIntent.setPackage("com.google.android.googlequicksearchbox")
            sceneViewerIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.startActivity(sceneViewerIntent)
        } catch (e: Exception) {
            try {
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://arvr.google.com/scene-viewer/1.0?file=$uri&mode=ar_preferred"))
                genericIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.startActivity(genericIntent)
            } catch (e2: Exception) {
                showNotification("Google Scene Viewer not available on this device")
            }
        }
    }

    fun selectModel(index: Int) {
        if (index in _uiState.value.models.indices) {
            val model = _uiState.value.models[index]
            _uiState.value = _uiState.value.copy(
                selectedModelIndex = index,
                currentModel = model,
                isModelPickerOpen = false
            )
            showNotification("Loaded ${model.name}")
        }
    }

    fun setModelPickerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isModelPickerOpen = isOpen)
    }

    fun triggerPhotoCapture() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showPhotoFlash = true)
            delay(180)
            _uiState.value = _uiState.value.copy(showPhotoFlash = false)
            showNotification("Spatial Snapshot Saved")
        }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            recordingJob?.cancel()
            _uiState.value = _uiState.value.copy(isRecording = false, recordingSeconds = 0)
            showNotification("Spatial Recording Saved")
        } else {
            _uiState.value = _uiState.value.copy(isRecording = true, recordingSeconds = 0)
            recordingJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _uiState.value = _uiState.value.copy(
                        recordingSeconds = _uiState.value.recordingSeconds + 1
                    )
                }
            }
        }
    }

    fun setHdriPreset(preset: HdriPreset) {
        _uiState.value = _uiState.value.copy(hdriPreset = preset)
        showNotification("HDRi Lighting: ${preset.title}")
    }

    fun cycleHdriPreset() {
        val presets = HdriPreset.values()
        val nextIndex = (presets.indexOf(_uiState.value.hdriPreset) + 1) % presets.size
        setHdriPreset(presets[nextIndex])
    }

    fun setRenderEngineProfile(profile: RenderEngineProfile) {
        _uiState.value = _uiState.value.copy(renderEngineProfile = profile)
        showNotification("Engine Profile: ${profile.title}")
    }

    fun cycleRenderEngineProfile() {
        val profiles = RenderEngineProfile.values()
        val nextIndex = (profiles.indexOf(_uiState.value.renderEngineProfile) + 1) % profiles.size
        setRenderEngineProfile(profiles[nextIndex])
    }

    fun clearAll() {
        _uiState.value = _uiState.value.copy(
            currentModel = null,
            models = emptyList(),
            rotX = 0.2f,
            rotY = 0.4f,
            scale = 1.0f,
            panX = 0f,
            panY = 0f,
            arAnchorPlaced = false,
            surfaceAnchor = null
        )
        showNotification("Cleared Model & Canvas")
    }

    fun resetView() {
        _uiState.value = _uiState.value.copy(
            rotX = 0.15f,
            rotY = 0.35f,
            scale = 1.0f,
            panX = 0f,
            panY = 0f
        )
        showNotification("View Reset")
    }

    fun resetPosition() {
        _uiState.value = _uiState.value.copy(
            panX = 0f,
            panY = 0f,
            scale = 1.0f,
            rotX = 0.15f,
            rotY = 0.35f
        )
        showNotification("Centered Model")
    }

    fun showNotification(msg: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notificationMessage = msg)
            delay(2200)
            if (_uiState.value.notificationMessage == msg) {
                _uiState.value = _uiState.value.copy(notificationMessage = null)
            }
        }
    }

    fun updateRotation(deltaX: Float, deltaY: Float) {
        _uiState.value = _uiState.value.copy(
            rotX = _uiState.value.rotX + deltaX,
            rotY = _uiState.value.rotY + deltaY
        )
    }

    fun updateScale(zoomFactor: Float) {
        val newScale = (_uiState.value.scale * zoomFactor).coerceIn(0.2f, 5.0f)
        _uiState.value = _uiState.value.copy(scale = newScale)
    }

    fun updatePan(dx: Float, dy: Float) {
        _uiState.value = _uiState.value.copy(
            panX = _uiState.value.panX + dx,
            panY = _uiState.value.panY + dy
        )
    }

    fun toggleAutoSpin() {
        _uiState.value = _uiState.value.copy(isAutoSpin = !_uiState.value.isAutoSpin)
    }

    fun toggleWireframe() {
        _uiState.value = _uiState.value.copy(isWireframe = !_uiState.value.isWireframe)
    }

    fun setModelColor(color: Color) {
        _uiState.value = _uiState.value.copy(modelColor = color)
    }

    fun setIpdDistance(ipd: Float) {
        _uiState.value = _uiState.value.copy(ipdDistance = ipd)
    }

    fun toggleArAnchor() {
        if (_uiState.value.surfaceAnchor != null) {
            clearSurfaceAnchor()
        } else {
            placeModelOnDetectedSurface()
        }
    }

    fun openApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        currentWindows[appId] = WindowState(appId = appId, isOpen = true, isMinimized = false)
        _uiState.value = _uiState.value.copy(
            openWindows = currentWindows,
            activeAppId = appId,
            isLauncherOpen = false
        )
    }

    fun closeApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        currentWindows[appId] = currentWindows[appId]?.copy(isOpen = false) ?: WindowState(appId, isOpen = false)
        val remainingActive = currentWindows.filter { it.value.isOpen && !it.value.isMinimized }.keys.lastOrNull()
        _uiState.value = _uiState.value.copy(
            openWindows = currentWindows,
            activeAppId = remainingActive ?: SpatialAppId.STUDIO_3D
        )
    }

    fun minimizeApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        currentWindows[appId] = currentWindows[appId]?.copy(isMinimized = true) ?: WindowState(appId, isOpen = true, isMinimized = true)
        _uiState.value = _uiState.value.copy(openWindows = currentWindows)
    }

    fun toggleMaximize(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        val current = currentWindows[appId] ?: WindowState(appId)
        currentWindows[appId] = current.copy(isMaximized = !current.isMaximized, isMinimized = false, isOpen = true)
        _uiState.value = _uiState.value.copy(openWindows = currentWindows, activeAppId = appId)
    }

    fun setLauncherOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isLauncherOpen = isOpen)
    }

    fun setEnvironment(env: SpatialEnvironment) {
        _uiState.value = _uiState.value.copy(environment = env)
    }

    fun toggleSpatialAudio() {
        _uiState.value = _uiState.value.copy(spatialAudioEnabled = !_uiState.value.spatialAudioEnabled)
    }
}


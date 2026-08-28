package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.HdriPreset
import com.example.engine.RenderEngineProfile
import com.example.engine.SensorOrientation
import com.example.engine.SensorTracker
import com.example.engine.ar.*
import com.example.math3d.Model3D
import com.example.math3d.ModelFileLoader
import com.example.math3d.Vec3
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class SpatialMode(val title: String, val badge: String) {
    OBJECT("3D Object", "Pure 3D"),
    AR("Augmented Reality", "6DoF Camera"),
    MR("Mixed Reality", "MR Pass-through");

    val label: String get() = title
}

enum class MRSubMode(val title: String) {
    PASS_THROUGH("AR Passthrough"),
    SPATIAL_PORTAL("Spatial Portal"),
    HOLO_DECK("Holo Deck"),
    AI_SEMANTIC_VISION("AI Semantic Vision")
}

enum class MeshQuality(val label: String) {
    LOW("Fast (Low)"),
    MEDIUM("Balanced"),
    HIGH("Ultra Precision"),
    CINEMATIC("Cinematic PBR")
}

enum class SpatialAppId(val displayName: String) {
    STUDIO_3D("3D Studio"),
    AR_MODE("AR Space"),
    STEREO_VR("VR Vision"),
    GALLERY("Holo Files"),
    NOTES("Spatial Notes"),
    SETTINGS("Settings")
}

data class WindowState(
    val appId: SpatialAppId,
    val isOpen: Boolean = true,
    val isMinimized: Boolean = false,
    val isMaximized: Boolean = false,
    val zIndex: Int = 0
)

enum class SpatialEnvironment(val displayName: String, val hdriPreset: HdriPreset) {
    STUDIO_VOID("Studio Void", HdriPreset.STUDIO_PRO),
    GOLDEN_SUNSET("Golden Sunset", HdriPreset.GOLDEN_HOUR),
    CYBERPUNK_NEON("Cyberpunk Neon", HdriPreset.CYBER_NEON),
    URBAN_SKY("Urban Daylight", HdriPreset.URBAN_DAYLIGHT),
    FOREST_CANOPY("Forest Canopy", HdriPreset.FOREST_CANOPY)
}

data class MRUiState(
    val currentMode: SpatialMode = SpatialMode.OBJECT,
    val mrSubMode: MRSubMode = MRSubMode.PASS_THROUGH,
    val gyroEnabled: Boolean = true,
    val isGyroEnabled: Boolean = true,
    val orientation: SensorOrientation = SensorOrientation(),
    val sensorOrientation: SensorOrientation = SensorOrientation(),
    val models: List<Model3D> = emptyList(),
    val selectedModelIndex: Int = 0,
    val currentModel: Model3D? = null,
    val scale: Float = 1.0f,
    val rotX: Float = 0f,
    val rotY: Float = 0f,
    val rotZ: Float = 0f,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val ipdDistance: Float = 0.064f,
    val showPhotoFlash: Boolean = false,
    val isWireframe: Boolean = false,
    val isLightingEnabled: Boolean = true,
    val isAutoRotating: Boolean = false,
    val isAutoSpin: Boolean = false,
    val isXRayEnabled: Boolean = false,
    val isGhostMode: Boolean = false,
    val isThermalVision: Boolean = false,
    val isNightVision: Boolean = false,
    val isEdgeDetection: Boolean = false,
    val isHologramGlitch: Boolean = false,
    val isDepthColoring: Boolean = false,
    val isDepthOcclusion: Boolean = true,
    val isBloomEnabled: Boolean = true,
    val isAmbientOcclusion: Boolean = true,
    val isShadowsEnabled: Boolean = true,
    val isPbrMetallic: Boolean = true,
    val isNormalMapping: Boolean = true,
    val isRoughnessMapping: Boolean = true,
    val isEmissionEnabled: Boolean = true,
    val isChromaKeyEnabled: Boolean = false,
    val isPortalView: Boolean = false,
    val isSpatialGrid: Boolean = true,
    val isPerformanceHud: Boolean = true,
    val isHdrToneMapping: Boolean = true,
    val isVignetteEnabled: Boolean = false,
    val isChromaticAberration: Boolean = false,
    val isScanlinesEnabled: Boolean = false,
    val isAnaglyph3D: Boolean = false,
    val isSideBySideVR: Boolean = false,
    val isStereoFov: Boolean = false,
    val passthroughOpacity: Float = 1.0f,
    val meshQuality: MeshQuality = MeshQuality.HIGH,
    val spatialAudioEnabled: Boolean = true,
    val occlusionThreshold: Float = 0.5f,
    val cameraExposure: Float = 0.0f,
    val cameraFlashlight: Boolean = false,
    val arAnchorPlaced: Boolean = false,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val fps: Int = 60,
    val frameTimeMs: Float = 16.6f,
    val drawCalls: Int = 1,
    val trianglesCount: Int = 0,
    val verticesCount: Int = 0,
    val activePassCount: Int = 1,
    val memoryUsageMb: Int = 42,
    val fov: Float = 60f,
    val nearClip: Float = 0.1f,
    val farClip: Float = 100f,
    val isMeasurementToolActive: Boolean = false,
    val measurementDistanceMeters: Float = 0.0f,
    val isMeshInspectMode: Boolean = false,
    val isColorGradingWarm: Boolean = false,
    val isColorGradingCool: Boolean = false,
    val isColorGradingCyberpunk: Boolean = false,
    val isAnimationPlaying: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val modelOpacity: Float = 1.0f,
    val environment: SpatialEnvironment = SpatialEnvironment.STUDIO_VOID,
    val spatialEnvironment: SpatialEnvironment = SpatialEnvironment.STUDIO_VOID,
    val modelColor: Color = Color(0xFF00E5FF),
    val hdriPreset: HdriPreset = HdriPreset.STUDIO_PRO,
    val renderEngineProfile: RenderEngineProfile = RenderEngineProfile.REALITYKIT,
    val isModelPickerOpen: Boolean = false,
    val isLoadingModel: Boolean = false,
    val notificationMessage: String? = null,

    // Window management
    val openWindows: Map<SpatialAppId, WindowState> = emptyMap(),
    val activeAppId: SpatialAppId? = null,

    // ARCore & AR Foundation Plane Detection State
    val capabilities: ARCoreCapabilities = ARCoreCapabilities(),
    val detectedPlanes: List<ARTrackedPlane> = emptyList(),
    val trackedImages: List<ARTrackedImage> = emptyList(),
    val pointCloud: List<Vec3> = emptyList(),
    val surfaceAnchor: ARSurfaceAnchor? = null,
    val isPlaneMeshVisible: Boolean = false,
    val isPointCloudVisible: Boolean = false,
    val planeFilter: ARPlaneFilter = ARPlaneFilter.ALL,
    val placementMode: ARPlacementMode = ARPlacementMode.TAP_TO_PLACE,
    val selectedPlaneId: String? = null,
    val arCoreStatus: String = "AR Surface Scanner Active",
    val lightIntensity: Float = 1.0f,
    val trackingQuality: ARTrackingStateQuality = ARTrackingStateQuality.INITIALIZING,
    val geospatialInfo: ARGeospatialInfo = ARGeospatialInfo(),
    val streetscapeMeshes: List<ARStreetscapeMesh> = emptyList(),
    val semanticDistribution: Map<SceneSemanticType, Float> = emptyMap(),
    val depthFusionInfo: ARDepthFusionInfo = ARDepthFusionInfo(),
    val faceTracking: ARFaceMeshTracking = ARFaceMeshTracking(),
    val isFaceTrackingActive: Boolean = false,
    val persistentAnchors: List<PersistentARAnchorData> = emptyList(),
    val recordedSessions: List<RecordedSessionItem> = emptyList(),
    val cloudAnchorStatus: String? = null,
    val isArSuitePanelOpen: Boolean = false
)

class MixedRealityViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorTracker = SensorTracker(application)
    val arCoreManager = ARCoreManager(application)
    private var recordingJob: Job? = null

    private val _uiState = MutableStateFlow(MRUiState())
    val uiState: StateFlow<MRUiState> = _uiState.asStateFlow()

    init {
        sensorTracker.start()

        val defaultModels = com.example.math3d.MeshGenerator.getDefaultModels()
        _uiState.value = _uiState.value.copy(
            models = defaultModels,
            currentModel = defaultModels.firstOrNull(),
            selectedModelIndex = 0
        )

        // Collect hardware capabilities
        viewModelScope.launch {
            arCoreManager.capabilities.collect { caps ->
                _uiState.value = _uiState.value.copy(capabilities = caps)
            }
        }

        // Collect sensor updates
        viewModelScope.launch {
            sensorTracker.orientation.collect { orientation ->
                _uiState.value = _uiState.value.copy(
                    orientation = orientation,
                    sensorOrientation = orientation
                )
                if (_uiState.value.currentMode != SpatialMode.OBJECT) {
                    arCoreManager.updateFrame(orientation.pitch, orientation.roll, orientation.yaw)
                }
            }
        }

        // Collect ARCore detected planes
        viewModelScope.launch {
            arCoreManager.trackedPlanes.collect { planes ->
                _uiState.value = _uiState.value.copy(detectedPlanes = planes)
            }
        }

        // Collect ARCore tracked images
        viewModelScope.launch {
            arCoreManager.trackedImages.collect { images ->
                _uiState.value = _uiState.value.copy(trackedImages = images)
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

        // Collect ARCore tracking quality
        viewModelScope.launch {
            arCoreManager.trackingQuality.collect { quality ->
                _uiState.value = _uiState.value.copy(trackingQuality = quality)
            }
        }

        // Collect ARCore Geospatial Info
        viewModelScope.launch {
            arCoreManager.geospatialInfo.collect { geo ->
                _uiState.value = _uiState.value.copy(geospatialInfo = geo)
            }
        }

        // Collect ARCore Streetscape Meshes
        viewModelScope.launch {
            arCoreManager.streetscapeMeshes.collect { meshes ->
                _uiState.value = _uiState.value.copy(streetscapeMeshes = meshes)
            }
        }

        // Collect Scene Semantics
        viewModelScope.launch {
            arCoreManager.semanticDistribution.collect { dist ->
                _uiState.value = _uiState.value.copy(semanticDistribution = dist)
            }
        }

        // Collect Depth Fusion
        viewModelScope.launch {
            arCoreManager.depthFusionInfo.collect { depthInfo ->
                _uiState.value = _uiState.value.copy(depthFusionInfo = depthInfo)
            }
        }

        // Collect Face Tracking
        viewModelScope.launch {
            arCoreManager.faceMeshTracking.collect { faceInfo ->
                _uiState.value = _uiState.value.copy(faceTracking = faceInfo)
            }
        }

        // Collect ARCore light estimation
        viewModelScope.launch {
            arCoreManager.lightIntensity.collect { intensity ->
                _uiState.value = _uiState.value.copy(lightIntensity = intensity)
            }
        }

        // Collect Recorded Sessions list
        viewModelScope.launch {
            arCoreManager.recordedSessions.collect { sessions ->
                _uiState.value = _uiState.value.copy(recordedSessions = sessions)
            }
        }

        // Load saved persistent anchors
        loadPersistentAnchors()
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
        } else {
            arCoreManager.pause()
        }
    }

    fun setMRSubMode(subMode: MRSubMode) {
        _uiState.value = _uiState.value.copy(mrSubMode = subMode)
        showNotification(subMode.title)
    }

    fun setEnvironment(env: SpatialEnvironment) {
        _uiState.value = _uiState.value.copy(
            spatialEnvironment = env,
            environment = env,
            hdriPreset = env.hdriPreset
        )
        showNotification("Environment: ${env.displayName}")
    }

    fun setModelColor(color: Color) {
        _uiState.value = _uiState.value.copy(modelColor = color)
    }

    fun setRenderEngineProfile(profile: RenderEngineProfile) {
        _uiState.value = _uiState.value.copy(renderEngineProfile = profile)
        showNotification("Engine: ${profile.title}")
    }

    fun launchApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        val nextZ = (currentWindows.values.maxOfOrNull { it.zIndex } ?: 0) + 1
        currentWindows[appId] = WindowState(appId = appId, isOpen = true, isMinimized = false, zIndex = nextZ)
        _uiState.value = _uiState.value.copy(openWindows = currentWindows, activeAppId = appId)
    }

    fun closeApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        currentWindows.remove(appId)
        val nextActive = currentWindows.keys.lastOrNull()
        _uiState.value = _uiState.value.copy(openWindows = currentWindows, activeAppId = nextActive)
    }

    fun minimizeApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        val state = currentWindows[appId] ?: return
        currentWindows[appId] = state.copy(isMinimized = true)
        _uiState.value = _uiState.value.copy(openWindows = currentWindows)
    }

    fun maximizeApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        val state = currentWindows[appId] ?: return
        currentWindows[appId] = state.copy(isMaximized = !state.isMaximized)
        _uiState.value = _uiState.value.copy(openWindows = currentWindows)
    }

    fun focusApp(appId: SpatialAppId) {
        val currentWindows = _uiState.value.openWindows.toMutableMap()
        val state = currentWindows[appId] ?: return
        val nextZ = (currentWindows.values.maxOfOrNull { it.zIndex } ?: 0) + 1
        currentWindows[appId] = state.copy(isOpen = true, isMinimized = false, zIndex = nextZ)
        _uiState.value = _uiState.value.copy(openWindows = currentWindows, activeAppId = appId)
    }

    fun toggleGyro() {
        val newState = !_uiState.value.gyroEnabled
        _uiState.value = _uiState.value.copy(gyroEnabled = newState, isGyroEnabled = newState)
        if (newState) sensorTracker.start() else sensorTracker.stop()
    }

    fun selectModel(index: Int) {
        if (index in _uiState.value.models.indices) {
            val model = _uiState.value.models[index]
            _uiState.value = _uiState.value.copy(
                selectedModelIndex = index,
                currentModel = model,
                trianglesCount = model.trianglesCount,
                verticesCount = model.verticesCount
            )
            showNotification("Selected: ${model.name}")
        }
    }

    fun setScale(scale: Float) {
        _uiState.value = _uiState.value.copy(scale = scale.coerceIn(0.1f, 5.0f))
    }

    fun updateScale(zoomFactor: Float) {
        _uiState.value = _uiState.value.copy(scale = (_uiState.value.scale * zoomFactor).coerceIn(0.05f, 10.0f))
    }

    fun setRotation(rx: Float, ry: Float, rz: Float) {
        _uiState.value = _uiState.value.copy(rotX = rx, rotY = ry, rotZ = rz)
    }

    fun updateRotation(deltaX: Float = 0f, deltaY: Float = 0f, deltaZ: Float = 0f) {
        _uiState.value = _uiState.value.copy(
            rotX = _uiState.value.rotX + deltaX,
            rotY = _uiState.value.rotY + deltaY,
            rotZ = _uiState.value.rotZ + deltaZ
        )
    }

    fun setPosition(px: Float, py: Float, pz: Float) {
        _uiState.value = _uiState.value.copy(posX = px, posY = py, posZ = pz)
    }

    fun updatePan(dx: Float, dy: Float) {
        _uiState.value = _uiState.value.copy(
            panX = _uiState.value.panX + dx * 0.005f,
            panY = _uiState.value.panY - dy * 0.005f
        )
    }

    fun setIpdDistance(ipd: Float) {
        _uiState.value = _uiState.value.copy(ipdDistance = ipd)
    }

    fun resetView() {
        _uiState.value = _uiState.value.copy(
            scale = 1.0f,
            rotX = 0f,
            rotY = 0f,
            rotZ = 0f,
            panX = 0f,
            panY = 0f,
            posX = 0f,
            posY = 0f,
            posZ = 0f
        )
        showNotification("View Reset")
    }

    fun resetPosition() {
        _uiState.value = _uiState.value.copy(
            panX = 0f,
            panY = 0f,
            posX = 0f,
            posY = 0f,
            posZ = 0f
        )
        showNotification("Position Reset")
    }

    fun toggleWireframe() {
        _uiState.value = _uiState.value.copy(isWireframe = !_uiState.value.isWireframe)
    }

    fun toggleLighting() {
        _uiState.value = _uiState.value.copy(isLightingEnabled = !_uiState.value.isLightingEnabled)
    }

    fun toggleAutoRotation() {
        val next = !_uiState.value.isAutoRotating
        _uiState.value = _uiState.value.copy(isAutoRotating = next, isAutoSpin = next)
    }

    fun toggleAutoSpin() {
        toggleAutoRotation()
    }

    fun togglePlaneMesh() {
        _uiState.value = _uiState.value.copy(isPlaneMeshVisible = !_uiState.value.isPlaneMeshVisible)
        showNotification(if (_uiState.value.isPlaneMeshVisible) "Plane Mesh: Visible" else "Plane Mesh: Hidden")
    }

    fun togglePointCloud() {
        _uiState.value = _uiState.value.copy(isPointCloudVisible = !_uiState.value.isPointCloudVisible)
        showNotification(if (_uiState.value.isPointCloudVisible) "Point Cloud: Visible" else "Point Cloud: Hidden")
    }

    fun setPlaneFilter(filter: ARPlaneFilter) {
        _uiState.value = _uiState.value.copy(planeFilter = filter)
        showNotification("Plane Filter: ${filter.label}")
    }

    fun setPlacementMode(mode: ARPlacementMode) {
        _uiState.value = _uiState.value.copy(placementMode = mode)
        showNotification("Placement Mode: ${mode.label}")
    }

    fun toggleFaceTrackingMode() {
        val next = !_uiState.value.isFaceTrackingActive
        _uiState.value = _uiState.value.copy(isFaceTrackingActive = next)
        arCoreManager.setFaceTrackingMode(next)
        showNotification(if (next) "Face 3D Mesh Mode Active 👤" else "Environment AR Surface Mode Active")
    }

    fun onSurfaceTapped(screenNormX: Float, screenNormY: Float, viewWidth: Int = 1080, viewHeight: Int = 1920) {
        placeObjectAtScreenCoordinate(screenNormX, screenNormY, viewWidth, viewHeight)
    }

    fun placeObjectAtScreenCoordinate(screenNormX: Float, screenNormY: Float, viewWidth: Int, viewHeight: Int) {
        val hitResult = arCoreManager.hitTest(screenNormX, screenNormY, viewWidth, viewHeight)
        if (hitResult != null) {
            val anchor = ARSurfaceAnchor(
                id = "anchor_${System.currentTimeMillis()}",
                planeId = hitResult.plane.id,
                position = hitResult.hitPosition,
                normal = hitResult.plane.normal,
                rotationY = _uiState.value.rotY,
                scale = _uiState.value.scale,
                surfaceType = hitResult.plane.orientation,
                arcoreAnchor = hitResult.arcoreAnchor,
                hitType = hitResult.hitType
            )
            _uiState.value = _uiState.value.copy(
                surfaceAnchor = anchor,
                selectedPlaneId = hitResult.plane.id,
                arAnchorPlaced = true
            )
            showNotification("Anchored to ${hitResult.plane.orientation.label} (${hitResult.hitType.label})")
        } else {
            showNotification("No surface detected at tap location")
        }
    }

    fun anchorObjectToPlane(planeId: String? = null) {
        val hitResult = arCoreManager.createAnchorOnDetectedPlane(planeId)
        if (hitResult != null) {
            val anchor = ARSurfaceAnchor(
                id = "anchor_${System.currentTimeMillis()}",
                planeId = hitResult.plane.id,
                position = hitResult.hitPosition,
                normal = hitResult.plane.normal,
                rotationY = _uiState.value.rotY,
                scale = _uiState.value.scale,
                surfaceType = hitResult.plane.orientation,
                arcoreAnchor = hitResult.arcoreAnchor,
                hitType = hitResult.hitType
            )
            _uiState.value = _uiState.value.copy(
                surfaceAnchor = anchor,
                selectedPlaneId = hitResult.plane.id,
                arAnchorPlaced = true
            )
            showNotification("Anchored to ${hitResult.plane.orientation.label} (${hitResult.hitType.label})")
        } else {
            showNotification("No physical planes detected yet")
        }
    }

    fun clearARAnchor() {
        val anchor = _uiState.value.surfaceAnchor?.arcoreAnchor
        arCoreManager.detachAnchor(anchor)
        _uiState.value = _uiState.value.copy(surfaceAnchor = null, arAnchorPlaced = false)
        showNotification("AR Anchor Cleared")
    }

    fun clearAll() {
        arCoreManager.clearAllAnchors()
        _uiState.value = _uiState.value.copy(surfaceAnchor = null, arAnchorPlaced = false)
        resetView()
        showNotification("Scene Cleared")
    }

    fun triggerPhotoCapture() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showPhotoFlash = true)
            delay(120)
            _uiState.value = _uiState.value.copy(showPhotoFlash = false)
            showNotification("Spatial Snapshot Captured 📸")
        }
    }

    fun bindModelToImageTarget(target: ARTrackedImage) {
        val anchor = ARSurfaceAnchor(
            id = "img_anchor_${target.id}",
            position = target.center,
            normal = Vec3(0f, 1f, 0f),
            rotationY = 0f,
            scale = target.extentX.coerceIn(0.5f, 2.0f),
            surfaceType = PlaneOrientation.HORIZONTAL_UPWARD,
            arcoreAnchor = target.anchor,
            hitType = ARHitType.AUGMENTED_IMAGE
        )
        _uiState.value = _uiState.value.copy(
            surfaceAnchor = anchor,
            arAnchorPlaced = true
        )
        showNotification("3D Model Attached to Image Target: ${target.name} 🎯")
    }

    fun resetTransform() {
        resetView()
    }

    fun showNotification(msg: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notificationMessage = msg)
            delay(3000)
            if (_uiState.value.notificationMessage == msg) {
                _uiState.value = _uiState.value.copy(notificationMessage = null)
            }
        }
    }

    fun toggleArSuitePanel() {
        _uiState.value = _uiState.value.copy(isArSuitePanelOpen = !_uiState.value.isArSuitePanelOpen)
    }

    // =========================================================================
    // 2. ASYNC CLOUD ANCHORS (NO MOCK IDENTIFIERS)
    // =========================================================================

    fun hostCurrentAnchorToCloud() {
        val anchor = _uiState.value.surfaceAnchor?.arcoreAnchor
        if (anchor == null) {
            showNotification("Place a 6DoF AR Anchor first to host to Google Cloud")
            return
        }
        showNotification("Hosting Cloud Anchor ☁️...")
        arCoreManager.hostCloudAnchor(anchor) { result ->
            result.onSuccess { cloudId ->
                showNotification("Cloud Anchor ID: $cloudId (Shareable)")
                saveCurrentAnchor(cloudId = cloudId)
            }.onFailure { error ->
                showNotification("Cloud Hosting Failed: ${error.localizedMessage}")
            }
        }
    }

    fun resolveCloudAnchorById(cloudId: String) {
        if (cloudId.isBlank()) {
            showNotification("Enter a valid Cloud Anchor ID")
            return
        }
        showNotification("Resolving Cloud Anchor ☁️: $cloudId")
        arCoreManager.resolveCloudAnchor(cloudId) { result ->
            result.onSuccess { resolvedAnchor ->
                val pose = resolvedAnchor.pose
                val anchor = ARSurfaceAnchor(
                    id = "cloud_$cloudId",
                    position = Vec3(pose.tx(), pose.ty(), pose.tz()),
                    normal = Vec3(0f, 1f, 0f),
                    arcoreAnchor = resolvedAnchor,
                    hitType = ARHitType.CLOUD_ANCHOR
                )
                _uiState.value = _uiState.value.copy(
                    surfaceAnchor = anchor,
                    arAnchorPlaced = true
                )
                showNotification("Cloud Anchor Linked & Grounded!")
            }.onFailure { error ->
                showNotification("Cloud Resolve Failed: ${error.localizedMessage}")
            }
        }
    }

    // =========================================================================
    // 5. GEOSPATIAL VALIDATION & TERRAIN / ROOFTOP ANCHORS
    // =========================================================================

    fun placeGeospatialAnchor(lat: Double, lng: Double, alt: Double = 0.0) {
        val result = arCoreManager.createGeospatialAnchor(lat, lng, alt, 0.0)
        result.onSuccess { geoAnchor ->
            val pos = Vec3(geoAnchor.pose.tx(), geoAnchor.pose.ty(), geoAnchor.pose.tz())
            val anchor = ARSurfaceAnchor(
                id = "geo_${System.currentTimeMillis()}",
                position = pos,
                normal = Vec3(0f, 1f, 0f),
                arcoreAnchor = geoAnchor,
                hitType = ARHitType.GEOSPATIAL_ANCHOR
            )
            _uiState.value = _uiState.value.copy(
                surfaceAnchor = anchor,
                arAnchorPlaced = true
            )
            saveCurrentAnchor(lat = lat, lng = lng, alt = alt)
            showNotification("Geospatial GPS Anchor Locked (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})")
        }.onFailure { error ->
            showNotification("Geospatial Blocked: ${error.localizedMessage}")
        }
    }

    fun placeTerrainOrRooftopAnchor(isRooftop: Boolean = false) {
        val geo = _uiState.value.geospatialInfo
        val lat = if (geo.latitude != 0.0) geo.latitude else 37.7749
        val lng = if (geo.longitude != 0.0) geo.longitude else -122.4194

        if (isRooftop) {
            arCoreManager.createRooftopAnchor(lat, lng, 0.0) { result ->
                result.onSuccess { anchor ->
                    val pos = Vec3(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz())
                    val surfaceAnchor = ARSurfaceAnchor(
                        id = "rooftop_${System.currentTimeMillis()}",
                        position = pos,
                        normal = Vec3(0f, 1f, 0f),
                        arcoreAnchor = anchor,
                        hitType = ARHitType.TERRAIN_ROOFTOP
                    )
                    _uiState.value = _uiState.value.copy(
                        surfaceAnchor = surfaceAnchor,
                        arAnchorPlaced = true
                    )
                    showNotification("Rooftop 3D Anchor Bound 🏙️")
                }.onFailure { error ->
                    showNotification("Rooftop Anchor Failed: ${error.localizedMessage}")
                }
            }
        } else {
            arCoreManager.createTerrainAnchor(lat, lng, 0.0) { result ->
                result.onSuccess { anchor ->
                    val pos = Vec3(anchor.pose.tx(), anchor.pose.ty(), anchor.pose.tz())
                    val surfaceAnchor = ARSurfaceAnchor(
                        id = "terrain_${System.currentTimeMillis()}",
                        position = pos,
                        normal = Vec3(0f, 1f, 0f),
                        arcoreAnchor = anchor,
                        hitType = ARHitType.TERRAIN_ROOFTOP
                    )
                    _uiState.value = _uiState.value.copy(
                        surfaceAnchor = surfaceAnchor,
                        arAnchorPlaced = true
                    )
                    showNotification("Terrain 3D Anchor Bound 🏔️")
                }.onFailure { error ->
                    showNotification("Terrain Anchor Failed: ${error.localizedMessage}")
                }
            }
        }
    }

    // =========================================================================
    // PERSISTENT AR ANCHORS (Local Storage)
    // =========================================================================

    fun saveCurrentAnchor(cloudId: String? = null, lat: Double? = null, lng: Double? = null, alt: Double? = null) {
        val anchor = _uiState.value.surfaceAnchor ?: return
        val currentModel = _uiState.value.currentModel ?: return
        val geoLat = _uiState.value.geospatialInfo.latitude
        val geoLng = _uiState.value.geospatialInfo.longitude
        val geoAlt = _uiState.value.geospatialInfo.altitudeMeters

        val persistentData = PersistentARAnchorData(
            id = anchor.id,
            modelName = currentModel.name,
            posX = anchor.position.x,
            posY = anchor.position.y,
            posZ = anchor.position.z,
            rotY = anchor.rotationY,
            scale = anchor.scale,
            cloudAnchorId = cloudId,
            latitude = lat ?: if (geoLat != 0.0) geoLat else null,
            longitude = lng ?: if (geoLng != 0.0) geoLng else null,
            altitude = alt ?: if (geoAlt != 0.0) geoAlt else null,
            hitType = anchor.hitType
        )
        arCoreManager.persistentStorage.saveAnchor(persistentData)
        loadPersistentAnchors()
        showNotification("Anchor Saved Persistently 💾")
    }

    fun loadPersistentAnchors() {
        val list = arCoreManager.persistentStorage.getAllAnchors()
        _uiState.value = _uiState.value.copy(persistentAnchors = list)
    }

    fun restorePersistentAnchor(data: PersistentARAnchorData) {
        val pos = Vec3(data.posX, data.posY, data.posZ)
        val anchor = ARSurfaceAnchor(
            id = data.id,
            position = pos,
            normal = Vec3(0f, 1f, 0f),
            rotationY = data.rotY,
            scale = data.scale,
            hitType = data.hitType
        )
        val matchedModel = _uiState.value.models.firstOrNull { it.name == data.modelName }
        val modelIdx = if (matchedModel != null) _uiState.value.models.indexOf(matchedModel) else _uiState.value.selectedModelIndex
        _uiState.value = _uiState.value.copy(
            surfaceAnchor = anchor,
            selectedModelIndex = modelIdx,
            currentModel = matchedModel ?: _uiState.value.currentModel,
            scale = data.scale,
            rotY = data.rotY,
            arAnchorPlaced = true
        )
        showNotification("Restored Anchor: ${data.modelName}")
    }

    fun deletePersistentAnchor(id: String) {
        arCoreManager.persistentStorage.removeAnchor(id)
        loadPersistentAnchors()
        showNotification("Anchor deleted from storage")
    }

    // =========================================================================
    // 9. AR SESSION RECORDING & PLAYBACK
    // =========================================================================

    fun toggleRecording(context: Context? = null) {
        val ctx = context ?: getApplication<Application>()
        toggleArSessionRecording(ctx)
    }

    fun toggleArSessionRecording(context: Context) {
        if (_uiState.value.isRecording) {
            arCoreManager.stopRecording()
            recordingJob?.cancel()
            _uiState.value = _uiState.value.copy(isRecording = false, recordingSeconds = 0)
            showNotification("AR Session Recording Saved 🎥 (.mp4)")
        } else {
            val file = File(context.cacheDir, "ar_session_${System.currentTimeMillis()}.mp4")
            arCoreManager.startRecording(file)
            _uiState.value = _uiState.value.copy(isRecording = true, recordingSeconds = 0)
            recordingJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _uiState.value = _uiState.value.copy(recordingSeconds = _uiState.value.recordingSeconds + 1)
                }
            }
            showNotification("Recording AR Session Dataset...")
        }
    }

    fun playRecordedSession(sessionItem: RecordedSessionItem) {
        val file = File(sessionItem.filePath)
        if (file.exists()) {
            val success = arCoreManager.setPlaybackDataset(file)
            if (success) {
                showNotification("Replaying AR Session Dataset: ${sessionItem.fileName} 🔄")
            } else {
                showNotification("Failed to load playback dataset")
            }
        }
    }

    fun toggleSpatialAudio() {
        _uiState.value = _uiState.value.copy(spatialAudioEnabled = !_uiState.value.spatialAudioEnabled)
    }

    fun loadModelFromUri(context: Context, uri: Uri) {
        loadCustomModelFromFile(uri, context)
    }

    fun loadModelFromUri(uri: Uri, context: Context) {
        loadCustomModelFromFile(uri, context)
    }

    fun loadCustomModelFromFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingModel = true)
            try {
                val loadedModel = ModelFileLoader.loadModelFromUri(context, uri)
                if (loadedModel != null) {
                    val updatedList = _uiState.value.models + loadedModel
                    _uiState.value = _uiState.value.copy(
                        models = updatedList,
                        currentModel = loadedModel,
                        selectedModelIndex = updatedList.lastIndex,
                        isLoadingModel = false,
                        isModelPickerOpen = false
                    )
                    showNotification("Imported ${loadedModel.name}")
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingModel = false)
                    showNotification("Failed to parse 3D file")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingModel = false)
                showNotification("Error: ${e.localizedMessage}")
            }
        }
    }
}

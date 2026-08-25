package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.SensorOrientation
import com.example.engine.SensorTracker
import com.example.math3d.Model3D
import com.example.math3d.ModelFileLoader
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
    val currentTab: AppTab = AppTab.STUDIO_3D,
    val activeAppId: SpatialAppId = SpatialAppId.STUDIO_3D,
    val openWindows: Map<SpatialAppId, WindowState> = mapOf(
        SpatialAppId.STUDIO_3D to WindowState(SpatialAppId.STUDIO_3D, isOpen = true)
    ),
    val isLauncherOpen: Boolean = false,
    val environment: SpatialEnvironment = SpatialEnvironment.HORIZON,
    val spatialAudioEnabled: Boolean = true,
    val currentModel: Model3D? = null,
    val selectedModelIndex: Int = 0,
    val models: List<Model3D> = emptyList(),
    val rotX: Float = 0.2f,
    val rotY: Float = 0.4f,
    val scale: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val isAutoSpin: Boolean = false,
    val isWireframe: Boolean = false,
    val modelColor: Color = Color(0xFF00E5FF),
    val sensorOrientation: SensorOrientation = SensorOrientation(),
    val arSurfaceDetected: Boolean = true,
    val arAnchorPlaced: Boolean = true,
    val ipdDistance: Float = 0.12f,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val showPhotoFlash: Boolean = false,
    val isModelPickerOpen: Boolean = false,
    val isLoadingModel: Boolean = false,
    val notificationMessage: String? = null
)

class MixedRealityViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorTracker = SensorTracker(application)
    private var recordingJob: Job? = null

    private val _uiState = MutableStateFlow(MRUiState())
    val uiState: StateFlow<MRUiState> = _uiState.asStateFlow()

    init {
        sensorTracker.start()
        viewModelScope.launch {
            sensorTracker.orientation.collect { orientation ->
                _uiState.value = _uiState.value.copy(sensorOrientation = orientation)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorTracker.stop()
    }

    fun setMode(mode: SpatialMode) {
        _uiState.value = _uiState.value.copy(currentMode = mode)
    }

    fun loadModelFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingModel = true)
            val model = withContext(Dispatchers.IO) {
                ModelFileLoader.loadModelFromUri(context, uri)
            }
            if (model != null && model.triangles.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    currentModel = model,
                    models = listOf(model),
                    selectedModelIndex = 0,
                    isLoadingModel = false,
                    rotX = 0.2f,
                    rotY = 0.4f,
                    scale = 1.0f,
                    panX = 0f,
                    panY = 0f
                )
                showNotification("Loaded: ${model.name} (${model.triangles.size} polygons)")
            } else {
                _uiState.value = _uiState.value.copy(isLoadingModel = false)
                showNotification("Could not parse 3D model. Please select an .obj or .stl file.")
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

    fun clearAll() {
        _uiState.value = _uiState.value.copy(
            currentModel = null,
            models = emptyList(),
            rotX = 0.2f,
            rotY = 0.4f,
            scale = 1.0f,
            panX = 0f,
            panY = 0f,
            arAnchorPlaced = false
        )
        showNotification("Cleared Model & Canvas")
    }

    fun resetView() {
        _uiState.value = _uiState.value.copy(
            rotX = 0.2f,
            rotY = 0.4f,
            scale = 1.0f,
            panX = 0f,
            panY = 0f
        )
        showNotification("View Reset")
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
        _uiState.value = _uiState.value.copy(arAnchorPlaced = !_uiState.value.arAnchorPlaced)
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

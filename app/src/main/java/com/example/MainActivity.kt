package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.Renderer3D
import com.example.ui.components.CameraPreview
import com.example.ui.components.StereoDualCameraPreview
import com.example.ui.theme.MixedRealityTheme
import com.example.viewmodel.MRUiState
import com.example.viewmodel.MixedRealityViewModel
import com.example.viewmodel.SpatialMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MixedRealityTheme {
                val viewModel: MixedRealityViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                SpatialMainScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun SpatialMainScreen(
    uiState: MRUiState,
    viewModel: MixedRealityViewModel
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // System File Picker for 3D model files (.glb, .gltf, .usdz, .obj, .stl, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadModelFromUri(context, uri)
        }
    }

    val renderer = remember { Renderer3D() }
    val currentModel = uiState.currentModel

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        when (uiState.currentMode) {
            SpatialMode.OBJECT -> {
                // Object Mode: Studio Canvas with smooth gesture rotation & scaling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFF6F8FA),
                                    Color(0xFFECEFF1)
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (pan.x != 0f || pan.y != 0f) {
                                    viewModel.updateRotation(
                                        deltaX = -pan.y * 0.008f,
                                        deltaY = pan.x * 0.008f
                                    )
                                }
                                if (zoom != 1.0f) {
                                    viewModel.updateScale(zoom)
                                }
                            }
                        }
                ) {
                    if (currentModel != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            renderer.render(
                                drawScope = this,
                                model = currentModel,
                                rotX = uiState.rotX,
                                rotY = uiState.rotY,
                                rotZ = 0f,
                                scale = uiState.scale,
                                panX = 0f,
                                panY = 0f,
                                wireframe = uiState.isWireframe,
                                primaryColor = uiState.modelColor,
                                drawShadow = false,
                                drawFloorGrid = false,
                                hdriPreset = uiState.hdriPreset
                            )
                        }
                    } else {
                        // Empty State Prompt
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .padding(32.dp)
                                    .clickable { filePickerLauncher.launch("*/*") }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE8F0FE),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.FileOpen,
                                            contentDescription = null,
                                            tint = Color(0xFF1A73E8),
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Tap to open a 3D Model",
                                    color = Color(0xFF202124),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Supports GLB, GLTF, Apple USDZ, OBJ & STL formats",
                                    color = Color(0xFF5F6368),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            SpatialMode.AR -> {
                // AR Mode: Real Camera Passthrough + Gyroscope Space Tracking
                val gyroPitch = if (uiState.isGyroEnabled) -uiState.sensorOrientation.pitch else 0f
                val gyroRoll = if (uiState.isGyroEnabled) uiState.sensorOrientation.roll else 0f
                val gyroYaw = if (uiState.isGyroEnabled) uiState.sensorOrientation.yaw else 0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (hasCameraPermission) {
                        // Direct Camera Stream
                        CameraPreview(modifier = Modifier.fillMaxSize())

                        // 3D Object Overlay with Gyroscope integration
                        if (currentModel != null) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            if (pan.x != 0f || pan.y != 0f) {
                                                viewModel.updatePan(pan.x, pan.y)
                                            }
                                            if (zoom != 1.0f) {
                                                viewModel.updateScale(zoom)
                                            }
                                            if (rotation != 0f) {
                                                viewModel.updateRotation(deltaX = 0f, deltaY = rotation * 0.02f)
                                            }
                                        }
                                    }
                            ) {
                                renderer.render(
                                    drawScope = this,
                                    model = currentModel,
                                    rotX = uiState.rotX + gyroPitch,
                                    rotY = uiState.rotY + gyroRoll,
                                    rotZ = gyroYaw * 0.5f,
                                    scale = uiState.scale,
                                    panX = uiState.panX + (gyroRoll * 200f),
                                    panY = uiState.panY - (gyroPitch * 200f),
                                    wireframe = false,
                                    primaryColor = uiState.modelColor,
                                    drawShadow = false,
                                    drawFloorGrid = false,
                                    hdriPreset = uiState.hdriPreset
                                )
                            }
                        }
                    } else {
                        // Camera Permission Request
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(54.dp)
                                )
                                Text(
                                    text = "Camera Access Required for AR",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Grant Access", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            SpatialMode.MR -> {
                // =============================================================
                // MR (MIXED REALITY) STEREO DUAL CAMERA
                // Dual-eye passthrough with synchronized left and right feeds
                // =============================================================
                val gyroPitch = if (uiState.isGyroEnabled) -uiState.sensorOrientation.pitch else 0f
                val gyroRoll = if (uiState.isGyroEnabled) uiState.sensorOrientation.roll else 0f
                val gyroYaw = if (uiState.isGyroEnabled) uiState.sensorOrientation.yaw else 0f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (hasCameraPermission) {
                        StereoDualCameraPreview(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        if (pan.x != 0f || pan.y != 0f) {
                                            viewModel.updatePan(pan.x, pan.y)
                                        }
                                        if (zoom != 1.0f) {
                                            viewModel.updateScale(zoom)
                                        }
                                        if (rotation != 0f) {
                                            viewModel.updateRotation(0f, rotation * 0.02f)
                                        }
                                    }
                                },
                            leftOverlay = {
                                if (currentModel != null) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        renderer.render(
                                            drawScope = this,
                                            model = currentModel,
                                            rotX = uiState.rotX + gyroPitch,
                                            rotY = uiState.rotY + gyroRoll - (uiState.ipdDistance * 0.35f),
                                            rotZ = gyroYaw * 0.4f,
                                            scale = uiState.scale * 0.85f,
                                            panX = uiState.panX - 25f,
                                            panY = uiState.panY - (gyroPitch * 150f),
                                            wireframe = false,
                                            primaryColor = uiState.modelColor,
                                            drawShadow = false,
                                            drawFloorGrid = false,
                                            hdriPreset = uiState.hdriPreset
                                        )
                                    }
                                }
                            },
                            rightOverlay = {
                                if (currentModel != null) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        renderer.render(
                                            drawScope = this,
                                            model = currentModel,
                                            rotX = uiState.rotX + gyroPitch,
                                            rotY = uiState.rotY + gyroRoll + (uiState.ipdDistance * 0.35f),
                                            rotZ = gyroYaw * 0.4f,
                                            scale = uiState.scale * 0.85f,
                                            panX = uiState.panX + 25f,
                                            panY = uiState.panY - (gyroPitch * 150f),
                                            wireframe = false,
                                            primaryColor = uiState.modelColor,
                                            drawShadow = false,
                                            drawFloorGrid = false,
                                            hdriPreset = uiState.hdriPreset
                                        )
                                    }
                                }
                            }
                        )
                    } else {
                        // Camera Permission Request
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Enable Camera for MR", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // TOP PILL BAR: [ MR | AR | Object ]
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 18.dp)
        ) {
            AppleLiquidSegmentedPill(
                currentMode = uiState.currentMode,
                onModeSelected = { viewModel.setMode(it) }
            )
        }

        // Recording Indicator
        if (uiState.isRecording) {
            val minutes = uiState.recordingSeconds / 60
            val seconds = uiState.recordingSeconds % 60
            val timeText = String.format("%02d:%02d", minutes, seconds)

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 22.dp, end = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x80000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // -------------------------------------------------------------
        // BOTTOM PILL BAR: [ PHOTO | (● REC) | Open | Clear ]
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            AppleLiquidBottomControls(
                isRecording = uiState.isRecording,
                onPhotoClick = { viewModel.triggerPhotoCapture() },
                onRecClick = { viewModel.toggleRecording() },
                onOpenClick = {
                    filePickerLauncher.launch("*/*")
                },
                onClearClick = { viewModel.clearAll() }
            )
        }

        // Loading Indicator
        if (uiState.isLoadingModel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Notification Toast Pill
        AnimatedVisibility(
            visible = uiState.notificationMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 74.dp)
        ) {
            uiState.notificationMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC1F2937))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Photo Flash Effect
        if (uiState.showPhotoFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }
    }
}

// ---------------------------------------------------------------------
// TOP SEGMENTED PILL (MR | AR | Object)
// ---------------------------------------------------------------------
@Composable
fun AppleLiquidSegmentedPill(
    currentMode: SpatialMode,
    onModeSelected: (SpatialMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(SpatialMode.MR, SpatialMode.AR, SpatialMode.OBJECT)

    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.White.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFFC7CBD1).copy(alpha = 0.85f))
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFFFFF), Color(0x80B0B5BC))
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            modes.forEach { mode ->
                val isSelected = currentMode == mode
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .defaultMinSize(minWidth = 64.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (isSelected) Color(0xFF8E959E).copy(alpha = 0.95f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onModeSelected(mode)
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) Color(0xFF1E242B) else Color(0xFF5F6670),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// BOTTOM CONTROLS PILL (PHOTO | REC | Open | Clear)
// ---------------------------------------------------------------------
@Composable
fun AppleLiquidBottomControls(
    isRecording: Boolean,
    onPhotoClick: () -> Unit,
    onRecClick: () -> Unit,
    onOpenClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(38.dp), spotColor = Color.White.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(38.dp))
            .background(Color(0xFFC7CBD1).copy(alpha = 0.88f))
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFFFFF), Color(0x80B0B5BC))
                ),
                shape = RoundedCornerShape(38.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // PHOTO
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPhotoClick
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PHOTO",
                    color = Color(0xFF2B313A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // REC (Red circular button)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRecClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isRecording) "STOP" else "REC",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Open (Opens Phone File Picker for 3D Models)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenClick
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open",
                    color = Color(0xFF2B313A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Clear
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClearClick
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Clear",
                    color = Color(0xFF2B313A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

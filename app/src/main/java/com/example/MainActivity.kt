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

    // System File Picker for 3D model files (.obj, .stl, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadModelFromUri(context, uri)
        }
    }

    val renderer = remember { Renderer3D() }
    val currentModel = uiState.currentModel

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // -------------------------------------------------------------
        // Main Viewport depending on mode (MR, AR, Object)
        // -------------------------------------------------------------
        when (uiState.currentMode) {
            SpatialMode.OBJECT -> {
                // Pure black canvas for 3D Object inspection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
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
                    if (currentModel != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            renderer.render(
                                drawScope = this,
                                model = currentModel,
                                rotX = uiState.rotX,
                                rotY = uiState.rotY,
                                rotZ = 0f,
                                scale = uiState.scale,
                                panX = uiState.panX,
                                panY = uiState.panY,
                                wireframe = uiState.isWireframe,
                                primaryColor = uiState.modelColor,
                                drawShadow = true,
                                drawFloorGrid = false
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
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .padding(32.dp)
                                    .clickable { filePickerLauncher.launch("*/*") }
                            ) {
                                Icon(
                                    Icons.Default.FileOpen,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = "Tap 'Open' below to select a 3D model",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Supports USDZ, GLB, GLTF, OBJ & STL files",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            SpatialMode.AR -> {
                // AR Mode: Real Camera Passthrough + Ground Anchored 3D Model
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (hasCameraPermission) {
                        // Direct Camera Stream
                        CameraPreview(modifier = Modifier.fillMaxSize())

                        // 3D Object Overlay
                        if (currentModel != null) {
                            val gyroPitch = uiState.sensorOrientation.pitch * 0.015f
                            val gyroRoll = uiState.sensorOrientation.roll * 0.015f
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
                                    rotZ = 0f,
                                    scale = uiState.scale,
                                    panX = uiState.panX,
                                    panY = uiState.panY,
                                    wireframe = uiState.isWireframe,
                                    primaryColor = uiState.modelColor,
                                    drawShadow = true,
                                    drawFloorGrid = true
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
                // Stereoscopic Dual View with Passthrough
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (hasCameraPermission) {
                        CameraPreview(modifier = Modifier.fillMaxSize())
                    }

                    Row(
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
                            }
                    ) {
                        val headPitch = uiState.sensorOrientation.pitch * 0.02f
                        val headRoll = uiState.sensorOrientation.roll * 0.02f

                        // Left Eye
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (currentModel != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    renderer.render(
                                        drawScope = this,
                                        model = currentModel,
                                        rotX = uiState.rotX + headPitch,
                                        rotY = uiState.rotY + headRoll - (uiState.ipdDistance * 0.4f),
                                        rotZ = 0f,
                                        scale = uiState.scale * 0.85f,
                                        panX = uiState.panX - 20f,
                                        panY = uiState.panY,
                                        wireframe = uiState.isWireframe,
                                        primaryColor = uiState.modelColor,
                                        drawShadow = true,
                                        drawFloorGrid = true
                                    )
                                }
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color(0x66FFFFFF))
                        )

                        // Right Eye
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (currentModel != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    renderer.render(
                                        drawScope = this,
                                        model = currentModel,
                                        rotX = uiState.rotX + headPitch,
                                        rotY = uiState.rotY + headRoll + (uiState.ipdDistance * 0.4f),
                                        rotZ = 0f,
                                        scale = uiState.scale * 0.85f,
                                        panX = uiState.panX + 20f,
                                        panY = uiState.panY,
                                        wireframe = uiState.isWireframe,
                                        primaryColor = uiState.modelColor,
                                        drawShadow = true,
                                        drawFloorGrid = true
                                    )
                                }
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

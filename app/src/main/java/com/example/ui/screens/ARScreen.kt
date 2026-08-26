package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.engine.Renderer3D
import com.example.engine.ar.ARPlaneFilter
import com.example.engine.ar.ARPlaneRenderer
import com.example.ui.components.CameraPreview
import com.example.viewmodel.MRUiState
import com.example.viewmodel.MixedRealityViewModel

@Composable
fun ARScreen(
    uiState: MRUiState,
    viewModel: MixedRealityViewModel,
    modifier: Modifier = Modifier
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

    val renderer = remember { Renderer3D() }
    val arPlaneRenderer = remember { ARPlaneRenderer() }
    val textMeasurer = rememberTextMeasurer()
    val currentModel = uiState.models.getOrNull(uiState.selectedModelIndex)

    val gyroPitch = uiState.sensorOrientation.pitch * 0.015f
    val gyroRoll = uiState.sensorOrientation.roll * 0.015f

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(modifier = Modifier.fillMaxSize())

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val normX = offset.x / size.width.toFloat()
                                val normY = offset.y / size.height.toFloat()
                                viewModel.onSurfaceTapped(normX, normY)
                            },
                            onDoubleTap = {
                                viewModel.resetPosition()
                            }
                        )
                    }
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
                // 1. Render ARCore Detected Planes and Point Cloud
                arPlaneRenderer.renderPlanes(
                    drawScope = this,
                    planes = uiState.detectedPlanes,
                    pointCloud = uiState.pointCloud,
                    anchor = uiState.surfaceAnchor,
                    isPlaneMeshVisible = uiState.isPlaneMeshVisible,
                    isPointCloudVisible = uiState.isPointCloudVisible,
                    selectedPlaneId = uiState.selectedPlaneId,
                    filter = uiState.planeFilter,
                    textMeasurer = textMeasurer
                )

                // 2. Render 3D Model
                if (currentModel != null) {
                    val anchor = uiState.surfaceAnchor
                    val basePanY = if (anchor != null) 100f else 0f

                    renderer.render(
                        drawScope = this,
                        model = currentModel,
                        rotX = uiState.rotX + gyroPitch,
                        rotY = uiState.rotY + gyroRoll + (anchor?.rotationY ?: 0f),
                        rotZ = 0f,
                        scale = uiState.scale * (anchor?.scale ?: 1.0f),
                        panX = uiState.panX,
                        panY = uiState.panY + basePanY,
                        wireframe = uiState.isWireframe,
                        primaryColor = uiState.modelColor,
                        drawShadow = true,
                        drawFloorGrid = false,
                        hdriPreset = uiState.hdriPreset,
                        engineProfile = uiState.renderEngineProfile
                    )
                }
            }

            // Top Status Chip
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, start = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xCC0F172A))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.detectedPlanes.isNotEmpty()) Color(0xFF00FF88) else Color(0xFFFFB703))
                    )
                    Text(
                        text = uiState.arCoreStatus,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Surface Controls (Right Edge)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { viewModel.togglePlaneMesh() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isPlaneMeshVisible) Color(0xCC00E5FF) else Color(0x991E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Grid4x4,
                        contentDescription = "Toggle Plane Meshes",
                        tint = if (uiState.isPlaneMeshVisible) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.placeModelOnDetectedSurface() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (uiState.surfaceAnchor != null) Color(0xCC00FF88) else Color(0x991E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalAlignBottom,
                        contentDescription = "Snap to Surface",
                        tint = if (uiState.surfaceAnchor != null) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
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


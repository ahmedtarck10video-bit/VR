package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.CameraPreview
import com.example.ui.components.Sceneview3DViewport
import com.example.ui.components.StereoDualCameraPreview
import com.example.ui.components.GlassCard
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.MRUiState
import com.example.viewmodel.MixedRealityViewModel

@Composable
fun StereoVRScreen(
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

    val currentModel = uiState.models.getOrNull(uiState.selectedModelIndex) ?: return

    val headPitch = uiState.sensorOrientation.pitch * 0.02f
    val headRoll = uiState.sensorOrientation.roll * 0.02f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Live Stereo Dual Camera Passthrough for Mixed Reality (MR / VR)
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
                    Sceneview3DViewport(
                        model = currentModel,
                        rotX = uiState.rotX + headPitch,
                        rotY = uiState.rotY + headRoll - (uiState.ipdDistance * 0.4f),
                        rotZ = 0f,
                        scale = uiState.scale * 0.85f,
                        panX = uiState.panX - 20f,
                        panY = uiState.panY,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "L Eye [MR Stereo]",
                        color = NeonCyan.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                },
                rightOverlay = {
                    Sceneview3DViewport(
                        model = currentModel,
                        rotX = uiState.rotX + headPitch,
                        rotY = uiState.rotY + headRoll + (uiState.ipdDistance * 0.4f),
                        rotZ = 0f,
                        scale = uiState.scale * 0.85f,
                        panX = uiState.panX + 20f,
                        panY = uiState.panY,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "R Eye [MR Stereo]",
                        color = NeonCyan.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            )
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
                        text = "Camera Access Required for MR Passthrough",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Enable MR Camera", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Liquid Glass Control Bar at bottom
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = Color(0x600F172A),
            borderColor = Color(0x4DFFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MR Stereo IPD: ${(uiState.ipdDistance * 100).toInt()} mm",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                    )
                    Row {
                        IconButton(onClick = { viewModel.toggleWireframe() }) {
                            Icon(Icons.Default.GridOn, contentDescription = "Wireframe", tint = if (uiState.isWireframe) NeonCyan else Color.White)
                        }
                        IconButton(onClick = { viewModel.resetView() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = NeonCyan)
                        }
                    }
                }
                Slider(
                    value = uiState.ipdDistance,
                    onValueChange = { viewModel.setIpdDistance(it) },
                    valueRange = 0.05f..0.25f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
            }
        }
    }
}

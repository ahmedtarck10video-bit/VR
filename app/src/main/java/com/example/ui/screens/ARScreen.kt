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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.SceneviewARViewport
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

    val currentModel = uiState.models.getOrNull(uiState.selectedModelIndex)

    val gyroPitch = uiState.sensorOrientation.pitch * 0.015f
    val gyroRoll = uiState.sensorOrientation.roll * 0.015f

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            // Unified Hardware-Accelerated AR Mode with Google ARCore & Filament
            SceneviewARViewport(
                model = currentModel,
                rotX = uiState.rotX,
                rotY = uiState.rotY,
                rotZ = uiState.rotZ,
                scale = uiState.scale,
                panX = uiState.panX,
                panY = uiState.panY,
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
            )

            // Floating HUD Overlay for AR Tracking & Hit Status
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            val anchor = uiState.surfaceAnchor
                            if (anchor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (anchor.isReal6DOFTracking) Color(0xFF4CAF50) else Color(0xFFFFB300))
                                )
                                Text(
                                    text = "${anchor.hitType.label} • ${anchor.surfaceType.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2196F3))
                                )
                                Text(
                                    text = "Tap surface to Anchor",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // AR Suite Tools Quick Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleArSuitePanel() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "AR Suite",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            // AR Suite Bottom Sheet modal
            if (uiState.isArSuitePanelOpen) {
                com.example.ui.components.ARSuiteBottomSheet(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDismiss = { viewModel.toggleArSuitePanel() }
                )
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


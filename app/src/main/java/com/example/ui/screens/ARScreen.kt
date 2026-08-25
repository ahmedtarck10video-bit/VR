package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.engine.Renderer3D
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
    val currentModel = uiState.models.getOrNull(uiState.selectedModelIndex) ?: return

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
                            onDoubleTap = {
                                viewModel.resetPosition()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            // 1. Pan moves model on screen / room (drag down to floor or anywhere!)
                            if (pan.x != 0f || pan.y != 0f) {
                                viewModel.updatePan(pan.x, pan.y)
                            }
                            // 2. Zoom scales model
                            if (zoom != 1.0f) {
                                viewModel.updateScale(zoom)
                            }
                            // 3. Rotation gesture rotates model
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

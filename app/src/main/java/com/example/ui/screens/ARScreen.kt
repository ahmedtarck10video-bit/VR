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

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(modifier = Modifier.fillMaxSize())

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            viewModel.updateRotation(
                                deltaX = -pan.y * 0.008f,
                                deltaY = pan.x * 0.008f
                            )
                            viewModel.updateScale(zoom)
                        }
                    }
            ) {
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
                    drawShadow = false
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

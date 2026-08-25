package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.Renderer3D
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
    val renderer = remember { Renderer3D() }
    val currentModel = uiState.models.getOrNull(uiState.selectedModelIndex) ?: return

    val headPitch = uiState.sensorOrientation.pitch * 0.03f
    val headRoll = uiState.sensorOrientation.roll * 0.03f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Dual Stereo Split Screen (Left & Right Eye)
        Row(
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
            // Left Eye
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                            radius = 600f
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val halfWidth = size.width
                    renderer.render(
                        drawScope = this,
                        model = currentModel,
                        rotX = uiState.rotX + headPitch,
                        rotY = uiState.rotY + headRoll - (uiState.ipdDistance * 0.5f),
                        rotZ = 0f,
                        scale = uiState.scale * 0.85f,
                        panX = uiState.panX - 25f,
                        panY = uiState.panY,
                        wireframe = uiState.isWireframe,
                        primaryColor = uiState.modelColor,
                        drawShadow = false
                    )
                }
                Text(
                    text = "L",
                    color = NeonCyan.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Divider Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color(0x33FFFFFF))
            )

            // Right Eye
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                            radius = 600f
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    renderer.render(
                        drawScope = this,
                        model = currentModel,
                        rotX = uiState.rotX + headPitch,
                        rotY = uiState.rotY + headRoll + (uiState.ipdDistance * 0.5f),
                        rotZ = 0f,
                        scale = uiState.scale * 0.85f,
                        panX = uiState.panX + 25f,
                        panY = uiState.panY,
                        wireframe = uiState.isWireframe,
                        primaryColor = uiState.modelColor,
                        drawShadow = false
                    )
                }
                Text(
                    text = "R",
                    color = NeonCyan.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Floating Liquid Glass Control Bar at bottom
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = Color(0x400F172A),
            borderColor = Color(0x4DFFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VR IPD Distance: ${(uiState.ipdDistance * 100).toInt()} mm",
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

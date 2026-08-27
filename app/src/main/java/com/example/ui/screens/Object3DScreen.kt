package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.Sceneview3DViewport
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.MRUiState
import com.example.viewmodel.MixedRealityViewModel

@Composable
fun Object3DScreen(
    uiState: MRUiState,
    viewModel: MixedRealityViewModel,
    modifier: Modifier = Modifier
) {
    val currentModel = uiState.models.getOrNull(uiState.selectedModelIndex) ?: return

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (uiState.isAutoSpin) 6.28318f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing)
        ),
        label = "spinAngle"
    )

    val colorPalette = listOf(
        Color(0xFF00E5FF),
        Color(0xFF0077FF),
        Color(0xFF9D4EDD),
        Color(0xFF00FF88),
        Color(0xFFFF3366),
        Color(0xFFFFB703)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0x3300E5FF),
                        Color(0x1A0F172A),
                        Color(0xFF070A10)
                    ),
                    radius = 1100f
                )
            )
    ) {
        // Unified Hardware-Accelerated 3D PBR Engine (Sceneview + Filament)
        Sceneview3DViewport(
            model = currentModel,
            rotX = uiState.rotX,
            rotY = uiState.rotY,
            rotZ = uiState.rotZ,
            scale = uiState.scale,
            panX = uiState.panX,
            panY = uiState.panY,
            isAutoSpin = uiState.isAutoSpin,
            autoSpinAngle = spinAngle,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        if (zoom != 1.0f) {
                            viewModel.updateScale(zoom)
                        }
                        if (rotation != 0f) {
                            viewModel.updateRotation(
                                deltaX = 0f,
                                deltaY = 0f,
                                deltaZ = rotation * 0.02f
                            )
                        }
                        if (pan.x != 0f || pan.y != 0f) {
                            viewModel.updateRotation(
                                deltaX = -pan.y * 0.008f,
                                deltaY = pan.x * 0.008f
                            )
                        }
                    }
                }
        )

        // Top Model Info Glass Card
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0x331E293B),
            borderColor = Color(0x4DFFFFFF),
            borderGlow = NeonCyan
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentModel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(uiState.scale * 100).toInt()}% Zoom",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentModel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xCCFFFFFF)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (currentModel.triangles.isNotEmpty()) "${currentModel.triangles.size} Tris" else "Hardware PBR Mesh",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0x8800E5FF)
                    )
                    Text(
                        text = "Size: ${String.format("%.2f", currentModel.realWorldWidthMeters)}m × ${String.format("%.2f", currentModel.realWorldHeightMeters)}m × ${String.format("%.2f", currentModel.realWorldDepthMeters)}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xAAFFFFFF)
                    )
                }
            }
        }

        // Bottom Controls Shelf
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(14.dp)
        ) {
            // Model Switcher Cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(uiState.models) { index, model ->
                    val isSelected = index == uiState.selectedModelIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x261E293B))
                            .border(1.dp, if (isSelected) NeonCyan else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectModel(index) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = model.name,
                            color = if (isSelected) NeonCyan else Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Toolbar (Colors, Wireframe, AutoSpin, Reset)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = Color(0x401E293B),
                borderColor = Color(0x4DFFFFFF)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color Selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        colorPalette.forEach { color ->
                            val isSelected = uiState.modelColor == color
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0x40FFFFFF),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setModelColor(color) }
                            )
                        }
                    }

                    // Wireframe Toggle
                    IconButton(onClick = { viewModel.toggleWireframe() }) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Wireframe",
                            tint = if (uiState.isWireframe) NeonCyan else Color.Gray
                        )
                    }

                    // Auto Spin Toggle
                    IconButton(onClick = { viewModel.toggleAutoSpin() }) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Auto Spin",
                            tint = if (uiState.isAutoSpin) NeonCyan else Color.Gray
                        )
                    }

                    // Reset Orientation & Zoom
                    IconButton(onClick = { viewModel.resetView() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = NeonCyan
                        )
                    }
                }
            }
        }
    }
}

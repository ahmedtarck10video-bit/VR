package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.RenderEngineProfile
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.MixedRealityViewModel
import com.example.viewmodel.SpatialEnvironment

@Composable
fun SpatialSettingsScreen(
    viewModel: MixedRealityViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val accentColors = listOf(
        Color(0xFF00E5FF) to "Neon Cyan",
        Color(0xFF9D4EDD) to "Electric Purple",
        Color(0xFF00FF88) to "Emerald Green",
        Color(0xFFFFB703) to "Solar Gold",
        Color(0xFFFF3366) to "Plasma Pink"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Liquid Glass & Spatial OS Settings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        // Spatial Environment Selection
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0x261E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Landscape, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Immersive Environment", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpatialEnvironment.values().forEach { env ->
                        val isSelected = uiState.environment == env
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0x1AFFFFFF))
                                .border(1.dp, if (isSelected) NeonCyan else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { viewModel.setEnvironment(env) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = env.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonCyan else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Neon Accent Colors
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0x261E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = uiState.modelColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Liquid Glass Tint & Accent", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    accentColors.forEach { (color, name) ->
                        val isSelected = uiState.modelColor == color
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0x40FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setModelColor(color) }
                        )
                    }
                }
            }
        }

        // Spatial Audio & Gyroscope Sensors
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0x261E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SurroundSound, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Spatial Audio Engine", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("HRTF binaural 3D audio cues", color = Color(0x88FFFFFF), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = uiState.spatialAudioEnabled,
                        onCheckedChange = { viewModel.toggleSpatialAudio() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonCyan,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }

                Divider(color = Color(0x1AFFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Continuous Sensor Tracking", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("9-DOF gyroscope & accelerometer fusion", color = Color(0x88FFFFFF), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonCyan
                        )
                    )
                }
            }
        }

        // 3D & AR Rendering Engines & Frameworks Section
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0x261E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("3D & AR Rendering Engines", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }

                    Text(
                        text = "Active: ${uiState.renderEngineProfile.shortName}",
                        color = uiState.renderEngineProfile.themeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "Tap an engine profile below to switch GLB/glTF shading, PBR specular reflection, and AR shadow pipelines:",
                    color = Color(0xAAFFFFFF),
                    fontSize = 12.sp
                )

                RenderEngineProfile.values().forEach { profile ->
                    val isSelected = uiState.renderEngineProfile == profile

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) profile.themeColor.copy(alpha = 0.15f) else Color(0x1AFFFFFF))
                            .border(
                                width = if (isSelected) 1.5f.dp else 1.dp,
                                color = if (isSelected) profile.themeColor else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setRenderEngineProfile(profile) }
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = profile.themeColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) profile.themeColor else profile.themeColor.copy(alpha = 0.2f))
                                        .border(1.dp, profile.themeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) "APPLIED" else "3D Engine",
                                        color = if (isSelected) Color.Black else profile.themeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = profile.description,
                                color = Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

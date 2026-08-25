package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.MRUiState
import com.example.viewmodel.MixedRealityViewModel
import com.example.viewmodel.SpatialAppId
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SpatialTopBar(
    uiState: MRUiState,
    onOpenLauncher: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Launcher trigger pill
        GlassCard(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onOpenLauncher() },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color(0x331E293B),
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Launchpad",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Launchpad",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        // Center Time & Environment Pill
        GlassCard(
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color(0x400F172A),
            borderColor = Color(0x4DFFFFFF),
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00FF88))
                )
                Text(
                    text = currentTime.ifEmpty { "12:00 PM" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "•  ${uiState.environment.displayName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xAAFFFFFF),
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Right Quick Controls (Battery, Audio & Settings)
        GlassCard(
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color(0x331E293B),
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (uiState.spatialAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Audio",
                    tint = if (uiState.spatialAudioEnabled) NeonCyan else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery",
                    tint = Color(0xFF00FF88),
                    modifier = Modifier.size(16.dp)
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onOpenSettings() }
                )
            }
        }
    }
}

@Composable
fun SpatialGlassDock(
    activeAppId: SpatialAppId,
    openWindows: Map<SpatialAppId, com.example.viewmodel.WindowState>,
    onAppClick: (SpatialAppId) -> Unit,
    onOpenLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dockApps = listOf(
        DockItem(SpatialAppId.STUDIO_3D, "3D Studio", Icons.Default.ViewInAr, Color(0xFF00E5FF)),
        DockItem(SpatialAppId.AR_MODE, "AR Space", Icons.Default.CameraAlt, Color(0xFF00FF88)),
        DockItem(SpatialAppId.STEREO_VR, "VR Vision", Icons.Default.Visibility, Color(0xFF9D4EDD)),
        DockItem(SpatialAppId.GALLERY, "Files", Icons.Default.Collections, Color(0xFFFFB703)),
        DockItem(SpatialAppId.NOTES, "Notes", Icons.Default.EditNote, Color(0xFFFF3366)),
        DockItem(SpatialAppId.SETTINGS, "Settings", Icons.Default.Settings, Color(0xFF38BDF8))
    )

    GlassCard(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
        backgroundColor = Color(0x400F172A),
        borderColor = Color(0x66FFFFFF),
        borderGlow = NeonCyan,
        elevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Launcher Icon Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x26FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onOpenLauncher() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "All Apps",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            VerticalDivider(
                modifier = Modifier.height(28.dp),
                color = Color(0x33FFFFFF)
            )

            // Pinned & Running Apps
            dockApps.forEach { item ->
                val windowState = openWindows[item.id]
                val isRunning = windowState?.isOpen == true
                val isFocused = isRunning && activeAppId == item.id && !(windowState?.isMinimized ?: false)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { onAppClick(item.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(if (isFocused) 10.dp else 0.dp, CircleShape, spotColor = item.color)
                            .clip(CircleShape)
                            .background(
                                brush = if (isFocused) Brush.radialGradient(
                                    listOf(item.color.copy(alpha = 0.5f), Color(0x331E293B))
                                ) else Brush.linearGradient(
                                    listOf(Color(0x1AFFFFFF), Color(0x1AFFFFFF))
                                )
                            )
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) item.color else Color(0x26FFFFFF),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            tint = if (isFocused) Color.White else Color(0xDDFFFFFF),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Dot indicator for running app
                    if (isRunning) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isFocused) item.color else Color(0x88FFFFFF))
                        )
                    }
                }
            }
        }
    }
}

private data class DockItem(
    val id: SpatialAppId,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

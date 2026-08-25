package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.viewmodel.SpatialAppId

data class AppLauncherItem(
    val id: SpatialAppId,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun SpatialAppLauncher(
    isOpen: Boolean,
    onClose: () -> Unit,
    onLaunchApp: (SpatialAppId) -> Unit,
    modifier: Modifier = Modifier
) {
    val apps = listOf(
        AppLauncherItem(SpatialAppId.STUDIO_3D, "3D Studio", Icons.Default.ViewInAr, Color(0xFF00E5FF)),
        AppLauncherItem(SpatialAppId.AR_MODE, "AR Space", Icons.Default.CameraAlt, Color(0xFF00FF88)),
        AppLauncherItem(SpatialAppId.STEREO_VR, "VR Vision", Icons.Default.Visibility, Color(0xFF9D4EDD)),
        AppLauncherItem(SpatialAppId.GALLERY, "Holo Files", Icons.Default.Collections, Color(0xFFFFB703)),
        AppLauncherItem(SpatialAppId.NOTES, "Spatial Notes", Icons.Default.EditNote, Color(0xFFFF3366)),
        AppLauncherItem(SpatialAppId.SETTINGS, "Settings", Icons.Default.Settings, Color(0xFF38BDF8))
    )

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0x800B0E14))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.72f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                backgroundColor = Color(0x660F172A),
                borderColor = Color(0x66FFFFFF),
                borderGlow = NeonCyan
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Spatial App Launchpad",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Tap to open in floating Liquid Glass space",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xAAFFFFFF))
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(apps) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable {
                                    onLaunchApp(app.id)
                                    onClose()
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .shadow(12.dp, CircleShape, spotColor = app.color)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    app.color.copy(alpha = 0.4f),
                                                    Color(0x331E293B)
                                                )
                                            )
                                        )
                                        .border(
                                            1.5.dp,
                                            Brush.linearGradient(
                                                listOf(Color.White.copy(alpha = 0.8f), app.color)
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = app.icon,
                                        contentDescription = app.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    GlassIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close Launchpad",
                        onClick = onClose,
                        backgroundColor = Color(0x33FFFFFF)
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.NeonCyan
import com.example.viewmodel.MixedRealityViewModel

data class SpatialAsset(
    val title: String,
    val category: String,
    val vertices: Int,
    val triangles: Int,
    val modelIndex: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun SpatialGalleryScreen(
    viewModel: MixedRealityViewModel,
    onOpenIn3D: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val assets = remember {
        listOf(
            SpatialAsset("MR Spatial Visor", "Hardware", 840, 1680, 0, Icons.Default.ViewInAr),
            SpatialAsset("Autonomous Drone", "Robotics", 620, 1240, 1, Icons.Default.FlightTakeoff),
            SpatialAsset("Companion Bot", "AI Unit", 950, 1900, 2, Icons.Default.SmartToy),
            SpatialAsset("Hologram Cube", "Primitive", 8, 12, 3, Icons.Default.Category),
            SpatialAsset("Spatial Anchor Tag", "Tracking", 120, 240, 0, Icons.Default.LocationOn),
            SpatialAsset("Laser Scanner Mesh", "Telemetry", 450, 900, 1, Icons.Default.QrCodeScanner)
        )
    }

    var selectedIndex by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Spatial Holographic Library",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Holographic assets & volumetric captures",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xAAFFFFFF))
                )
            }

            Button(
                onClick = { onOpenIn3D(assets[selectedIndex].modelIndex) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Project in 3D", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Asset Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(assets) { index, asset ->
                val isSelected = selectedIndex == index
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .clickable { selectedIndex = index },
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = if (isSelected) Color(0x4D00E5FF) else Color(0x261E293B),
                    borderColor = if (isSelected) NeonCyan else Color(0x33FFFFFF),
                    borderGlow = if (isSelected) NeonCyan else Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x33FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = asset.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = asset.category,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) NeonCyan else Color(0x88FFFFFF)
                                )
                            )
                        }

                        Column {
                            Text(
                                text = asset.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "${asset.vertices} verts • ${asset.triangles} polys",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xAAFFFFFF),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

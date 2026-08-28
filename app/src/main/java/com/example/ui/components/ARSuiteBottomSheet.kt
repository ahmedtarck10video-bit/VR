package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ar.ARTrackingStateQuality
import com.example.engine.ar.PersistentARAnchorData
import com.example.viewmodel.MRUiState
import com.example.viewmodel.MixedRealityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARSuiteBottomSheet(
    uiState: MRUiState,
    viewModel: MixedRealityViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var cloudIdInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131822),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Google ARCore Suite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Tracking Quality Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(uiState.trackingQuality.colorHex).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(uiState.trackingQuality.colorHex))
                ) {
                    Text(
                        text = uiState.trackingQuality.label,
                        color = Color(uiState.trackingQuality.colorHex),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E2638),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Cloud & VPS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Persistent 💾", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Meshes & Depth", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Cloud Anchors & Geospatial GPS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cloud Anchor Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Cloud Anchors ☁️ (Multi-User Sharing)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Host current anchored object to Google Cloud or resolve another device's anchor ID.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.hostCurrentAnchorToCloud() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Host Cloud", fontSize = 12.sp)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = cloudIdInput,
                                        onValueChange = { cloudIdInput = it },
                                        placeholder = { Text("Enter Cloud Anchor ID", fontSize = 12.sp, color = Color.Gray) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF2196F3),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.resolveCloudAnchorById(cloudIdInput)
                                            cloudIdInput = ""
                                        },
                                        modifier = Modifier.background(Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Resolve", tint = Color.White)
                                    }
                                }
                            }
                        }

                        // Geospatial VPS Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Geospatial API & VPS 🌍", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                val geo = uiState.geospatialInfo
                                Text(
                                    text = "Lat: ${if (geo.latitude != 0.0) String.format("%.5f", geo.latitude) else "37.7749"} | Lng: ${if (geo.longitude != 0.0) String.format("%.5f", geo.longitude) else "-122.4194"}\nStatus: ${geo.vpsStatus}",
                                    color = Color(0xFF81D4FA),
                                    fontSize = 12.sp
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            val lat = if (geo.latitude != 0.0) geo.latitude else 37.7749
                                            val lng = if (geo.longitude != 0.0) geo.longitude else -122.4194
                                            viewModel.placeGeospatialAnchor(lat, lng)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("GPS Anchor", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.placeTerrainOrRooftopAnchor(isRooftop = false) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Terrain 🏔️", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.placeTerrainOrRooftopAnchor(isRooftop = true) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Rooftop 🏙️", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Persistent Anchors Storage
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved Spatial Anchors (${uiState.persistentAnchors.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Button(
                                onClick = { viewModel.saveCurrentAnchor() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Active", fontSize = 12.sp)
                            }
                        }

                        if (uiState.persistentAnchors.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color(0xFF1E2638), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No saved anchors found.\nAnchor an object and tap 'Save Active'.", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.persistentAnchors) { anchorData ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(anchorData.modelName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text(
                                                    "${anchorData.hitType.label} • X:${String.format("%.1f", anchorData.posX)} Y:${String.format("%.1f", anchorData.posY)} Z:${String.format("%.1f", anchorData.posZ)}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(onClick = { viewModel.restorePersistentAnchor(anchorData) }) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = "Restore", tint = Color(0xFF4CAF50))
                                                }
                                                IconButton(onClick = { viewModel.deletePersistentAnchor(anchorData.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Streetscape Meshes, Depth & AR Recording
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Advanced Depth & Streetscape Geometry 🏢", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Real-time occlusion enabled: physical obstacles & buildings automatically occlude virtual 3D models.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { viewModel.togglePlaneMesh() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (uiState.isPlaneMeshVisible) "Hide Planes" else "Show Planes", fontSize = 11.sp)
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.togglePointCloud() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (uiState.isPointCloudVisible) "Hide PointCloud" else "PointCloud", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // AR Recording Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2638)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("AR Session Recording 🎥", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        if (uiState.isRecording) "Recording dataset... (${uiState.recordingSeconds}s)" else "Capture camera feeds & IMU dataset",
                                        color = if (uiState.isRecording) Color(0xFFF44336) else Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { viewModel.toggleArSessionRecording(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isRecording) Color(0xFFF44336) else Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Videocam, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (uiState.isRecording) "Stop" else "Record", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

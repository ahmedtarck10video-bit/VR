package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.GlassCard
import com.example.ui.theme.NeonCyan

data class SpatialNote(
    val id: String,
    val title: String,
    val text: String,
    val time: String,
    val tag: String
)

@Composable
fun SpatialNotesScreen(
    modifier: Modifier = Modifier
) {
    var notes by remember {
        mutableStateOf(
            listOf(
                SpatialNote("1", "Optics Calibration", "Calibrated eye tracking sensors and verified 60fps low-latency spatial passthrough.", "10:42 AM", "Hardware"),
                SpatialNote("2", "3D Mesh Export", "Exported bipedal companion robot mesh with vertex normal smoothing.", "11:15 AM", "Models"),
                SpatialNote("3", "Spatial Anchor #04", "Pinned AR holographic board in physical workspace coordinate (x: 0.4, y: 1.2, z: -1.5).", "01:20 PM", "AR Anchor")
            )
        )
    }

    var newNoteText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Spatial Notes & Transcripts",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        // Add note field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                placeholder = { Text("Dictate or type spatial note...", color = Color(0x66FFFFFF), fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0x261E293B),
                    unfocusedContainerColor = Color(0x1A1E293B)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    if (newNoteText.isNotBlank()) {
                        notes = listOf(
                            SpatialNote(
                                id = System.currentTimeMillis().toString(),
                                title = "Quick Spatial Memo",
                                text = newNoteText.trim(),
                                time = "Just now",
                                tag = "Memo"
                            )
                        ) + notes
                        newNoteText = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.85f))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Add", tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }

        // Notes list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(notes, key = { it.id }) { note ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = Color(0x261E293B),
                    borderColor = Color(0x33FFFFFF)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = note.tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonCyan,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xDDFFFFFF))
                        )

                        Text(
                            text = note.time,
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0x66FFFFFF))
                        )
                    }
                }
            }
        }
    }
}

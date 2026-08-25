package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import kotlin.math.roundToInt

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0x331E293B),
    borderColor: Color = Color(0x4DFFFFFF),
    borderGlow: Color = NeonCyan.copy(alpha = 0.35f),
    elevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, spotColor = borderGlow)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.65f),
                        backgroundColor.copy(alpha = 0.35f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderGlow.copy(alpha = 0.4f),
                        Color(0x1AFFFFFF)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    backgroundColor: Color = Color(0x33FFFFFF),
    size: Dp = 44.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun SpatialWindow(
    title: String,
    icon: ImageVector,
    isOpen: Boolean,
    isMaximized: Boolean,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.85f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.85f)
    ) {
        Box(
            modifier = (if (isMaximized) {
                Modifier.fillMaxSize().padding(12.dp)
            } else {
                modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.82f)
            })
        ) {
            GlassCard(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color(0x4D0F172A),
                borderColor = Color(0x66FFFFFF),
                borderGlow = accentColor
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Window Title Bar (Draggable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x33FFFFFF),
                                        accentColor.copy(alpha = 0.15f),
                                        Color(0x1AFFFFFF)
                                    )
                                )
                            )
                            .pointerInput(isMaximized) {
                                if (!isMaximized) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offsetX = (offsetX + dragAmount.x).coerceIn(-150f, 150f)
                                        offsetY = (offsetY + dragAmount.y).coerceIn(-200f, 200f)
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Traffic lights (Close, Minimize, Maximize)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5F56))
                                    .clickable { onClose() }
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFBD2E))
                                    .clickable { onMinimize() }
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF27C93F))
                                    .clickable { onToggleMaximize() }
                            )
                        }

                        // Window Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )
                        }

                        // Right actions (Maximize toggle & close)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onToggleMaximize,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMaximized) Icons.Default.CropSquare else Icons.Default.Fullscreen,
                                    contentDescription = "Expand",
                                    tint = Color(0xCCFFFFFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xCCFFFFFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Window Content Body
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

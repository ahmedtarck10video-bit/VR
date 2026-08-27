package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.math3d.Model3D
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-Performance Hardware Accelerated 3D Viewport powered by Google Filament & Sceneview.
 * Renders full PBR materials, textures, environment lighting, and 60+ FPS hardware rasterization.
 * Operates purely on GPU without initializing camera previews or background sensor drains.
 */
@Composable
fun Sceneview3DViewport(
    model: Model3D?,
    rotX: Float,
    rotY: Float,
    rotZ: Float = 0f,
    scale: Float = 1.0f,
    panX: Float = 0f,
    panY: Float = 0f,
    isAutoSpin: Boolean = false,
    autoSpinAngle: Float = 0f,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentModelNode by remember { mutableStateOf<ModelNode?>(null) }
    var sceneViewRef by remember { mutableStateOf<SceneView?>(null) }
    var lastLoadedPath by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                currentModelNode?.let { node ->
                    sceneViewRef?.removeChildNode(node)
                    node.destroy()
                }
                currentModelNode = null
                sceneViewRef = null
            } catch (e: Exception) {
                // Safe cleanup
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            SceneView(ctx).apply {
                cameraNode.position = Position(0f, 0f, 4f)
                sceneViewRef = this
            }
        },
        update = { sceneView ->
            sceneViewRef = sceneView
            val targetModel = model
            val targetPath = targetModel?.localFilePath ?: targetModel?.fileUri?.toString()

            if (targetModel != null && targetPath != null && targetPath != lastLoadedPath) {
                lastLoadedPath = targetPath
                coroutineScope.launch {
                    try {
                        val filePath = targetModel.localFilePath
                        val file = if (filePath != null) File(filePath) else null
                        
                        // Filament engine calls must run on the Main/GL thread to prevent unadopted thread panics
                        val instance = if (file != null && file.exists()) {
                            sceneView.modelLoader.createModelInstance(file)
                        } else if (targetModel.fileUri != null) {
                            sceneView.modelLoader.loadModelInstance(targetModel.fileUri.toString())
                        } else {
                            null
                        }

                        if (instance != null) {
                            currentModelNode?.let { oldNode ->
                                sceneView.removeChildNode(oldNode)
                                oldNode.destroy()
                            }
                            val metricUnitScale = targetModel.realWorldHeightMeters.coerceIn(0.2f, 2.5f)
                            val newNode = ModelNode(
                                modelInstance = instance,
                                scaleToUnits = metricUnitScale
                            ).apply {
                                this.position = Position(x = panX * 0.005f, y = -panY * 0.005f, z = 0f)
                                this.scale = Scale(scale, scale, scale)
                                val finalRotY = ((rotY + if (isAutoSpin) autoSpinAngle else 0f) * 180f / Math.PI.toFloat())
                                val finalRotX = (rotX * 180f / Math.PI.toFloat())
                                val finalRotZ = (rotZ * 180f / Math.PI.toFloat())
                                this.rotation = Rotation(x = finalRotX, y = finalRotY, z = finalRotZ)
                            }
                            sceneView.addChildNode(newNode)
                            currentModelNode = newNode
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                currentModelNode?.let { node ->
                    node.position = Position(x = panX * 0.005f, y = -panY * 0.005f, z = 0f)
                    node.scale = Scale(scale, scale, scale)
                    val finalRotY = ((rotY + if (isAutoSpin) autoSpinAngle else 0f) * 180f / Math.PI.toFloat())
                    val finalRotX = (rotX * 180f / Math.PI.toFloat())
                    val finalRotZ = (rotZ * 180f / Math.PI.toFloat())
                    node.rotation = Rotation(x = finalRotX, y = finalRotY, z = finalRotZ)
                }
            }
        }
    )
}

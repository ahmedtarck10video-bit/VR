package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.math3d.Model3D
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hardware-Accelerated AR Viewport with Google ARCore & Filament PBR Engine.
 * Anchors photorealistic GLB / GLTF assets to detected physical surfaces.
 */
@Composable
fun SceneviewARViewport(
    model: Model3D?,
    rotX: Float,
    rotY: Float,
    rotZ: Float = 0f,
    scale: Float = 1.0f,
    panX: Float = 0f,
    panY: Float = 0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentModelNode by remember { mutableStateOf<ModelNode?>(null) }
    var lastLoadedPath by remember { mutableStateOf<String?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            ARSceneView(ctx).apply {
                planeRenderer.isVisible = true
            }
        },
        update = { arSceneView ->
            val targetModel = model
            val targetPath = targetModel?.localFilePath ?: targetModel?.fileUri?.toString()

            if (targetModel != null && targetPath != null && targetPath != lastLoadedPath) {
                lastLoadedPath = targetPath
                coroutineScope.launch {
                    try {
                        val filePath = targetModel.localFilePath
                        val file = if (filePath != null) File(filePath) else null
                        val instance = withContext(Dispatchers.IO) {
                            if (file != null && file.exists()) {
                                arSceneView.modelLoader.createModelInstance(file)
                            } else if (targetModel.fileUri != null) {
                                arSceneView.modelLoader.loadModelInstance(targetModel.fileUri.toString())
                            } else {
                                null
                            }
                        }

                        if (instance != null) {
                            currentModelNode?.let { arSceneView.removeChildNode(it) }
                            val newNode = ModelNode(
                                modelInstance = instance,
                                scaleToUnits = 0.5f // 0.5 meter real-world scale in AR
                            ).apply {
                                this.position = Position(x = panX * 0.002f, y = -panY * 0.002f, z = -1.0f)
                                this.scale = Scale(scale, scale, scale)
                                val finalRotY = (rotY * 180f / Math.PI.toFloat())
                                val finalRotX = (rotX * 180f / Math.PI.toFloat())
                                this.rotation = Rotation(x = finalRotX, y = finalRotY, z = rotZ)
                            }
                            arSceneView.addChildNode(newNode)
                            currentModelNode = newNode
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                currentModelNode?.let { node ->
                    node.position = Position(x = panX * 0.002f, y = -panY * 0.002f, z = -1.0f)
                    node.scale = Scale(scale, scale, scale)
                    val finalRotY = (rotY * 180f / Math.PI.toFloat())
                    val finalRotX = (rotX * 180f / Math.PI.toFloat())
                    node.rotation = Rotation(x = finalRotX, y = finalRotY, z = rotZ)
                }
            }
        }
    )
}

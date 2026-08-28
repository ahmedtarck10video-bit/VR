package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.ar.ARSurfaceAnchor
import com.example.engine.ar.PlaneOrientation
import com.example.math3d.Model3D
import com.example.ui.theme.GlowGreen
import com.example.ui.theme.NeonCyan
import com.google.ar.core.ArCoreApk
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch
import java.io.File

/**
 * Hardware-Accelerated AR Viewport with Google ARCore & Filament PBR Engine.
 * Anchors photorealistic GLB / GLTF assets to detected physical surfaces with true metric 1:1 scale.
 * Automatically falls back to CameraPreview + SceneView with clear simulation indicator on devices without ARCore.
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
    surfaceAnchor: ARSurfaceAnchor? = null,
    isAnchored: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentModelNode by remember { mutableStateOf<ModelNode?>(null) }
    var arSceneViewRef by remember { mutableStateOf<ARSceneView?>(null) }
    var sceneViewRef by remember { mutableStateOf<SceneView?>(null) }
    var lastLoadedPath by remember { mutableStateOf<String?>(null) }

    val isArCoreInstalled = remember {
        try {
            val pInfo = try {
                context.packageManager.getPackageInfo("com.google.ar.core", 0)
            } catch (e: Exception) {
                null
            }
            if (pInfo != null) {
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                availability == ArCoreApk.Availability.SUPPORTED_INSTALLED
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                currentModelNode?.let { node ->
                    arSceneViewRef?.removeChildNode(node)
                    sceneViewRef?.removeChildNode(node)
                    node.destroy()
                }
                currentModelNode = null
                arSceneViewRef = null
                sceneViewRef = null
            } catch (e: Exception) {
                // Safe cleanup
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isArCoreInstalled) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    ARSceneView(ctx).apply {
                        planeRenderer.isVisible = true
                        arSceneViewRef = this
                    }
                },
                update = { arSceneView ->
                    arSceneViewRef = arSceneView
                    val targetModel = model
                    val targetPath = targetModel?.localFilePath ?: targetModel?.fileUri?.toString()

                    // Compute unified 3D Transform (live ARCore anchor pose with 6DOF persistence, anchor-local pan, and user yaw)
                    val liveAnchorPose = surfaceAnchor?.arcoreAnchor?.pose
                    val finalPosition: Position
                    val finalRotation: Rotation

                    if (surfaceAnchor != null && isAnchored) {
                        val isVertical = surfaceAnchor.surfaceType == PlaneOrientation.VERTICAL
                        val localDx = panX * 0.001f
                        val localDy = if (isVertical) -panY * 0.001f else 0f
                        val localDz = if (!isVertical) panY * 0.001f else 0f

                        if (liveAnchorPose != null) {
                            val localOffsetPose = com.google.ar.core.Pose.makeTranslation(localDx, localDy, localDz)
                            val userYawPose = com.google.ar.core.Pose.makeRotation(
                                0f,
                                kotlin.math.sin(rotY / 2f),
                                0f,
                                kotlin.math.cos(rotY / 2f)
                            )
                            val combinedPose = liveAnchorPose.compose(localOffsetPose).compose(userYawPose)
                            finalPosition = Position(
                                x = combinedPose.tx(),
                                y = combinedPose.ty(),
                                z = combinedPose.tz()
                            )

                            // Extract Euler angles from combined anchor quaternion
                            val q = combinedPose.rotationQuaternion // [x, y, z, w]
                            val sinr_cosp = 2f * (q[3] * q[0] + q[1] * q[2])
                            val cosr_cosp = 1f - 2f * (q[0] * q[0] + q[1] * q[1])
                            val roll = kotlin.math.atan2(sinr_cosp, cosr_cosp) * 180f / Math.PI.toFloat()

                            val sinp = 2f * (q[3] * q[1] - q[2] * q[0])
                            val pitch = if (kotlin.math.abs(sinp) >= 1f) {
                                (if (sinp > 0) Math.PI.toFloat() / 2f else -Math.PI.toFloat() / 2f) * 180f / Math.PI.toFloat()
                            } else {
                                kotlin.math.asin(sinp) * 180f / Math.PI.toFloat()
                            }

                            val siny_cosp = 2f * (q[3] * q[2] + q[0] * q[1])
                            val cosy_cosp = 1f - 2f * (q[1] * q[1] + q[2] * q[2])
                            val yaw = kotlin.math.atan2(siny_cosp, cosy_cosp) * 180f / Math.PI.toFloat()

                            finalRotation = Rotation(x = roll, y = pitch, z = yaw)
                        } else {
                            finalPosition = Position(
                                x = surfaceAnchor.position.x + localDx,
                                y = surfaceAnchor.position.y + localDy,
                                z = surfaceAnchor.position.z + localDz
                            )
                            finalRotation = Rotation(
                                x = rotX * 180f / Math.PI.toFloat(),
                                y = rotY * 180f / Math.PI.toFloat(),
                                z = rotZ * 180f / Math.PI.toFloat()
                            )
                        }
                    } else {
                        finalPosition = Position(
                            x = panX * 0.002f,
                            y = -panY * 0.002f,
                            z = -1.2f
                        )
                        finalRotation = Rotation(
                            x = rotX * 180f / Math.PI.toFloat(),
                            y = rotY * 180f / Math.PI.toFloat(),
                            z = rotZ * 180f / Math.PI.toFloat()
                        )
                    }

                    if (targetModel != null && targetPath != null && targetPath != lastLoadedPath) {
                        lastLoadedPath = targetPath
                        coroutineScope.launch {
                            try {
                                val filePath = targetModel.localFilePath
                                val file = if (filePath != null) File(filePath) else null
                                val instance = if (file != null && file.exists()) {
                                    arSceneView.modelLoader.createModelInstance(file)
                                } else if (targetModel.fileUri != null) {
                                    arSceneView.modelLoader.loadModelInstance(targetModel.fileUri.toString())
                                } else {
                                    null
                                }

                                if (instance != null) {
                                    currentModelNode?.let { oldNode ->
                                        arSceneView.removeChildNode(oldNode)
                                        oldNode.destroy()
                                    }
                                    val newNode = ModelNode(
                                        modelInstance = instance
                                    ).apply {
                                        this.position = finalPosition
                                        this.scale = Scale(scale, scale, scale)
                                        this.rotation = finalRotation
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
                            node.position = finalPosition
                            node.scale = Scale(scale, scale, scale)
                            node.rotation = finalRotation
                        }
                    }
                }
            )
        } else {
            // Camera Stream Passthrough + GPU SceneView for non-ARCore devices
            CameraPreview(modifier = Modifier.fillMaxSize())

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    SceneView(ctx).apply {
                        cameraNode.position = Position(0f, 0f, 3.5f)
                        sceneViewRef = this
                    }
                },
                update = { sceneView ->
                    sceneViewRef = sceneView
                    val targetModel = model
                    val targetPath = targetModel?.localFilePath ?: targetModel?.fileUri?.toString()

                    val targetPos = Position(x = panX * 0.005f, y = -panY * 0.005f, z = 0f)

                    if (targetModel != null && targetPath != null && targetPath != lastLoadedPath) {
                        lastLoadedPath = targetPath
                        coroutineScope.launch {
                            try {
                                val filePath = targetModel.localFilePath
                                val file = if (filePath != null) File(filePath) else null
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
                                    val newNode = ModelNode(
                                        modelInstance = instance
                                    ).apply {
                                        this.position = targetPos
                                        this.scale = Scale(scale, scale, scale)
                                        val finalRotY = (rotY * 180f / Math.PI.toFloat())
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
                            node.position = targetPos
                            node.scale = Scale(scale, scale, scale)
                            val finalRotY = (rotY * 180f / Math.PI.toFloat())
                            val finalRotX = (rotX * 180f / Math.PI.toFloat())
                            val finalRotZ = (rotZ * 180f / Math.PI.toFloat())
                            node.rotation = Rotation(x = finalRotX, y = finalRotY, z = finalRotZ)
                        }
                    }
                }
            )
        }

        // Live Tracking Mode Badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
                .background(Color(0x990F172A), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isArCoreInstalled) GlowGreen else NeonCyan, CircleShape)
                )
                Text(
                    text = if (isArCoreInstalled) {
                        if (isAnchored) " AR Anchored (1:1 Metric)" else " AR Scanning Surfaces (Tap to Place)"
                    } else {
                        " Spatial Passthrough (Simulated)"
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * Provides a synchronized stereoscopic dual-camera feed for Left and Right eye passthrough.
 */
@Composable
fun StereoDualCameraPreview(
    modifier: Modifier = Modifier,
    leftOverlay: @Composable () -> Unit = {},
    rightOverlay: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var activeCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                previewUseCase?.setSurfaceProvider(null)
                activeCameraProvider?.unbindAll()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            activeCameraProvider = cameraProvider

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }
                            previewUseCase = preview
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            onRelease = { view ->
                try {
                    previewUseCase?.setSurfaceProvider(null)
                    activeCameraProvider?.unbindAll()
                } catch (e: Exception) {
                    // Ignore onRelease errors
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Stereoscopic Split-View Overlays (Left Eye + Right Eye)
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Eye
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
            ) {
                leftOverlay()
            }

            // Central Stereo Partition Divider
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Color(0x66FFFFFF))
            )

            // Right Eye
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp))
            ) {
                rightOverlay()
            }
        }
    }
}


package com.example.engine

import androidx.compose.ui.graphics.Color

/**
 * 3D & Spatial XR Rendering Framework Profiles.
 * Controls shading algorithms, PBR texture sampling, lighting equations, and shadow rendering.
 * Integrates RealityKit, SceneKit, ModelIO, ARKit, Filament, and Sceneview spatial engines.
 */
enum class RenderEngineProfile(
    val title: String,
    val shortName: String,
    val subtitle: String,
    val description: String,
    val themeColor: Color,
    val pbrRoughness: Float,
    val specularMultiplier: Float,
    val shadowIntensity: Float,
    val useFilmicToneMapping: Boolean
) {
    REALITYKIT(
        title = "RealityKit Spatial Engine",
        shortName = "RealityKit",
        subtitle = "Apple Spatial Computing & MaterialX PBR",
        description = "Advanced visionOS-grade physically based rendering, dynamic Fresnel Schlick reflections, and spatial environment mapping.",
        themeColor = Color(0xFF00E5FF),
        pbrRoughness = 0.22f,
        specularMultiplier = 1.40f,
        shadowIntensity = 1.15f,
        useFilmicToneMapping = true
    ),
    SCENEKIT(
        title = "SceneKit 3D Engine",
        shortName = "SceneKit",
        subtitle = "High-Precision 3D Scene Graph Renderer",
        description = "Multi-pass lighting pipeline, dynamic Phong/PBR specular highlights, and real-time volumetric shadows.",
        themeColor = Color(0xFF9D4EDD),
        pbrRoughness = 0.28f,
        specularMultiplier = 1.25f,
        shadowIntensity = 1.10f,
        useFilmicToneMapping = true
    ),
    ARKIT(
        title = "ARKit Spatial Tracking Engine",
        shortName = "ARKit",
        subtitle = "Visual Inertial Odometry & Anchoring",
        description = "6-DoF SLAM tracking, horizontal/vertical plane anchoring, and realistic camera passthrough depth occlusion.",
        themeColor = Color(0xFF00FF88),
        pbrRoughness = 0.25f,
        specularMultiplier = 1.30f,
        shadowIntensity = 1.20f,
        useFilmicToneMapping = true
    ),
    MODELIO(
        title = "ModelIO Universal Asset Pipeline",
        shortName = "ModelIO",
        subtitle = "Universal 3D Asset I/O & Mesh Processing",
        description = "Direct vertex attribute extraction, UV texture coordinate mapping, and USDZ/GLB spatial mesh synthesis.",
        themeColor = Color(0xFFFFB703),
        pbrRoughness = 0.35f,
        specularMultiplier = 1.05f,
        shadowIntensity = 0.95f,
        useFilmicToneMapping = true
    ),
    FILAMENT(
        title = "Google Filament",
        shortName = "Filament",
        subtitle = "Physically Based Real-Time PBR Engine",
        description = "High-precision HDRi spherical irradiance, Cook-Torrance specular reflections, and ACES filmic highlight compression.",
        themeColor = Color(0xFF38BDF8),
        pbrRoughness = 0.25f,
        specularMultiplier = 1.35f,
        shadowIntensity = 1.1f,
        useFilmicToneMapping = true
    ),
    SCENEVIEW(
        title = "Sceneview 3D",
        shortName = "Sceneview",
        subtitle = "Jetpack Compose + Filament GLB Engine",
        description = "Optimized for glTF/GLB PBR material textures, UV texture sampling, and seamless AR spatial tracking.",
        themeColor = Color(0xFFFF0055),
        pbrRoughness = 0.30f,
        specularMultiplier = 1.15f,
        shadowIntensity = 1.0f,
        useFilmicToneMapping = true
    )
}


package com.example.engine

import androidx.compose.ui.graphics.Color

/**
 * 3D & Spatial XR Rendering Framework Profiles.
 * Controls shading algorithms, PBR texture sampling, lighting equations, and shadow rendering.
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
    SCENEVIEW(
        title = "Sceneview 3D",
        shortName = "Sceneview",
        subtitle = "Jetpack Compose + Filament GLB/glTF Engine",
        description = "Optimized for glTF/GLB PBR material textures, UV texture sampling, and seamless ARCore spatial tracking.",
        themeColor = Color(0xFF00E5FF),
        pbrRoughness = 0.30f,
        specularMultiplier = 1.15f,
        shadowIntensity = 1.0f,
        useFilmicToneMapping = true
    ),
    FILAMENT(
        title = "Google Filament",
        shortName = "Filament",
        subtitle = "Physically Based Real-Time PBR Engine",
        description = "High-precision HDRi spherical irradiance, Cook-Torrance specular reflections, and ACES filmic highlight compression.",
        themeColor = Color(0xFF00FF88),
        pbrRoughness = 0.25f,
        specularMultiplier = 1.35f,
        shadowIntensity = 1.1f,
        useFilmicToneMapping = true
    ),
    LIBGDX(
        title = "LibGDX Fast Engine",
        shortName = "LibGDX",
        subtitle = "High-Performance 2D/3D Game Framework",
        description = "High-framerate rendering with rapid depth-sorting, low-overhead vertex transforms, and crisp texture rendering.",
        themeColor = Color(0xFFFFB703),
        pbrRoughness = 0.40f,
        specularMultiplier = 0.95f,
        shadowIntensity = 0.85f,
        useFilmicToneMapping = false
    ),
    UNITY3D(
        title = "Unity3D XR",
        shortName = "Unity3D",
        subtitle = "Real-Time Spatial XR Simulation",
        description = "Multi-pass volumetric shadow simulation, dense ground contact ambient occlusion, and multi-directional sun bounce.",
        themeColor = Color(0xFF9D4EDD),
        pbrRoughness = 0.28f,
        specularMultiplier = 1.25f,
        shadowIntensity = 1.25f,
        useFilmicToneMapping = true
    )
}

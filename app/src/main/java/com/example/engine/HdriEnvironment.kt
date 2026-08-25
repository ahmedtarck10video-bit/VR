package com.example.engine

import com.example.math3d.Vec3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * High Dynamic Range (HDRi) Environment Lighting System.
 * Simulates physical image-based lighting (IBL) including:
 * - Spherical environment map radiance sampling
 * - Hemispherical diffuse irradiance (Zenith, Horizon, Nadir/Ground bounce)
 * - Multi-source HDR direct & fill illuminance
 * - Microfacet specular reflection & Fresnel Schlick approximation
 * - Linear color space pipeline with ACES-like tone mapping
 */
enum class HdriPreset(
    val title: String,
    val description: String,
    val sunDir: Vec3,
    val sunColor: Vec3,
    val zenithColor: Vec3,
    val horizonColor: Vec3,
    val groundColor: Vec3,
    val fillDir: Vec3,
    val fillColor: Vec3,
    val rimDir: Vec3,
    val rimColor: Vec3,
    val exposure: Float = 1.0f
) {
    STUDIO_PRO(
        title = "Studio Pro",
        description = "Neutral daylight with soft dual diffuse panels and clean specular highlights",
        sunDir = Vec3(0.55f, 0.90f, -0.65f).normalize(),
        sunColor = Vec3(1.25f, 1.22f, 1.18f),
        zenithColor = Vec3(0.52f, 0.58f, 0.68f),
        horizonColor = Vec3(0.62f, 0.64f, 0.68f),
        groundColor = Vec3(0.28f, 0.28f, 0.30f),
        fillDir = Vec3(-0.70f, 0.45f, -0.40f).normalize(),
        fillColor = Vec3(0.45f, 0.48f, 0.55f),
        rimDir = Vec3(0.0f, -0.3f, 0.95f).normalize(),
        rimColor = Vec3(0.35f, 0.38f, 0.45f),
        exposure = 1.05f
    ),

    GOLDEN_HOUR(
        title = "Golden Hour",
        description = "Warm low-angle sunset sun with deep blue twilight zenith and golden horizon glow",
        sunDir = Vec3(0.82f, 0.35f, -0.45f).normalize(),
        sunColor = Vec3(1.85f, 1.15f, 0.45f),
        zenithColor = Vec3(0.22f, 0.35f, 0.65f),
        horizonColor = Vec3(0.95f, 0.55f, 0.25f),
        groundColor = Vec3(0.25f, 0.15f, 0.08f),
        fillDir = Vec3(-0.60f, 0.70f, -0.30f).normalize(),
        fillColor = Vec3(0.30f, 0.42f, 0.65f),
        rimDir = Vec3(-0.4f, -0.2f, 0.90f).normalize(),
        rimColor = Vec3(0.65f, 0.35f, 0.15f),
        exposure = 1.12f
    ),

    URBAN_DAYLIGHT(
        title = "Urban Daylight",
        description = "Bright clear sky noon sun with architectural atmospheric bounce",
        sunDir = Vec3(0.35f, 0.95f, -0.30f).normalize(),
        sunColor = Vec3(1.45f, 1.42f, 1.35f),
        zenithColor = Vec3(0.32f, 0.55f, 0.92f),
        horizonColor = Vec3(0.68f, 0.72f, 0.80f),
        groundColor = Vec3(0.25f, 0.24f, 0.23f),
        fillDir = Vec3(-0.65f, 0.50f, -0.55f).normalize(),
        fillColor = Vec3(0.35f, 0.48f, 0.75f),
        rimDir = Vec3(0.1f, -0.5f, 0.85f).normalize(),
        rimColor = Vec3(0.25f, 0.35f, 0.50f),
        exposure = 1.0f
    ),

    FOREST_CANOPY(
        title = "Forest Canopy",
        description = "Natural dappled sunbeams through leaves with rich emerald ground bounce",
        sunDir = Vec3(0.45f, 0.88f, -0.50f).normalize(),
        sunColor = Vec3(1.35f, 1.30f, 0.95f),
        zenithColor = Vec3(0.35f, 0.60f, 0.85f),
        horizonColor = Vec3(0.45f, 0.65f, 0.40f),
        groundColor = Vec3(0.15f, 0.32f, 0.12f),
        fillDir = Vec3(-0.55f, 0.60f, -0.45f).normalize(),
        fillColor = Vec3(0.25f, 0.50f, 0.28f),
        rimDir = Vec3(0.0f, -0.4f, 0.90f).normalize(),
        rimColor = Vec3(0.30f, 0.55f, 0.25f),
        exposure = 1.08f
    ),

    CYBER_NEON(
        title = "Cyber Neon",
        description = "High-contrast electric cyan key with vivid magenta backlight rim highlights",
        sunDir = Vec3(0.65f, 0.75f, -0.50f).normalize(),
        sunColor = Vec3(0.40f, 1.45f, 1.85f),
        zenithColor = Vec3(0.10f, 0.08f, 0.25f),
        horizonColor = Vec3(0.35f, 0.12f, 0.45f),
        groundColor = Vec3(0.05f, 0.04f, 0.08f),
        fillDir = Vec3(-0.75f, 0.40f, -0.45f).normalize(),
        fillColor = Vec3(1.65f, 0.25f, 1.15f),
        rimDir = Vec3(0.0f, -0.2f, 0.95f).normalize(),
        rimColor = Vec3(0.20f, 0.95f, 1.35f),
        exposure = 1.20f
    );

    /**
     * Samples the radiance of the spherical HDRi environment along a given reflection or view ray.
     */
    fun sampleEnvironment(dir: Vec3): Vec3 {
        val normDir = dir.normalize()
        val y = normDir.y.coerceIn(-1f, 1f)

        // Hemispherical gradient interpolation
        val baseRadiance = if (y >= 0f) {
            val t = y.pow(0.75f)
            horizonColor * (1f - t) + zenithColor * t
        } else {
            val t = (-y).pow(0.85f)
            horizonColor * (1f - t) + groundColor * t
        }

        // Direct Sun Glare in the HDR environment map
        val sunDot = max(0f, normDir.dot(sunDir))
        val sunGlare = if (sunDot > 0.88f) {
            val factor = ((sunDot - 0.88f) / 0.12f).pow(6)
            sunColor * (factor * 3.5f)
        } else {
            Vec3(0f, 0f, 0f)
        }

        // Fill Light Glow
        val fillDot = max(0f, normDir.dot(fillDir))
        val fillGlare = if (fillDot > 0.92f) {
            val factor = ((fillDot - 0.92f) / 0.08f).pow(4)
            fillColor * (factor * 1.5f)
        } else {
            Vec3(0f, 0f, 0f)
        }

        return (baseRadiance + sunGlare + fillGlare) * exposure
    }

    /**
     * Computes the total diffuse irradiance at a surface with normal N in Linear space.
     */
    fun computeDiffuseIrradiance(normal: Vec3): Vec3 {
        val n = normal.normalize()

        // 1. Ambient Hemisphere Irradiance
        val hemisphereFactor = (n.y * 0.5f + 0.5f).coerceIn(0f, 1f)
        val ambient = if (hemisphereFactor > 0.5f) {
            val t = (hemisphereFactor - 0.5f) * 2f
            horizonColor * (1f - t) + zenithColor * t
        } else {
            val t = hemisphereFactor * 2f
            groundColor * (1f - t) + horizonColor * t
        }

        // 2. Direct Sun Diffuse (Lambertian)
        val nDotSun = max(0f, n.dot(sunDir))
        val directSun = sunColor * (nDotSun * 0.90f)

        // 3. Sky Fill Diffuse
        val nDotFill = max(0f, n.dot(fillDir))
        val skyFill = fillColor * (nDotFill * 0.40f)

        // 4. Ground Bounce Diffuse
        val groundDir = Vec3(0f, -0.9f, -0.1f).normalize()
        val nDotGround = max(0f, n.dot(groundDir))
        val groundBounce = groundColor * (nDotGround * 0.25f)

        return (ambient * 0.60f + directSun + skyFill + groundBounce) * exposure
    }

    /**
     * Computes physical Specular Reflection & Fresnel using the HDRi environment map.
     */
    fun computeSpecularRadiance(normal: Vec3, viewDir: Vec3, roughness: Float = 0.35f): Vec3 {
        val n = normal.normalize()
        val v = viewDir.normalize()

        // Reflection Vector R = 2(N . V)N - V
        val nDotV = max(0f, n.dot(v))
        val r = (n * (2f * nDotV) - v).normalize()

        // Sample HDRi environment radiance along reflection ray R
        val envReflection = sampleEnvironment(r)

        // Specular highlight from primary sun
        val h = (sunDir + v).normalize()
        val nDotH = max(0f, n.dot(h))
        val shininess = ((1f - roughness).coerceIn(0.05f, 0.98f) * 128f)
        val sunSpecular = sunColor * (nDotH.pow(shininess) * 0.65f)

        // Fresnel Schlick Factor: F0 + (1 - F0) * (1 - cos theta)^5
        val f0 = 0.04f // Dielectric base reflectance (4% for plastics, glass, ceramics)
        val fresnel = f0 + (1f - f0) * (1f - nDotV).pow(5)

        return (envReflection * (fresnel * 1.2f) + sunSpecular) * exposure
    }
}

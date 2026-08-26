package com.example.math3d

import kotlin.math.cos
import kotlin.math.sin

object MeshGenerator {

    fun getDefaultModels(): List<Model3D> {
        return listOf(
            Model3D(
                name = "Spatial Vision Pro [USDZ]",
                description = "Apple Vision MR Headset with curved glass & spatial audio straps",
                triangles = createMixedRealityHeadset()
            ),
            Model3D(
                name = "Cyberpunk Drone [GLB]",
                description = "Quad-rotor autonomous drone with carbon frame & sensors",
                triangles = createDrone()
            ),
            Model3D(
                name = "Companion AI Bot [GLTF]",
                description = "Bipedal holographic companion android with visor display",
                triangles = createRobot()
            ),
            Model3D(
                name = "Spatial Audio Pod [USDZ]",
                description = "Spherical acoustics transducer with orbital magnetic ring",
                triangles = createAudioPod()
            ),
            Model3D(
                name = "Spatial Anchor Prism [GLB]",
                description = "High-precision geometric spatial reference prism for ARKit anchoring",
                triangles = createCube()
            )
        )
    }

    fun createCube(): List<Triangle> {
        val s = 0.9f
        val p = listOf(
            Vec3(-s, -s, -s), Vec3(s, -s, -s), Vec3(s, s, -s), Vec3(-s, s, -s),
            Vec3(-s, -s, s), Vec3(s, -s, s), Vec3(s, s, s), Vec3(-s, s, s)
        )
        return listOf(
            // Front (Cyan Neon)
            quad(p[4], p[5], p[6], p[7], color = 0xFF00E5FF),
            // Back (Deep Purple)
            quad(p[1], p[0], p[3], p[2], color = 0xFF7928CA),
            // Left (Emerald Green)
            quad(p[0], p[4], p[7], p[3], color = 0xFF00FF88),
            // Right (Amber Gold)
            quad(p[5], p[1], p[2], p[6], color = 0xFFFFB703),
            // Top (Titanium White)
            quad(p[7], p[6], p[2], p[3], color = 0xFFE2E8F0),
            // Bottom (Dark Carbon)
            quad(p[0], p[1], p[5], p[4], color = 0xFF1E293B)
        ).flatten()
    }

    fun createRobot(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        val darkChassis = 0xFF1E293B
        val goldArmor = 0xFFFFB703
        val cyanVisor = 0xFF00E5FF
        val emeraldCore = 0xFF00FF88
        val silverLimbs = 0xFF94A3B8

        // Torso
        list.addAll(createBox(Vec3(0f, 0f, 0f), 0.8f, 1.1f, 0.55f, color = darkChassis))
        // Chest Core Arc Reactor (Emerald)
        list.addAll(createBox(Vec3(0f, 0.2f, 0.3f), 0.35f, 0.35f, 0.08f, color = emeraldCore))
        // Head
        list.addAll(createBox(Vec3(0f, 0.95f, 0f), 0.55f, 0.45f, 0.45f, color = darkChassis))
        // Visor glass (Cyan Glow)
        list.addAll(createBox(Vec3(0f, 0.98f, 0.25f), 0.45f, 0.18f, 0.08f, color = cyanVisor))
        // Left Shoulder & Arm
        list.addAll(createBox(Vec3(-0.65f, 0.35f, 0f), 0.25f, 0.25f, 0.25f, color = goldArmor))
        list.addAll(createBox(Vec3(-0.65f, -0.1f, 0f), 0.2f, 0.75f, 0.2f, color = silverLimbs))
        // Right Shoulder & Arm
        list.addAll(createBox(Vec3(0.65f, 0.35f, 0f), 0.25f, 0.25f, 0.25f, color = goldArmor))
        list.addAll(createBox(Vec3(0.65f, -0.1f, 0f), 0.2f, 0.75f, 0.2f, color = silverLimbs))
        // Pelvis
        list.addAll(createBox(Vec3(0f, -0.65f, 0f), 0.6f, 0.2f, 0.45f, color = goldArmor))
        // Left Leg
        list.addAll(createBox(Vec3(-0.3f, -1.25f, 0f), 0.25f, 0.95f, 0.25f, color = silverLimbs))
        // Right Leg
        list.addAll(createBox(Vec3(0.3f, -1.25f, 0f), 0.25f, 0.95f, 0.25f, color = silverLimbs))
        return list
    }

    fun createDrone(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        val carbonBody = 0xFF0F172A
        val cyberCyan = 0xFF00E5FF
        val redSensor = 0xFFFF0055
        val goldPropellers = 0xFFFFD166

        // Central Core
        list.addAll(createBox(Vec3(0f, 0f, 0f), 0.65f, 0.22f, 0.65f, color = carbonBody))
        // Top Dome LiDAR (Red Sensor)
        list.addAll(createCylinder(Vec3(0f, 0.18f, 0f), 0.25f, 0.12f, 10, color = redSensor))
        // Arms
        list.addAll(createBox(Vec3(0.75f, 0.04f, 0.75f), 0.14f, 0.08f, 0.85f, color = cyberCyan))
        list.addAll(createBox(Vec3(-0.75f, 0.04f, 0.75f), 0.14f, 0.08f, 0.85f, color = cyberCyan))
        list.addAll(createBox(Vec3(0.75f, 0.04f, -0.75f), 0.14f, 0.08f, 0.85f, color = cyberCyan))
        list.addAll(createBox(Vec3(-0.75f, 0.04f, -0.75f), 0.14f, 0.08f, 0.85f, color = cyberCyan))
        // Rotors (Gold blades)
        list.addAll(createCylinder(Vec3(1.05f, 0.15f, 1.05f), 0.38f, 0.04f, 8, color = goldPropellers))
        list.addAll(createCylinder(Vec3(-1.05f, 0.15f, 1.05f), 0.38f, 0.04f, 8, color = goldPropellers))
        list.addAll(createCylinder(Vec3(1.05f, 0.15f, -1.05f), 0.38f, 0.04f, 8, color = goldPropellers))
        list.addAll(createCylinder(Vec3(-1.05f, 0.15f, -1.05f), 0.38f, 0.04f, 8, color = goldPropellers))
        return list
    }

    fun createMixedRealityHeadset(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        val visorGlass = 0xFF00E5FF
        val aluminumFrame = 0xFFE2E8F0
        val audioStrap = 0xFFFFB703
        val cameraSensor = 0xFF111827

        // Main Visor curved block
        list.addAll(createBox(Vec3(0f, 0.08f, 0f), 1.5f, 0.65f, 0.75f, color = aluminumFrame))
        // Front Glass Display Panel (Holographic Cyan Glass)
        list.addAll(createBox(Vec3(0f, 0.08f, 0.40f), 1.38f, 0.52f, 0.06f, color = visorGlass))
        // Top sensor pill
        list.addAll(createBox(Vec3(0f, 0.35f, 0.41f), 0.32f, 0.07f, 0.04f, color = cameraSensor))
        // Side Straps (Warm audio strap weave)
        list.addAll(createBox(Vec3(-0.8f, 0.08f, -0.55f), 0.07f, 0.22f, 1.1f, color = audioStrap))
        list.addAll(createBox(Vec3(0.8f, 0.08f, -0.55f), 0.07f, 0.22f, 1.1f, color = audioStrap))
        // Rear battery dial
        list.addAll(createBox(Vec3(0f, 0.08f, -1.1f), 0.55f, 0.28f, 0.18f, color = aluminumFrame))
        return list
    }

    fun createAudioPod(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        val metallicAcoustic = 0xFF7928CA
        val goldOrbital = 0xFFFFD166
        val grillMat = 0xFF1E293B
        val baseSilver = 0xFF94A3B8

        // Center sphere/cylinder
        list.addAll(createCylinder(Vec3(0f, 0f, 0f), 0.6f, 0.7f, 12, color = metallicAcoustic))
        // Outer orbital ring
        list.addAll(createCylinder(Vec3(0f, 0f, 0f), 1.1f, 0.08f, 16, color = goldOrbital))
        // Top speaker grill
        list.addAll(createCylinder(Vec3(0f, 0.4f, 0f), 0.4f, 0.08f, 10, color = grillMat))
        // Bottom acoustic base
        list.addAll(createCylinder(Vec3(0f, -0.4f, 0f), 0.45f, 0.1f, 10, color = baseSilver))
        return list
    }

    private fun createBox(center: Vec3, width: Float, height: Float, depth: Float, color: Long = 0L): List<Triangle> {
        val hw = width / 2f
        val hh = height / 2f
        val hd = depth / 2f
        val p = listOf(
            center + Vec3(-hw, -hh, -hd), center + Vec3(hw, -hh, -hd),
            center + Vec3(hw, hh, -hd), center + Vec3(-hw, hh, -hd),
            center + Vec3(-hw, -hh, hd), center + Vec3(hw, -hh, hd),
            center + Vec3(hw, hh, hd), center + Vec3(-hw, hh, hd)
        )
        return listOf(
            quad(p[4], p[5], p[6], p[7], color),
            quad(p[1], p[0], p[3], p[2], color),
            quad(p[0], p[4], p[7], p[3], color),
            quad(p[5], p[1], p[2], p[6], color),
            quad(p[7], p[6], p[2], p[3], color),
            quad(p[0], p[1], p[5], p[4], color)
        ).flatten()
    }

    private fun createCylinder(center: Vec3, radius: Float, height: Float, segments: Int, color: Long = 0L): List<Triangle> {
        val list = mutableListOf<Triangle>()
        val halfH = height / 2f
        val step = (2 * Math.PI / segments).toFloat()

        for (i in 0 until segments) {
            val a1 = i * step
            val a2 = (i + 1) * step
            val x1 = radius * cos(a1)
            val z1 = radius * sin(a1)
            val x2 = radius * cos(a2)
            val z2 = radius * sin(a2)

            val b1 = center + Vec3(x1, -halfH, z1)
            val b2 = center + Vec3(x2, -halfH, z2)
            val t1 = center + Vec3(x1, halfH, z1)
            val t2 = center + Vec3(x2, halfH, z2)

            list.addAll(quad(b1, b2, t2, t1, color))
        }
        return list
    }

    private fun quad(v1: Vec3, v2: Vec3, v3: Vec3, v4: Vec3, color: Long = 0L): List<Triangle> {
        val n1 = (v2 - v1).cross(v3 - v1).normalize()
        val n2 = (v3 - v1).cross(v4 - v1).normalize()
        return listOf(
            Triangle(v1, v2, v3, n1, color = color),
            Triangle(v1, v3, v4, n2, color = color)
        )
    }
}

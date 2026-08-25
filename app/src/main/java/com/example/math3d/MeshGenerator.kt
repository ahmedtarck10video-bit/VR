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
                name = "Hologram Cube [OBJ]",
                description = "High-precision geometric spatial reference cube",
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
            // Front
            quad(p[4], p[5], p[6], p[7]),
            // Back
            quad(p[1], p[0], p[3], p[2]),
            // Left
            quad(p[0], p[4], p[7], p[3]),
            // Right
            quad(p[5], p[1], p[2], p[6]),
            // Top
            quad(p[7], p[6], p[2], p[3]),
            // Bottom
            quad(p[0], p[1], p[5], p[4])
        ).flatten()
    }

    fun createRobot(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        // Torso
        list.addAll(createBox(Vec3(0f, 0f, 0f), 0.8f, 1.1f, 0.55f))
        // Chest Core Arc Reactor
        list.addAll(createBox(Vec3(0f, 0.2f, 0.3f), 0.35f, 0.35f, 0.08f))
        // Head / Visor
        list.addAll(createBox(Vec3(0f, 0.95f, 0f), 0.55f, 0.45f, 0.45f))
        // Visor glass
        list.addAll(createBox(Vec3(0f, 0.98f, 0.25f), 0.45f, 0.18f, 0.08f))
        // Left Shoulder & Arm
        list.addAll(createBox(Vec3(-0.65f, 0.35f, 0f), 0.25f, 0.25f, 0.25f))
        list.addAll(createBox(Vec3(-0.65f, -0.1f, 0f), 0.2f, 0.75f, 0.2f))
        // Right Shoulder & Arm
        list.addAll(createBox(Vec3(0.65f, 0.35f, 0f), 0.25f, 0.25f, 0.25f))
        list.addAll(createBox(Vec3(0.65f, -0.1f, 0f), 0.2f, 0.75f, 0.2f))
        // Pelvis
        list.addAll(createBox(Vec3(0f, -0.65f, 0f), 0.6f, 0.2f, 0.45f))
        // Left Leg
        list.addAll(createBox(Vec3(-0.3f, -1.25f, 0f), 0.25f, 0.95f, 0.25f))
        // Right Leg
        list.addAll(createBox(Vec3(0.3f, -1.25f, 0f), 0.25f, 0.95f, 0.25f))
        return list
    }

    fun createDrone(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        // Central Core
        list.addAll(createBox(Vec3(0f, 0f, 0f), 0.65f, 0.22f, 0.65f))
        // Top Dome LiDAR
        list.addAll(createCylinder(Vec3(0f, 0.18f, 0f), 0.25f, 0.12f, 10))
        // Arms
        list.addAll(createBox(Vec3(0.75f, 0.04f, 0.75f), 0.14f, 0.08f, 0.85f))
        list.addAll(createBox(Vec3(-0.75f, 0.04f, 0.75f), 0.14f, 0.08f, 0.85f))
        list.addAll(createBox(Vec3(0.75f, 0.04f, -0.75f), 0.14f, 0.08f, 0.85f))
        list.addAll(createBox(Vec3(-0.75f, 0.04f, -0.75f), 0.14f, 0.08f, 0.85f))
        // Rotors
        list.addAll(createCylinder(Vec3(1.05f, 0.15f, 1.05f), 0.38f, 0.04f, 8))
        list.addAll(createCylinder(Vec3(-1.05f, 0.15f, 1.05f), 0.38f, 0.04f, 8))
        list.addAll(createCylinder(Vec3(1.05f, 0.15f, -1.05f), 0.38f, 0.04f, 8))
        list.addAll(createCylinder(Vec3(-1.05f, 0.15f, -1.05f), 0.38f, 0.04f, 8))
        return list
    }

    fun createMixedRealityHeadset(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        // Main Visor curved block
        list.addAll(createBox(Vec3(0f, 0.08f, 0f), 1.5f, 0.65f, 0.75f))
        // Front Glass Display Panel
        list.addAll(createBox(Vec3(0f, 0.08f, 0.40f), 1.38f, 0.52f, 0.06f))
        // Top sensor pill
        list.addAll(createBox(Vec3(0f, 0.35f, 0.41f), 0.32f, 0.07f, 0.04f))
        // Side Straps
        list.addAll(createBox(Vec3(-0.8f, 0.08f, -0.55f), 0.07f, 0.22f, 1.1f))
        list.addAll(createBox(Vec3(0.8f, 0.08f, -0.55f), 0.07f, 0.22f, 1.1f))
        // Rear battery dial
        list.addAll(createBox(Vec3(0f, 0.08f, -1.1f), 0.55f, 0.28f, 0.18f))
        return list
    }

    fun createAudioPod(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        // Center sphere/cylinder
        list.addAll(createCylinder(Vec3(0f, 0f, 0f), 0.6f, 0.7f, 12))
        // Outer orbital ring
        list.addAll(createCylinder(Vec3(0f, 0f, 0f), 1.1f, 0.08f, 16))
        // Top speaker grill
        list.addAll(createCylinder(Vec3(0f, 0.4f, 0f), 0.4f, 0.08f, 10))
        // Bottom acoustic base
        list.addAll(createCylinder(Vec3(0f, -0.4f, 0f), 0.45f, 0.1f, 10))
        return list
    }

    private fun createBox(center: Vec3, width: Float, height: Float, depth: Float): List<Triangle> {
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
            quad(p[4], p[5], p[6], p[7]),
            quad(p[1], p[0], p[3], p[2]),
            quad(p[0], p[4], p[7], p[3]),
            quad(p[5], p[1], p[2], p[6]),
            quad(p[7], p[6], p[2], p[3]),
            quad(p[0], p[1], p[5], p[4])
        ).flatten()
    }

    private fun createCylinder(center: Vec3, radius: Float, height: Float, segments: Int): List<Triangle> {
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

            list.addAll(quad(b1, b2, t2, t1))
        }
        return list
    }

    private fun quad(v1: Vec3, v2: Vec3, v3: Vec3, v4: Vec3): List<Triangle> {
        val n1 = (v2 - v1).cross(v3 - v1).normalize()
        val n2 = (v3 - v1).cross(v4 - v1).normalize()
        return listOf(
            Triangle(v1, v2, v3, n1),
            Triangle(v1, v3, v4, n2)
        )
    }
}

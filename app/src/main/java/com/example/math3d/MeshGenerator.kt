package com.example.math3d

import kotlin.math.cos
import kotlin.math.sin

object MeshGenerator {

    fun createCube(): List<Triangle> {
        val s = 1.0f
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
        // Body
        list.addAll(createBox(Vec3(0f, 0f, 0f), 0.8f, 1.2f, 0.6f))
        // Head / Visor
        list.addAll(createBox(Vec3(0f, 1.0f, 0f), 0.6f, 0.5f, 0.5f))
        // Visor glass
        list.addAll(createBox(Vec3(0f, 1.05f, 0.28f), 0.45f, 0.2f, 0.08f))
        // Left Arm
        list.addAll(createBox(Vec3(-0.7f, 0.1f, 0f), 0.25f, 0.9f, 0.25f))
        // Right Arm
        list.addAll(createBox(Vec3(0.7f, 0.1f, 0f), 0.25f, 0.9f, 0.25f))
        // Left Leg
        list.addAll(createBox(Vec3(-0.35f, -1.1f, 0f), 0.3f, 0.9f, 0.3f))
        // Right Leg
        list.addAll(createBox(Vec3(0.35f, -1.1f, 0f), 0.3f, 0.9f, 0.3f))
        return list
    }

    fun createDrone(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        // Central Core
        list.addAll(createBox(Vec3(0f, 0f, 0f), 0.7f, 0.25f, 0.7f))
        // Arms
        list.addAll(createBox(Vec3(0.8f, 0.05f, 0.8f), 0.15f, 0.1f, 0.9f))
        list.addAll(createBox(Vec3(-0.8f, 0.05f, 0.8f), 0.15f, 0.1f, 0.9f))
        list.addAll(createBox(Vec3(0.8f, 0.05f, -0.8f), 0.15f, 0.1f, 0.9f))
        list.addAll(createBox(Vec3(-0.8f, 0.05f, -0.8f), 0.15f, 0.1f, 0.9f))
        // Rotors
        list.addAll(createCylinder(Vec3(1.1f, 0.2f, 1.1f), 0.4f, 0.05f, 8))
        list.addAll(createCylinder(Vec3(-1.1f, 0.2f, 1.1f), 0.4f, 0.05f, 8))
        list.addAll(createCylinder(Vec3(1.1f, 0.2f, -1.1f), 0.4f, 0.05f, 8))
        list.addAll(createCylinder(Vec3(-1.1f, 0.2f, -1.1f), 0.4f, 0.05f, 8))
        return list
    }

    fun createMixedRealityHeadset(): List<Triangle> {
        val list = mutableListOf<Triangle>()
        // Main Visor curved block
        list.addAll(createBox(Vec3(0f, 0.1f, 0f), 1.6f, 0.7f, 0.8f))
        // Front Glass Display Panel
        list.addAll(createBox(Vec3(0f, 0.1f, 0.42f), 1.45f, 0.55f, 0.05f))
        // Top sensor pill
        list.addAll(createBox(Vec3(0f, 0.38f, 0.43f), 0.35f, 0.08f, 0.04f))
        // Side Straps
        list.addAll(createBox(Vec3(-0.85f, 0.1f, -0.6f), 0.08f, 0.25f, 1.2f))
        list.addAll(createBox(Vec3(0.85f, 0.1f, -0.6f), 0.08f, 0.25f, 1.2f))
        // Rear battery dial
        list.addAll(createBox(Vec3(0f, 0.1f, -1.2f), 0.6f, 0.3f, 0.2f))
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

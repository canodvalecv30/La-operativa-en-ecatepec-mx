package com.example.game.gl

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

data class MeshData(
    val vertexBuffer: FloatBuffer,
    val normalBuffer: FloatBuffer,
    val colorBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int
)

object MeshBuilder {

    fun createBox(
        width: Float, height: Float, depth: Float,
        r: Float, g: Float, b: Float, a: Float = 1.0f
    ): MeshData {
        val hw = width / 2f
        val hh = height / 2f
        val hd = depth / 2f

        // 6 faces * 4 vertices = 24 vertices
        val vertices = floatArrayOf(
            // Front face (+Z)
            -hw, -hh,  hd,   hw, -hh,  hd,   hw,  hh,  hd,  -hw,  hh,  hd,
            // Back face (-Z)
             hw, -hh, -hd,  -hw, -hh, -hd,  -hw,  hh, -hd,   hw,  hh, -hd,
            // Top face (+Y)
            -hw,  hh,  hd,   hw,  hh,  hd,   hw,  hh, -hd,  -hw,  hh, -hd,
            // Bottom face (-Y)
            -hw, -hh, -hd,   hw, -hh, -hd,   hw, -hh,  hd,  -hw, -hh,  hd,
            // Right face (+X)
             hw, -hh,  hd,   hw, -hh, -hd,   hw,  hh, -hd,   hw,  hh,  hd,
            // Left face (-X)
            -hw, -hh, -hd,  -hw, -hh,  hd,  -hw,  hh,  hd,  -hw,  hh, -hd
        )

        val normals = floatArrayOf(
            // Front
            0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,   0f, 0f, 1f,
            // Back
            0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
            // Top
            0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,   0f, 1f, 0f,
            // Bottom
            0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
            // Right
            1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,   1f, 0f, 0f,
            // Left
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f
        )

        val colors = FloatArray(24 * 4)
        for (i in 0 until 24) {
            colors[i * 4 + 0] = r
            colors[i * 4 + 1] = g
            colors[i * 4 + 2] = b
            colors[i * 4 + 3] = a
        }

        val indices = ShortArray(36)
        var idx = 0
        for (f in 0 until 6) {
            val base = (f * 4).toShort()
            indices[idx++] = base
            indices[idx++] = (base + 1).toShort()
            indices[idx++] = (base + 2).toShort()
            indices[idx++] = base
            indices[idx++] = (base + 2).toShort()
            indices[idx++] = (base + 3).toShort()
        }

        return buildMesh(vertices, normals, colors, indices)
    }

    fun createCylinder(
        radius: Float, height: Float, segments: Int = 16,
        r: Float, g: Float, b: Float, a: Float = 1.0f
    ): MeshData {
        val halfH = height / 2f
        val vertexCount = (segments + 1) * 2 + 2 // 2 centers + rim vertices
        val vertices = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val colors = ArrayList<Float>()
        val indices = ArrayList<Short>()

        // Side faces
        for (i in 0..segments) {
            val angle = (i * 2.0 * Math.PI / segments).toFloat()
            val x = cos(angle.toDouble()).toFloat() * radius
            val z = sin(angle.toDouble()).toFloat() * radius

            // Bottom rim
            vertices.add(x); vertices.add(-halfH); vertices.add(z)
            normals.add(x / radius); normals.add(0f); normals.add(z / radius)
            colors.add(r); colors.add(g); colors.add(b); colors.add(a)

            // Top rim
            vertices.add(x); vertices.add(halfH); vertices.add(z)
            normals.add(x / radius); normals.add(0f); normals.add(z / radius)
            colors.add(r); colors.add(g); colors.add(b); colors.add(a)
        }

        for (i in 0 until segments) {
            val b1 = (i * 2).toShort()
            val t1 = (i * 2 + 1).toShort()
            val b2 = ((i + 1) * 2).toShort()
            val t2 = ((i + 1) * 2 + 1).toShort()

            indices.add(b1); indices.add(t1); indices.add(b2)
            indices.add(t1); indices.add(t2); indices.add(b2)
        }

        // Top and bottom caps
        val topCenterIdx = (vertices.size / 3).toShort()
        vertices.add(0f); vertices.add(halfH); vertices.add(0f)
        normals.add(0f); normals.add(1f); normals.add(0f)
        colors.add(r * 0.8f); colors.add(g * 0.8f); colors.add(b * 0.8f); colors.add(a)

        val bottomCenterIdx = (vertices.size / 3).toShort()
        vertices.add(0f); vertices.add(-halfH); vertices.add(0f)
        normals.add(0f); normals.add(-1f); normals.add(0f)
        colors.add(r * 0.8f); colors.add(g * 0.8f); colors.add(b * 0.8f); colors.add(a)

        for (i in 0 until segments) {
            val t1 = (i * 2 + 1).toShort()
            val t2 = ((i + 1) * 2 + 1).toShort()
            indices.add(topCenterIdx); indices.add(t1); indices.add(t2)

            val b1 = (i * 2).toShort()
            val b2 = ((i + 1) * 2).toShort()
            indices.add(bottomCenterIdx); indices.add(b2); indices.add(b1)
        }

        return buildMesh(
            vertices.toFloatArray(),
            normals.toFloatArray(),
            colors.toFloatArray(),
            indices.toShortArray()
        )
    }

    fun createRamp(
        width: Float, length: Float, height: Float,
        r: Float, g: Float, b: Float
    ): MeshData {
        val hw = width / 2f
        // 5 vertices for wedge
        // 0: (-hw, 0, 0), 1: (hw, 0, 0), 2: (hw, height, length), 3: (-hw, height, length)
        // 4: (-hw, 0, length), 5: (hw, 0, length)
        val vertices = floatArrayOf(
            // Slope quad
            -hw, 0f, 0f,
             hw, 0f, 0f,
             hw, height, length,
            -hw, height, length,
            // Back vertical quad
            -hw, 0f, length,
             hw, 0f, length,
             hw, height, length,
            -hw, height, length,
            // Left triangle
            -hw, 0f, 0f,
            -hw, height, length,
            -hw, 0f, length,
            // Right triangle
             hw, 0f, 0f,
             hw, 0f, length,
             hw, height, length
        )

        val normals = floatArrayOf(
            // Slope (angled up and back)
            0f, length / height, -1f,  0f, length / height, -1f,
            0f, length / height, -1f,  0f, length / height, -1f,
            // Back (+Z)
            0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
            // Left (-X)
            -1f, 0f, 0f, -1f, 0f, 0f, -1f, 0f, 0f,
            // Right (+X)
             1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f
        )

        val colors = FloatArray(14 * 4)
        for (i in 0 until 14) {
            colors[i * 4 + 0] = r
            colors[i * 4 + 1] = g
            colors[i * 4 + 2] = b
            colors[i * 4 + 3] = 1.0f
        }

        val indices = shortArrayOf(
            0, 1, 2,  0, 2, 3,       // Slope
            4, 5, 6,  4, 6, 7,       // Back
            8, 9, 10,                // Left side
            11, 12, 13               // Right side
        )

        return buildMesh(vertices, normals, colors, indices)
    }

    private fun buildMesh(
        vertices: FloatArray,
        normals: FloatArray,
        colors: FloatArray,
        indices: ShortArray
    ): MeshData {
        val vb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(vertices).position(0)

        val nb = ByteBuffer.allocateDirect(normals.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        nb.put(normals).position(0)

        val cb = ByteBuffer.allocateDirect(colors.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        cb.put(colors).position(0)

        val ib = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(indices).position(0)

        return MeshData(vb, nb, cb, ib, indices.size)
    }
}

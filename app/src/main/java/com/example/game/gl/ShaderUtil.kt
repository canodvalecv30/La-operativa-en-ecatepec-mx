package com.example.game.gl

import android.opengl.GLES20
import android.util.Log

object ShaderUtil {
    private const val TAG = "ShaderUtil"

    const val VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform vec3 uLightPos;
        
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;
        
        varying vec4 vColor;
        varying vec3 vNormal;
        varying vec3 vFragPos;
        varying float vFogDist;
        
        void main() {
            vec4 worldPos = uModelMatrix * aPosition;
            vFragPos = vec3(worldPos);
            vNormal = mat3(uModelMatrix) * aNormal;
            vColor = aColor;
            
            gl_Position = uMVPMatrix * aPosition;
            vFogDist = gl_Position.z;
        }
    """

    const val FRAGMENT_SHADER = """
        precision mediump float;
        
        uniform vec3 uLightPos;
        uniform vec3 uViewPos;
        uniform vec4 uOverrideColor; // If w > 0, overrides aColor
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform vec3 uFogColor;
        
        varying vec4 vColor;
        varying vec3 vNormal;
        varying vec3 vFragPos;
        varying float vFogDist;
        
        void main() {
            vec3 norm = normalize(vNormal);
            vec3 lightDir = normalize(uLightPos - vFragPos);
            
            // Ambient
            float ambientStrength = 0.45;
            vec3 ambient = ambientStrength * vec3(1.0, 1.0, 1.0);
            
            // Diffuse
            float diff = max(dot(norm, lightDir), 0.0);
            vec3 diffuse = diff * vec3(1.0, 0.95, 0.9);
            
            // Specular
            float specularStrength = 0.35;
            vec3 viewDir = normalize(uViewPos - vFragPos);
            vec3 reflectDir = reflect(-lightDir, norm);
            float spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
            vec3 specular = specularStrength * spec * vec3(1.0, 1.0, 1.0);
            
            vec4 baseColor = (uOverrideColor.a > 0.0) ? uOverrideColor : vColor;
            vec3 result = (ambient + diffuse + specular) * baseColor.rgb;
            
            // Distance fog
            float fogFactor = clamp((uFogEnd - vFogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
            vec3 finalColor = mix(uFogColor, result, fogFactor);
            
            gl_FragColor = vec4(finalColor, baseColor.a);
        }
    """

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) return 0

        var program = GLES20.glCreateProgram()
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: " + GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                program = 0
            }
        }
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader != 0) {
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader $type: " + GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
                return 0
            }
        }
        return shader
    }
}

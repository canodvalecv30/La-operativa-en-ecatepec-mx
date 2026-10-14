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
            vNormal = normalize(mat3(uModelMatrix) * aNormal);
            vColor = aColor;
            
            gl_Position = uMVPMatrix * aPosition;
            vFogDist = gl_Position.z;
        }
    """

    const val FRAGMENT_SHADER = """
        precision mediump float;
        
        uniform vec3 uLightPos;
        uniform vec3 uViewPos;
        uniform vec4 uOverrideColor; // If a > 0, overrides aColor
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform vec3 uFogColor;
        
        // Headlight projection uniforms
        uniform vec3 uHeadlightPos;
        uniform vec3 uHeadlightDir;
        uniform int uEnableHeadlight;
        
        varying vec4 vColor;
        varying vec3 vNormal;
        varying vec3 vFragPos;
        varying float vFogDist;
        
        void main() {
            vec3 norm = normalize(vNormal);
            vec3 lightDir = normalize(uLightPos - vFragPos);
            vec3 viewDir = normalize(uViewPos - vFragPos);
            
            // 1. Two-tone hemispheric atmospheric ambient lighting (Sky / Ground bounce)
            float upFactor = clamp(norm.y * 0.5 + 0.5, 0.0, 1.0);
            vec3 skyAmbient = vec3(0.58, 0.64, 0.78);
            vec3 groundAmbient = vec3(0.24, 0.25, 0.28);
            vec3 ambient = mix(groundAmbient, skyAmbient, upFactor);
            
            // 2. Diffuse Sun Key Light (Soft wrap for natural surface curvature)
            float NdotL = dot(norm, lightDir);
            float diff = max(NdotL * 0.8 + 0.2, 0.0);
            vec3 sunColor = vec3(1.0, 0.97, 0.91);
            vec3 diffuse = diff * sunColor * 0.82;
            
            // 3. Specular Metallic & Clearcoat (Blinn-Phong Half-Vector)
            vec3 halfDir = normalize(lightDir + viewDir);
            float NdotH = max(dot(norm, halfDir), 0.0);
            float specPower = 44.0;
            float specular = pow(NdotH, specPower);
            
            // Fresnel rim reflection on curved automotive and visor surfaces
            float NdotV = max(dot(norm, viewDir), 0.0);
            float fresnel = pow(1.0 - NdotV, 3.5) * 0.45;
            vec3 specularTotal = (specular * 0.65 + fresnel) * vec3(1.0, 0.98, 0.95);
            
            // 4. Realistic Road Asphalt Sheen (Shallow view angle reflections)
            if (norm.y > 0.85) {
                float roadSheen = pow(1.0 - NdotV, 2.5) * 0.28;
                specularTotal += vec3(0.85, 0.92, 1.0) * roadSheen;
            }
            
            // 5. Dual-Cone Xenon Headlight Spotlights on Road and Barriers
            vec3 headlightColor = vec3(0.0);
            if (uEnableHeadlight > 0) {
                // Left and right projector positions
                vec3 headDirNorm = normalize(uHeadlightDir);
                vec3 rightVec = normalize(cross(headDirNorm, vec3(0.0, 1.0, 0.0))) * 0.85;
                
                // Beam 1: Left projector
                vec3 toFragL = vFragPos - (uHeadlightPos - rightVec);
                float distL = length(toFragL);
                if (distL > 0.6 && distL < 50.0) {
                    float spotL = dot(normalize(toFragL), headDirNorm);
                    if (spotL > 0.84) {
                        float intensityL = smoothstep(0.84, 0.97, spotL);
                        float attL = clamp(1.0 - (distL / 50.0), 0.0, 1.0);
                        headlightColor += vec3(0.70, 0.88, 1.0) * (intensityL * attL * 0.9);
                    }
                }
                
                // Beam 2: Right projector
                vec3 toFragR = vFragPos - (uHeadlightPos + rightVec);
                float distR = length(toFragR);
                if (distR > 0.6 && distR < 50.0) {
                    float spotR = dot(normalize(toFragR), headDirNorm);
                    if (spotR > 0.84) {
                        float intensityR = smoothstep(0.84, 0.97, spotR);
                        float attR = clamp(1.0 - (distR / 50.0), 0.0, 1.0);
                        headlightColor += vec3(0.70, 0.88, 1.0) * (intensityR * attR * 0.9);
                    }
                }
            }
            
            vec4 baseColor = (uOverrideColor.a > 0.0) ? uOverrideColor : vColor;
            
            // 6. Emissive boost for brake lights, nitro flames, streetlights and neon visors
            float luminance = dot(baseColor.rgb, vec3(0.299, 0.587, 0.114));
            float emissiveFactor = (luminance > 0.82) ? 0.35 : 0.0;
            
            // Combine lighting passes
            vec3 lighting = ambient + diffuse + headlightColor + vec3(emissiveFactor);
            vec3 result = (lighting * baseColor.rgb) + (specularTotal * (baseColor.a > 0.5 ? 1.0 : 0.0));
            
            // 7. Realistic atmospheric quadratic distance fog
            float fogFactor = clamp((uFogEnd - vFogDist) / (uFogEnd - uFogStart), 0.0, 1.0);
            fogFactor = fogFactor * fogFactor; // Smooth non-linear fog falloff
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

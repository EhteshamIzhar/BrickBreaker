package com.example.ehte6848.brickbreaker;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by ehte6848 on 20-07-2017.
 */

public class Util {
    private static final String TAG = BreakoutActivity.TAG;
    
    public static int createImageTexture(ByteBuffer data, int width, int height, int format) {
        int[] textureHandles = new int[1];
        int textureHandle;

        GLES20.glGenTextures(1, textureHandles, 0);
        textureHandle = textureHandles[0];
        Util.checkGlError("glGenTextures");

        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        Util.checkGlError("loadImageTexture");

        // Load the data from the buffer into the texture handle.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, /*level*/ 0, format,
                width, height, /*border*/ 0, format, GLES20.GL_UNSIGNED_BYTE, data);
        Util.checkGlError("loadImageTexture");

        return textureHandle;
    }

    public static int loadShader(int type, String shaderCode) {
        int shaderHandle = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shaderHandle, shaderCode);
        GLES20.glCompileShader(shaderHandle);

        // Check for failure.
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] != GLES20.GL_TRUE) {
            // Extract the detailed failure message.
            String msg = GLES20.glGetShaderInfoLog(shaderHandle);
            GLES20.glDeleteProgram(shaderHandle);
            Log.e(TAG, "glCompileShader: " + msg);
            throw new RuntimeException("glCompileShader failed");
        }

        return shaderHandle;
    }

    public static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        // Load the shaders.
        int vertexShader =
                Util.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader =
                Util.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Build the program.
        int programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(programHandle, vertexShader);
        GLES20.glAttachShader(programHandle, fragmentShader);
        GLES20.glLinkProgram(programHandle);

        // Check for failure.
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            // Extract the detailed failure message.
            String msg = GLES20.glGetProgramInfoLog(programHandle);
            GLES20.glDeleteProgram(programHandle);
            Log.e(TAG, "glLinkProgram: " + msg);
            throw new RuntimeException("glLinkProgram failed");
        }

        return programHandle;
    }


    public static void checkGlError(String msg) {
        int error, lastError = GLES20.GL_NO_ERROR;

        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, msg + ": glError " + error);
            lastError = error;
        }
        if (lastError != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(msg + ": glError " + lastError);
        }
    }
}
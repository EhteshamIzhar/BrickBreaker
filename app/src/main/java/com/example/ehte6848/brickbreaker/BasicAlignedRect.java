package com.example.ehte6848.brickbreaker;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.FloatBuffer;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class BasicAlignedRect extends BaseRect {
    private static final String TAG = BreakoutActivity.TAG;


    static final String VERTEX_SHADER_CODE =
            "uniform mat4 u_mvpMatrix;" +
                    "attribute vec4 a_position;" +

                    "void main() {" +
                    "  gl_Position = u_mvpMatrix * a_position;" +
                    "}";

    static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;" +
                    "uniform vec4 u_color;" +

                    "void main() {" +
                    "  gl_FragColor = u_color;" +
                    "}";

    // Reference to vertex data.
    static FloatBuffer sVertexBuffer = getVertexArray();

    // Handles to the GL program and various components of it.
    static int sProgramHandle = -1;
    static int sColorHandle = -1;
    static int sPositionHandle = -1;
    static int sMVPMatrixHandle = -1;

    // RGBA color vector.
    float[] mColor = new float[4];

    // Sanity check on draw prep.
    private static boolean sDrawPrepared;


    static float[] sTempMVP = new float[16];



    public static void createProgram() {
        sProgramHandle = Util.createProgram(VERTEX_SHADER_CODE,
                FRAGMENT_SHADER_CODE);
        Log.d(TAG, "Created program " + sProgramHandle);


        sPositionHandle = GLES20.glGetAttribLocation(sProgramHandle, "a_position");
        Util.checkGlError("glGetAttribLocation");


        sColorHandle = GLES20.glGetUniformLocation(sProgramHandle, "u_color");
        Util.checkGlError("glGetUniformLocation");


        sMVPMatrixHandle = GLES20.glGetUniformLocation(sProgramHandle, "u_mvpMatrix");
        Util.checkGlError("glGetUniformLocation");
    }


    public void setColor(float r, float g, float b) {
        Util.checkGlError("setColor start");
        mColor[0] = r;
        mColor[1] = g;
        mColor[2] = b;
        mColor[3] = 1.0f;
    }


    public float[] getColor() {

        return mColor;
    }


    public static void prepareToDraw() {

        GLES20.glUseProgram(sProgramHandle);
        Util.checkGlError("glUseProgram");

        GLES20.glEnableVertexAttribArray(sPositionHandle);
        Util.checkGlError("glEnableVertexAttribArray");

        GLES20.glVertexAttribPointer(sPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, VERTEX_STRIDE, sVertexBuffer);
        Util.checkGlError("glVertexAttribPointer");

        sDrawPrepared = true;
    }


    public static void finishedDrawing() {
        sDrawPrepared = false;

        // Disable vertex array and program.  Not strictly necessary.
        GLES20.glDisableVertexAttribArray(sPositionHandle);
        GLES20.glUseProgram(0);
    }


    public void draw() {
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("draw start");
        if (!sDrawPrepared) {
            throw new RuntimeException("not prepared");
        }

        float[] mvp = sTempMVP;     // scratch storage
        Matrix.multiplyMM(mvp, 0, GameSurfaceRenderer.mProjectionMatrix, 0, mModelView, 0);

        GLES20.glUniformMatrix4fv(sMVPMatrixHandle, 1, false, mvp, 0);
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("glUniformMatrix4fv");

        GLES20.glUniform4fv(sColorHandle, 1, mColor, 0);
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("glUniform4fv ");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT);
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("glDrawArrays");
    }
}

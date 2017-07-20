package com.example.ehte6848.brickbreaker;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.ConditionVariable;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class GameSurfaceRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = BreakoutActivity.TAG;
    public static final boolean EXTRA_CHECK = true;         // enable additional assertions


    static final float mProjectionMatrix[] = new float[16];


    private int mViewportWidth, mViewportHeight;
    private int mViewportXoff, mViewportYoff;

    private GameSurfaceView mSurfaceView;
    private GameState mGameState;
    private TextResources.Configuration mTextConfig;



    public GameSurfaceRenderer(GameState gameState, GameSurfaceView surfaceView,
                               TextResources.Configuration textConfig) {
        mSurfaceView = surfaceView;
        mGameState = gameState;
        mTextConfig = textConfig;
    }


    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        if (EXTRA_CHECK) Util.checkGlError("onSurfaceCreated start");

        // Generate programs and data.
        BasicAlignedRect.createProgram();
        TexturedAlignedRect.createProgram();

        // Allocate objects associated with the various graphical elements.
        GameState gameState = mGameState;
        gameState.setTextResources(new TextResources(mTextConfig));
        gameState.allocBorders();
        gameState.allocBricks();
        gameState.allocPaddle();
        gameState.allocBall();
        gameState.allocScore();
        gameState.allocMessages();
        gameState.allocDebugStuff();

        // Restore game state from static storage.
        gameState.restore();

        // Set the background color.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Disable depth testing -- we're 2D only.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Don't need backface culling.  (If you're feeling pedantic, you can turn it on to
        // make sure we're defining our shapes correctly.)
        if (EXTRA_CHECK) {
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        } else {
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }

        if (EXTRA_CHECK) Util.checkGlError("onSurfaceCreated end");
    }


    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {


        if (EXTRA_CHECK) Util.checkGlError("onSurfaceChanged start");

        float arenaRatio = GameState.ARENA_HEIGHT / GameState.ARENA_WIDTH;
        int x, y, viewWidth, viewHeight;

        if (height > (int) (width * arenaRatio)) {
            // limited by narrow width; restrict height
            viewWidth = width;
            viewHeight = (int) (width * arenaRatio);
        } else {
            // limited by short height; restrict width
            viewHeight = height;
            viewWidth = (int) (height / arenaRatio);
        }
        x = (width - viewWidth) / 2;
        y = (height - viewHeight) / 2;

        Log.d(TAG, "onSurfaceChanged w=" + width + " h=" + height);
        Log.d(TAG, " --> x=" + x + " y=" + y + " gw=" + viewWidth + " gh=" + viewHeight);

        GLES20.glViewport(x, y, viewWidth, viewHeight);

        mViewportWidth = viewWidth;
        mViewportHeight = viewHeight;
        mViewportXoff = x;
        mViewportYoff = y;


        Matrix.orthoM(mProjectionMatrix, 0,  0, GameState.ARENA_WIDTH,
                0, GameState.ARENA_HEIGHT,  -1, 1);

        // Nudge game state after the surface change.
        mGameState.surfaceChanged();

        if (EXTRA_CHECK) Util.checkGlError("onSurfaceChanged end");
    }


    @Override
    public void onDrawFrame(GL10 unused) {
        GameState gameState = mGameState;

        gameState.calculateNextFrame();

        // Simulate slow game state update, to see impact on animation.
//        try { Thread.sleep(33); }
//        catch (InterruptedException ie) {}

        if (EXTRA_CHECK) Util.checkGlError("onDrawFrame start");

        // Clear entire screen to background color.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Draw the various elements.  These are all BasicAlignedRect.
        BasicAlignedRect.prepareToDraw();
        gameState.drawBorders();
        gameState.drawBricks();
        gameState.drawPaddle();
        BasicAlignedRect.finishedDrawing();



        // Enable alpha blending.
        GLES20.glEnable(GLES20.GL_BLEND);
        // Blend based on the fragment's alpha value.
        GLES20.glBlendFunc(GLES20.GL_ONE /*GL_SRC_ALPHA*/, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        TexturedAlignedRect.prepareToDraw();
        gameState.drawScore();
        gameState.drawBall();
        gameState.drawMessages();
        TexturedAlignedRect.finishedDrawing();

        gameState.drawDebugStuff();

        // Turn alpha blending off.
        GLES20.glDisable(GLES20.GL_BLEND);

        if (EXTRA_CHECK) Util.checkGlError("onDrawFrame end");


        if (!gameState.isAnimating()) {
            Log.d(TAG, "Game over, stopping animation");

            mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }


    public void onViewPause(ConditionVariable syncObj) {

        mGameState.save();

        syncObj.open();
    }


    public void touchEvent(float x, float y) {


        float arenaX = (x - mViewportXoff) * (GameState.ARENA_WIDTH / mViewportWidth);
        float arenaY = (y - mViewportYoff) * (GameState.ARENA_HEIGHT / mViewportHeight);
        //Log.v(TAG, "touch at x=" + (int) x + " y=" + (int) y + " --> arenaX=" + (int) arenaX);

        mGameState.movePaddle(arenaX);
    }
}
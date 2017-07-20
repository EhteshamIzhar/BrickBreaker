package com.example.ehte6848.brickbreaker;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.ConditionVariable;
import android.view.MotionEvent;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class GameSurfaceView extends GLSurfaceView {
    private static final String TAG = BreakoutActivity.TAG;

    private GameSurfaceRenderer mRenderer;
    private final ConditionVariable syncObj = new ConditionVariable();


    public GameSurfaceView(Context context, GameState gameState,
                           TextResources.Configuration textConfig) {
        super(context);

        setEGLContextClientVersion(2);      // Request OpenGL ES 2.0


        mRenderer = new GameSurfaceRenderer(gameState, this, textConfig);
        setRenderer(mRenderer);
    }

    @Override
    public void onPause() {


        super.onPause();

        //Log.d(TAG, "asking renderer to pause");
        syncObj.close();
        queueEvent(new Runnable() {
            @Override public void run() {
                mRenderer.onViewPause(syncObj);
            }});
        syncObj.block();

        //Log.d(TAG, "renderer pause complete");
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                final float x, y;
                x = e.getX();
                y = e.getY();
                //Log.d(TAG, "GameSurfaceView onTouchEvent x=" + x + " y=" + y);
                queueEvent(new Runnable() {
                    @Override public void run() {
                        mRenderer.touchEvent(x, y);
                    }});
                break;
            default:
                break;
        }

        return true;
    }
}
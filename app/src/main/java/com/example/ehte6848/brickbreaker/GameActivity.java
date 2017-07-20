package com.example.ehte6848.brickbreaker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class GameActivity extends Activity {
    private static final String TAG = BreakoutActivity.TAG;

    private static final int DIFFICULTY_MIN = 0;
    private static final int DIFFICULTY_MAX = 3;        // inclusive
    private static final int DIFFICULTY_DEFAULT = 1;
    private static int sDifficultyIndex;

    private static boolean sNeverLoseBall;

    private static boolean sSoundEffectsEnabled;


    private GameSurfaceView mGLView;

    private GameState mGameState;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "GameActivity onCreate");


        SoundResources.initialize(this);
        TextResources.Configuration textConfig = TextResources.configure(this);

        mGameState = new GameState();
        configureGameState();

        mGLView = new GameSurfaceView(this, mGameState, textConfig);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() {

        Log.d(TAG, "GameActivity pausing");
        super.onPause();
        mGLView.onPause();

        updateHighScore(GameState.getFinalScore());
    }

    @Override
    protected void onResume() {

        Log.d(TAG, "GameActivity resuming");
        super.onResume();
        mGLView.onResume();
    }

    private void configureGameState() {
        int maxLives, minSpeed, maxSpeed;
        float ballSize, paddleSize, scoreMultiplier;

        switch (sDifficultyIndex) {
            case 0:                     // easy
                ballSize = 2.0f;
                paddleSize = 2.0f;
                scoreMultiplier = 0.75f;
                maxLives = 4;
                minSpeed = 200;
                maxSpeed = 500;
                break;
            case 1:                     // normal
                ballSize = 1;
                paddleSize = 1.0f;
                scoreMultiplier = 1.0f;
                maxLives = 3;
                minSpeed = 300;
                maxSpeed = 800;
                break;
            case 2:                     // hard
                ballSize = 1.0f;
                paddleSize = 0.8f;
                scoreMultiplier = 1.25f;
                maxLives = 3;
                minSpeed = 600;
                maxSpeed = 1200;
                break;
            case 3:                     // absurd
                ballSize = 1.0f;
                paddleSize = 0.5f;
                scoreMultiplier = 2f;
                maxLives = 1;
                minSpeed = 1000;
                maxSpeed = 100000;
                break;
            default:
                throw new RuntimeException("bad difficulty index " + sDifficultyIndex);
        }

        mGameState.setBallSizeMultiplier(ballSize);
        mGameState.setPaddleSizeMultiplier(paddleSize);
        mGameState.setScoreMultiplier(scoreMultiplier);
        mGameState.setMaxLives(maxLives);
        mGameState.setBallInitialSpeed(minSpeed);
        mGameState.setBallMaximumSpeed(maxSpeed);

        mGameState.setNeverLoseBall(sNeverLoseBall);

        SoundResources.setSoundEffectsEnabled(sSoundEffectsEnabled);
    }


    public static int getDifficultyIndex() {
        return sDifficultyIndex;
    }


    public static int getDefaultDifficultyIndex() {
        return DIFFICULTY_DEFAULT;
    }

    public static void setDifficultyIndex(int difficultyIndex) {

        if (difficultyIndex < DIFFICULTY_MIN || difficultyIndex > DIFFICULTY_MAX) {
            Log.w(TAG, "Invalid difficulty index " + difficultyIndex + ", using default");
            difficultyIndex = DIFFICULTY_DEFAULT;
        }

        if (sDifficultyIndex != difficultyIndex) {
            sDifficultyIndex = difficultyIndex;
            invalidateSavedGame();
        }
    }


    public static boolean getNeverLoseBall() {
        return sNeverLoseBall;
    }


    public static void setNeverLoseBall(boolean neverLoseBall) {
        if (sNeverLoseBall != neverLoseBall) {
            sNeverLoseBall = neverLoseBall;
            invalidateSavedGame();
        }
    }


    public static boolean getSoundEffectsEnabled() {
        return sSoundEffectsEnabled;
    }

    public static void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        sSoundEffectsEnabled = soundEffectsEnabled;
    }


    public static void invalidateSavedGame() {
        GameState.invalidateSavedGame();
    }


    public static boolean canResumeFromSave() {
        return GameState.canResumeFromSave();
    }

    private void updateHighScore(int lastScore) {
        SharedPreferences prefs = getSharedPreferences(BreakoutActivity.PREFS_NAME, MODE_PRIVATE);
        int highScore = prefs.getInt(BreakoutActivity.HIGH_SCORE_KEY, 0);

        Log.d(TAG, "final score was " + lastScore);
        if (lastScore > highScore) {
            Log.d(TAG, "new high score!  (" + highScore + " vs. " + lastScore + ")");

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(BreakoutActivity.HIGH_SCORE_KEY, lastScore);
            editor.commit();
        }
    }
}

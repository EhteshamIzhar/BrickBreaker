package com.example.ehte6848.brickbreaker;

import android.graphics.Rect;
import android.util.Log;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class GameState {
    private static final String TAG = BreakoutActivity.TAG;
    public static final boolean DEBUG_COLLISIONS = false;       // enable increased logging
    public static final boolean SHOW_DEBUG_STUFF = false;       // enable on-screen debugging


    private boolean mNeverLoseBall = false;      // if true, bounce off the bottom
    private int mMaxLives = 3;
    private int mBallInitialSpeed = 300;
    private int mBallMaximumSpeed = 800;
    private float mBallSizeMultiplier = 1.0f;
    private float mPaddleSizeMultiplier = 1.0f;
    private float mScoreMultiplier = 1.0f;


    private static SavedGame sSavedGame = new SavedGame();



    static final float ARENA_WIDTH = 768.0f;
    static final float ARENA_HEIGHT = 1024.0f;


    private static final float BRICK_TOP_PERC = 85 / 100.0f;
    private static final float BRICK_BOTTOM_PERC = 43 / 100.0f;
    private static final float BORDER_WIDTH_PERC = 2 / 100.0f;
    private static final int BRICK_COLUMNS = 12;
    private static final int BRICK_ROWS = 8;

    private static final float BORDER_WIDTH = (int) (BORDER_WIDTH_PERC * ARENA_WIDTH);


    private static final float SCORE_TOP = ARENA_HEIGHT - BORDER_WIDTH * 2;
    private static final float SCORE_RIGHT = ARENA_WIDTH - BORDER_WIDTH * 2;
    private static final float SCORE_HEIGHT_PERC = 5 / 100.0f;

    private static final float BRICK_HORIZONTAL_GAP_PERC = 20 / 100.0f;
    private static final float BRICK_VERTICAL_GAP_PERC = 50 / 100.0f;


    private static final float PADDLE_VERTICAL_PERC = 12 / 100.0f;
    private static final float PADDLE_HEIGHT_PERC = 1 / 100.0f;
    private static final float PADDLE_WIDTH_PERC = 2 / 100.0f;
    private static final int PADDLE_DEFAULT_WIDTH = 6;


    private static final float BALL_WIDTH_PERC = 2.5f / 100.0f;


    private static final int NUM_BORDERS = 4;
    private static final int BOTTOM_BORDER = 0;
    private BasicAlignedRect mBorders[] = new BasicAlignedRect[NUM_BORDERS];
    private BasicAlignedRect mBackground;


    private Brick mBricks[] = new Brick[BRICK_COLUMNS * BRICK_ROWS];
    private int mLiveBrickCount;


    private static final int DEFAULT_PADDLE_WIDTH =
            (int) (ARENA_WIDTH * PADDLE_WIDTH_PERC * PADDLE_DEFAULT_WIDTH);
    private BasicAlignedRect mPaddle;


    private static final int DEFAULT_BALL_DIAMETER = (int) (ARENA_WIDTH * BALL_WIDTH_PERC);
    private Ball mBall;


    private static final double NANOS_PER_SECOND = 1000000000.0;
    private static final double MAX_FRAME_DELTA_SEC = 0.5;
    private long mPrevFrameWhenNsec;


    private float mPauseDuration;


    private int mDebugSlowMotionFrames;

    // If FRAME_RATE_SMOOTHING is true, then the rest of these fields matter.
    private static final boolean FRAME_RATE_SMOOTHING = false;
    private static final int RECENT_TIME_DELTA_COUNT = 5;
    double mRecentTimeDelta[] = new double[RECENT_TIME_DELTA_COUNT];
    int mRecentTimeDeltaNext;


    private static final int HIT_FACE_NONE = 0;
    private static final int HIT_FACE_VERTICAL = 1;
    private static final int HIT_FACE_HORIZONTAL = 2;
    private static final int HIT_FACE_SHARPCORNER = 3;
    private BaseRect[] mPossibleCollisions =
            new BaseRect[BRICK_COLUMNS * BRICK_ROWS + NUM_BORDERS + NUM_SCORE_DIGITS + 1/*paddle*/];
    private float mHitDistanceTraveled;     // result from findFirstCollision()
    private float mHitXAdj, mHitYAdj;       // result from findFirstCollision()
    private int mHitFace;                   // result from findFirstCollision()
    private OutlineAlignedRect mDebugCollisionRect;  // visual debugging


    private static final int GAME_INITIALIZING = 0;
    private static final int GAME_READY = 1;
    private static final int GAME_PLAYING = 2;
    private static final int GAME_WON = 3;
    private static final int GAME_LOST = 4;
    private int mGamePlayState;

    private boolean mIsAnimating;
    private int mLivesRemaining;
    private int mScore;


    private static final int EVENT_NONE = 0;
    private static final int EVENT_LAST_BRICK = 1;
    private static final int EVENT_BALL_LOST = 2;

    private static final float STATUS_MESSAGE_WIDTH_PERC = 85 / 100.0f;
    private TexturedAlignedRect mGameStatusMessages;
    private int mGameStatusMessageNum;
    private int mDebugFramedString;


    private static final int NUM_SCORE_DIGITS = 5;
    private TexturedAlignedRect[] mScoreDigits = new TexturedAlignedRect[NUM_SCORE_DIGITS];


    private TextResources mTextRes;


    public GameState() {}


    public void setNeverLoseBall(boolean neverLoseBall) {
        mNeverLoseBall = neverLoseBall;
    }
    public void setMaxLives(int maxLives) {
        mMaxLives = maxLives;
    }
    public void setBallInitialSpeed(int speed) {
        mBallInitialSpeed = speed;
    }
    public void setBallMaximumSpeed(int speed) {
        mBallMaximumSpeed = speed;
    }
    public void setBallSizeMultiplier(float mult) {
        mBallSizeMultiplier = mult;
    }
    public void setPaddleSizeMultiplier(float mult) {
        mPaddleSizeMultiplier = mult;
    }
    public void setScoreMultiplier(float mult) {
        mScoreMultiplier = mult;
    }


    private void reset() {


        mGamePlayState = GAME_INITIALIZING;
        mIsAnimating = true;
        mGameStatusMessageNum = TextResources.NO_MESSAGE;
        mPrevFrameWhenNsec = 0;
        mPauseDuration = 0.0f;
        mRecentTimeDeltaNext = -1;
        mLivesRemaining = mMaxLives;
        mScore = 0;
        resetBall();
        //mLiveBrickCount = 0;      // initialized by allocBricks
    }


    private void resetBall() {
        mBall.setDirection(-0.3f, -1.0f);
        mBall.setSpeed(mBallInitialSpeed);

        mBall.setPosition(ARENA_WIDTH / 2.0f + 45, ARENA_HEIGHT * BRICK_BOTTOM_PERC - 100);
    }


    public void save() {


        synchronized (sSavedGame) {
            SavedGame save = sSavedGame;

            boolean[] bricks = new boolean[BRICK_ROWS * BRICK_COLUMNS];
            for (int i = 0; i < bricks.length; i++) {
                bricks[i] = mBricks[i].isAlive();
            }
            save.mLiveBricks = bricks;

            save.mBallXDirection = mBall.getXDirection();
            save.mBallYDirection = mBall.getYDirection();
            save.mBallXPosition = mBall.getXPosition();
            save.mBallYPosition = mBall.getYPosition();
            save.mBallSpeed = mBall.getSpeed();
            save.mPaddlePosition = mPaddle.getXPosition();

            save.mGamePlayState = mGamePlayState;
            save.mGameStatusMessageNum = mGameStatusMessageNum;
            save.mLivesRemaining = mLivesRemaining;
            save.mScore = mScore;

            save.mIsValid = true;
        }

        //Log.d(TAG, "game saved");
    }


    public boolean restore() {
        synchronized (sSavedGame) {
            SavedGame save = sSavedGame;
            if (!save.mIsValid) {
                Log.d(TAG, "No valid saved game found");
                reset();
                save();     // initialize save area
                return false;
            }
            boolean[] bricks = save.mLiveBricks;
            for (int i = 0; i < bricks.length; i++) {
                if (bricks[i]) {
                    // board creation sets all bricks to "live", don't need to setAlive() here
                } else {
                    mBricks[i].setAlive(false);
                    mLiveBrickCount--;
                }
            }


            mBall.setDirection(save.mBallXDirection, save.mBallYDirection);
            mBall.setPosition(save.mBallXPosition, save.mBallYPosition);
            mBall.setSpeed(save.mBallSpeed);
            movePaddle(save.mPaddlePosition);

            mGamePlayState = save.mGamePlayState;
            mGameStatusMessageNum = save.mGameStatusMessageNum;
            mLivesRemaining = save.mLivesRemaining;
            mScore = save.mScore;
        }

        return true;
    }


    public void surfaceChanged() {

        setPauseTime(1.5f);


        mPrevFrameWhenNsec = 0;

        mIsAnimating = true;
    }

    public void setTextResources(TextResources textRes) {
        mTextRes = textRes;
    }


    public static void invalidateSavedGame() {
        synchronized (sSavedGame) {
            sSavedGame.mIsValid = false;
        }
    }


    public static boolean canResumeFromSave() {
        synchronized (sSavedGame) {

            return sSavedGame.mIsValid &&
                    (sSavedGame.mGamePlayState == GAME_PLAYING ||
                            sSavedGame.mGamePlayState == GAME_READY);
        }
    }

    public static int getFinalScore() {
        synchronized (sSavedGame) {
            if (sSavedGame.mIsValid &&
                    (sSavedGame.mGamePlayState == GAME_WON ||
                            sSavedGame.mGamePlayState == GAME_LOST)) {
                return sSavedGame.mScore;
            } else {

                return -1;
            }
        }
    }


    public boolean isAnimating() {
        return mIsAnimating;
    }


    void allocBricks() {
        final float totalBrickWidth = ARENA_WIDTH - BORDER_WIDTH * 2;
        final float brickWidth = totalBrickWidth / BRICK_COLUMNS;
        final float totalBrickHeight = ARENA_HEIGHT * (BRICK_TOP_PERC - BRICK_BOTTOM_PERC);
        final float brickHeight = totalBrickHeight / BRICK_ROWS;

        final float zoneBottom = ARENA_HEIGHT * BRICK_BOTTOM_PERC;
        final float zoneLeft = BORDER_WIDTH;

        for (int i = 0; i < mBricks.length; i++) {
            Brick brick = new Brick();

            int row = i / BRICK_COLUMNS;
            int col = i % BRICK_COLUMNS;

            float bottom = zoneBottom + row * brickHeight;
            float left = zoneLeft + col * brickWidth;

            brick.setPosition(left + brickWidth / 2, bottom + brickHeight / 2);


            brick.setScale(brickWidth * (1.0f - BRICK_HORIZONTAL_GAP_PERC),
                    brickHeight * (1.0f - BRICK_VERTICAL_GAP_PERC));


            float factor = (float) i / (mBricks.length-1);  // [0..1], linear across all bricks
            int oddness = (row & 1) ^ (col & 1);            // 0 or 1, every other brick
            brick.setColor(factor, 1.0f - factor, 0.25f + 0.20f * oddness);


            brick.setScoreValue((row + 1) * 100);
            brick.setAlive(true);

            mBricks[i] = brick;
        }



        if (false) {
            // The maximum possible score determines how many digits we need to display.
            int max = 0;
            for (int j = 0; j < BRICK_ROWS; j++) {
                max += (j+1) * BRICK_COLUMNS;
            }
            Log.d(TAG, "max score on 'normal' is " + (max * 100));
        }


        mLiveBrickCount = mBricks.length;
    }


    void drawBricks() {
        for (int i = 0; i < mBricks.length; i++) {
            Brick brick = mBricks[i];

            if (brick.isAlive()) {
                brick.draw();
            }
        }
    }


    void allocBorders() {
        BasicAlignedRect rect;


        rect = new BasicAlignedRect();
        rect.setPosition(ARENA_WIDTH/2, ARENA_HEIGHT/2);
        rect.setScale(ARENA_WIDTH, ARENA_HEIGHT);
        rect.setColor(0.1f, 0.1f, 0.1f);
        mBackground = rect;


        rect = new BasicAlignedRect();
        rect.setPosition(ARENA_WIDTH/2, -BORDER_WIDTH/2);
        rect.setScale(ARENA_WIDTH, BORDER_WIDTH);
        rect.setColor(1.0f, 0.65f, 0.0f);
        mBorders[BOTTOM_BORDER] = rect;

        // Need one rect each for left / right / top.
        rect = new BasicAlignedRect();
        rect.setPosition(BORDER_WIDTH/2, ARENA_HEIGHT/2);
        rect.setScale(BORDER_WIDTH, ARENA_HEIGHT);
        rect.setColor(0.6f, 0.6f, 0.6f);
        mBorders[1] = rect;

        rect = new BasicAlignedRect();
        rect.setPosition(ARENA_WIDTH - BORDER_WIDTH/2, ARENA_HEIGHT/2);
        rect.setScale(BORDER_WIDTH, ARENA_HEIGHT);
        rect.setColor(0.6f, 0.6f, 0.6f);
        mBorders[2] = rect;

        rect = new BasicAlignedRect();
        rect.setPosition(ARENA_WIDTH/2, ARENA_HEIGHT - BORDER_WIDTH/2);
        rect.setScale(ARENA_WIDTH - BORDER_WIDTH*2, BORDER_WIDTH);
        rect.setColor(0.6f, 0.6f, 0.6f);
        mBorders[3] = rect;
    }


    void drawBorders() {
        mBackground.draw();
        for (int i = 0; i < mBorders.length; i++) {
            mBorders[i].draw();
        }
    }

    void allocPaddle() {
        BasicAlignedRect rect = new BasicAlignedRect();
        rect.setScale(DEFAULT_PADDLE_WIDTH * mPaddleSizeMultiplier,
                ARENA_HEIGHT * PADDLE_HEIGHT_PERC);
        rect.setColor(1.0f, 1.0f, 1.0f);        // note color is cycled during pauses

        rect.setPosition(ARENA_WIDTH / 2.0f, ARENA_HEIGHT * PADDLE_VERTICAL_PERC);
        //Log.d(TAG, "paddle y=" + rect.getYPosition());

        mPaddle = rect;
    }


    void drawPaddle() {
        mPaddle.draw();
    }


    void movePaddle(float arenaX) {


        float paddleWidth = mPaddle.getXScale() / 2;
        final float minX = BORDER_WIDTH + paddleWidth;
        final float maxX = ARENA_WIDTH - BORDER_WIDTH - paddleWidth;

        if (arenaX < minX) {
            arenaX = minX;
        } else if (arenaX > maxX) {
            arenaX = maxX;
        }

        mPaddle.setXPosition(arenaX);
    }


    void allocBall() {
        Ball ball = new Ball();
        int diameter = (int) (DEFAULT_BALL_DIAMETER * mBallSizeMultiplier);
        // ovals don't work right -- collision detection requires a circle
        ball.setScale(diameter, diameter);
        mBall = ball;
    }


    void drawBall() {


        Ball ball = mBall;
        float savedX = ball.getXPosition();
        float savedY = ball.getYPosition();
        float radius = ball.getRadius();

        float xpos = BORDER_WIDTH * 2 + radius;
        float ypos = BORDER_WIDTH + radius;
        int lives = mLivesRemaining;
        boolean ballIsLive = (mGamePlayState != GAME_INITIALIZING && mGamePlayState != GAME_READY);
        if (ballIsLive) {

            lives--;
        }

        for (int i = 0; i < lives; i++) {

            float jitterX = 0.0f;
            float jitterY = 0.0f;
            if (mLiveBrickCount > 0 && mLiveBrickCount < 4) {
                jitterX = (float) ((4 - mLiveBrickCount) * (Math.random() - 0.5) * 2);
                jitterY = (float) ((4 - mLiveBrickCount) * (Math.random() - 0.5) * 2);
            }
            ball.setPosition(xpos + jitterX, ypos + jitterY);
            ball.draw();

            xpos += radius * 3;
        }

        ball.setPosition(savedX, savedY);
        if (ballIsLive) {
            ball.draw();
        }
    }


    void allocScore() {


        int maxWidth = 0;
        Rect widest = null;
        for (int i = 0 ; i < 10; i++) {
            Rect boundsRect = mTextRes.getTextureRect(TextResources.DIGIT_START + i);
            int rectWidth = boundsRect.width();
            if (maxWidth < rectWidth) {
                maxWidth = rectWidth;
                widest = boundsRect;
            }
        }

        float widthHeightRatio = (float) widest.width() / widest.height();
        float cellHeight = ARENA_HEIGHT * SCORE_HEIGHT_PERC;
        float cellWidth = cellHeight * widthHeightRatio * 1.05f; // add 5% spacing between digits

        // Note these are laid out from right to left, i.e. mScoreDigits[0] is the 1s digit.
        float top = SCORE_TOP;
        float right = SCORE_RIGHT;
        for (int i = 0; i < NUM_SCORE_DIGITS; i++) {
            mScoreDigits[i] = new TexturedAlignedRect();
            mScoreDigits[i].setTexture(mTextRes.getTextureHandle(),
                    mTextRes.getTextureWidth(), mTextRes.getTextureHeight());
            mScoreDigits[i].setPosition(SCORE_RIGHT - (i * cellWidth) - cellWidth/2,
                    SCORE_TOP - cellHeight/2);
        }
    }


    void drawScore() {
        float cellHeight = ARENA_HEIGHT * SCORE_HEIGHT_PERC;
        int score = mScore;
        for (int i = 0; i < NUM_SCORE_DIGITS; i++) {
            int val = score % 10;
            Rect boundsRect = mTextRes.getTextureRect(TextResources.DIGIT_START + val);
            float ratio = cellHeight / boundsRect.height();

            TexturedAlignedRect scoreCell = mScoreDigits[i];
            scoreCell.setTextureCoords(boundsRect);
            scoreCell.setScale(boundsRect.width() * ratio,  cellHeight);
            scoreCell.draw();

            score /= 10;
        }
    }


    void allocMessages() {


        mGameStatusMessages = new TexturedAlignedRect();
        mGameStatusMessages.setTexture(mTextRes.getTextureHandle(),
                mTextRes.getTextureWidth(), mTextRes.getTextureHeight());
        mGameStatusMessages.setPosition(ARENA_WIDTH / 2, ARENA_HEIGHT / 2);
    }


    void drawMessages() {
        if (mGameStatusMessageNum != TextResources.NO_MESSAGE) {
            TexturedAlignedRect msgBox = mGameStatusMessages;

            Rect boundsRect = mTextRes.getTextureRect(mGameStatusMessageNum);
            msgBox.setTextureCoords(boundsRect);



            float scale = (ARENA_WIDTH * STATUS_MESSAGE_WIDTH_PERC) / boundsRect.width();
            msgBox.setScale(boundsRect.width() * scale, boundsRect.height() * scale);

            //Log.d(TAG, "drawing " + mGameStatusMessageNum);
            msgBox.draw();
        }
    }


    void allocDebugStuff() {
        mDebugCollisionRect = new OutlineAlignedRect();
        mDebugCollisionRect.setColor(1.0f, 0.0f, 0.0f);
    }


    void drawDebugStuff() {
        if (!SHOW_DEBUG_STUFF) {
            return;
        }

        // Draw a red outline rectangle around the ball.  This shows the area that was
        // examined for collisions during the "coarse" pass.
        if (true) {
            OutlineAlignedRect.prepareToDraw();
            mDebugCollisionRect.draw();
            OutlineAlignedRect.finishedDrawing();
        }

        // Draw the entire message texture so we can see what it looks like.
        if (true) {
            int textureWidth = mTextRes.getTextureWidth();
            int textureHeight = mTextRes.getTextureHeight();
            float scale = (ARENA_WIDTH * STATUS_MESSAGE_WIDTH_PERC) / textureWidth;

            // Draw an orange rect around the texture.
            OutlineAlignedRect outline = new OutlineAlignedRect();
            outline.setPosition(ARENA_WIDTH / 2, ARENA_HEIGHT / 2);
            outline.setScale(textureWidth * scale + 2, textureHeight * scale + 2);
            outline.setColor(1.0f, 0.65f, 0.0f);
            OutlineAlignedRect.prepareToDraw();
            outline.draw();
            OutlineAlignedRect.finishedDrawing();

            // Draw the full texture.  Note you can set the background to opaque white in
            // TextResources to see what the drop shadow looks like.
            Rect boundsRect = new Rect(0, 0, textureWidth, textureHeight);
            TexturedAlignedRect msgBox = mGameStatusMessages;
            msgBox.setTextureCoords(boundsRect);
            msgBox.setScale(textureWidth * scale, textureHeight * scale);
            TexturedAlignedRect.prepareToDraw();
            msgBox.draw();
            TexturedAlignedRect.finishedDrawing();

            // Draw a rectangle around each individual text item.  We draw a different one each
            // time to get a flicker effect, so it doesn't fully obscure the text.
            if (true) {
                outline.setColor(1.0f, 1.0f, 1.0f);
                int stringNum = mDebugFramedString;
                mDebugFramedString = (mDebugFramedString + 1) % TextResources.getNumStrings();
                boundsRect = mTextRes.getTextureRect(stringNum);

                float boundsCenterX = boundsRect.exactCenterX()- (textureWidth / 2);
                float boundsCenterY = boundsRect.exactCenterY() - (textureHeight / 2);

                boundsCenterX = ARENA_WIDTH / 2 + (boundsCenterX * scale);
                boundsCenterY = ARENA_HEIGHT / 2 - (boundsCenterY * scale);
                // Set the values and draw the rect.
                outline.setPosition(boundsCenterX, boundsCenterY);
                outline.setScale(boundsRect.width() * scale, boundsRect.height() * scale);
                OutlineAlignedRect.prepareToDraw();
                outline.draw();
                OutlineAlignedRect.finishedDrawing();
            }
        }
    }


    void setPauseTime(float durationMsec) {
        mPauseDuration = durationMsec;
    }


    void calculateNextFrame() {
        // First frame has no time delta, so make it a no-op.
        if (mPrevFrameWhenNsec == 0) {
            mPrevFrameWhenNsec = System.nanoTime();     // use monotonic clock
            mRecentTimeDeltaNext = -1;                  // reset saved values
            return;
        }



        long nowNsec = System.nanoTime();
        double curDeltaSec = (nowNsec - mPrevFrameWhenNsec) / NANOS_PER_SECOND;
        if (curDeltaSec > MAX_FRAME_DELTA_SEC) {
            // We went to sleep for an extended period.  Cap it at a reasonable limit.
            Log.d(TAG, "delta time was " + curDeltaSec + ", capping at " + MAX_FRAME_DELTA_SEC);
            curDeltaSec = MAX_FRAME_DELTA_SEC;
        }
        double deltaSec;

        if (FRAME_RATE_SMOOTHING) {
            if (mRecentTimeDeltaNext < 0) {
                // first time through, fill table with current value
                for (int i = 0; i < RECENT_TIME_DELTA_COUNT; i++) {
                    mRecentTimeDelta[i] = curDeltaSec;
                }
                mRecentTimeDeltaNext = 0;
            }

            mRecentTimeDelta[mRecentTimeDeltaNext] = curDeltaSec;
            mRecentTimeDeltaNext = (mRecentTimeDeltaNext + 1) % RECENT_TIME_DELTA_COUNT;

            deltaSec = 0.0f;
            for (int i = 0; i < RECENT_TIME_DELTA_COUNT; i++) {
                deltaSec += mRecentTimeDelta[i];
            }
            deltaSec /= RECENT_TIME_DELTA_COUNT;
        } else {
            deltaSec = curDeltaSec;
        }

        boolean advanceFrame = true;

        // If we're in a pause, animate the color of the paddle, but don't advance any state.
        if (mPauseDuration > 0.0f) {
            advanceFrame = false;
            if (mPauseDuration > deltaSec) {
                mPauseDuration -= deltaSec;

                if (mGamePlayState == GAME_PLAYING) {
                    // rotate through yellow, magenta, cyan
                    float[] colors = mPaddle.getColor();
                    if (colors[0] == 0.0f) {
                        mPaddle.setColor(1.0f, 0.0f, 1.0f);
                    } else if (colors[1] == 0.0f) {
                        mPaddle.setColor(1.0f, 1.0f, 0.0f);
                    } else {
                        mPaddle.setColor(0.0f, 1.0f, 1.0f);
                    }
                }
            } else {
                // leaving pause, restore paddle color to white
                mPauseDuration = 0.0f;
                mPaddle.setColor(1.0f, 1.0f, 1.0f);
            }
        }

        // Do something appropriate based on our current state.
        switch (mGamePlayState) {
            case GAME_INITIALIZING:
                mGamePlayState = GAME_READY;
                break;
            case GAME_READY:
                mGameStatusMessageNum = TextResources.READY;
                if (advanceFrame) {
                    // "ready" has expired, move ball to starting position
                    mGamePlayState = GAME_PLAYING;
                    mGameStatusMessageNum = TextResources.NO_MESSAGE;
                    setPauseTime(0.5f);
                    advanceFrame = false;
                }
                break;
            case GAME_WON:
                mGameStatusMessageNum = TextResources.WINNER;
                mIsAnimating = false;
                advanceFrame = false;
                break;
            case GAME_LOST:
                mGameStatusMessageNum = TextResources.GAME_OVER;
                mIsAnimating = false;
                advanceFrame = false;
                break;
            case GAME_PLAYING:
                break;
            default:
                Log.e(TAG, "GLITCH: bad state " + mGamePlayState);
                break;
        }

        // If we're playing, move the ball around.
        if (advanceFrame) {
            int event = moveBall(deltaSec);
            switch (event) {
                case EVENT_LAST_BRICK:
                    mGamePlayState = GAME_WON;

                    SoundResources.play(SoundResources.PADDLE_HIT);
                    SoundResources.play(SoundResources.WALL_HIT);
                    SoundResources.play(SoundResources.BALL_LOST);
                    break;
                case EVENT_BALL_LOST:
                    if (--mLivesRemaining == 0) {
                        // game over, man
                        mGamePlayState = GAME_LOST;
                    } else {
                        // switch back to "ready" state, reset ball position
                        mGamePlayState = GAME_READY;
                        mGameStatusMessageNum = TextResources.READY;
                        setPauseTime(1.5f);
                        resetBall();
                    }
                    break;
                case EVENT_NONE:
                    break;
                default:
                    throw new RuntimeException("bad game event: " + event);
            }
        }

        mPrevFrameWhenNsec = nowNsec;
    }


    private int moveBall(double deltaSec) {


        int event = EVENT_NONE;

        float radius = mBall.getRadius();
        float distance = (float) (mBall.getSpeed() * deltaSec);
        //Log.d(TAG, "delta=" + deltaSec * 60.0f + " dist=" + distance);

        if (mDebugSlowMotionFrames > 0) {

            final float SLOW_FACTOR = 8.0f;
            final float RAMP_FRAMES = 60.0f;
            float div;
            if (mDebugSlowMotionFrames > RAMP_FRAMES) {
                div = SLOW_FACTOR;
            } else {

                final float STEP = (SLOW_FACTOR - 1.0f) / RAMP_FRAMES;

                div = SLOW_FACTOR - (STEP * (RAMP_FRAMES - mDebugSlowMotionFrames));
            }
            distance /= div;

            mDebugSlowMotionFrames--;
        }

        while (distance > 0.0f) {
            float curX = mBall.getXPosition();
            float curY = mBall.getYPosition();
            float dirX = mBall.getXDirection();
            float dirY = mBall.getYDirection();
            float finalX = curX + dirX * distance;
            float finalY = curY + dirY * distance;
            float left, right, top, bottom;


            if (curX < finalX) {
                left = curX - radius;
                right = finalX + radius;
            } else {
                left = finalX - radius;
                right = curX + radius;
            }
            if (curY < finalY) {
                bottom = curY - radius;
                top = finalY + radius;
            } else {
                bottom = finalY - radius;
                top = curY + radius;
            }
            /* debug */
            mDebugCollisionRect.setPosition((curX + finalX) / 2, (curY + finalY) / 2);
            mDebugCollisionRect.setScale(right - left, top - bottom);

            int hits = 0;

            // test bricks
            for (int i = 0; i < mBricks.length; i++) {
                if (mBricks[i].isAlive() &&
                        checkCoarseCollision(mBricks[i], left, right, bottom, top)) {
                    mPossibleCollisions[hits++] = mBricks[i];
                }
            }

            // test borders
            for (int i = 0; i < NUM_BORDERS; i++) {
                if (checkCoarseCollision(mBorders[i], left, right, bottom, top)) {
                    mPossibleCollisions[hits++] = mBorders[i];
                }
            }

            // test paddle
            if (checkCoarseCollision(mPaddle, left, right, bottom, top)) {
                mPossibleCollisions[hits++] = mPaddle;
            }

            // test score... because we can
            if (false) {
                for (int i = 0; i < NUM_SCORE_DIGITS; i++) {

                    if (checkCoarseCollision(mScoreDigits[i], left, right, bottom, top)) {
                        mPossibleCollisions[hits++] = mScoreDigits[i];
                    }
                }
            }

            if (hits != 0) {
                // may have hit something, look closer
                BaseRect hit = findFirstCollision(mPossibleCollisions, hits, curX, curY,
                        dirX, dirY, distance, radius);

                if (hit == null) {
                    // didn't actually hit, clear counter
                    hits = 0;
                } else {
                    if (GameSurfaceRenderer.EXTRA_CHECK) {
                        if (mHitDistanceTraveled <= 0.0f) {
                            Log.e(TAG, "GLITCH: collision detection didn't move the ball");
                            mHitDistanceTraveled = distance;
                        }
                    }

                    // Update posn for the actual distance traveled and the collision adjustment
                    float newPosX = curX + dirX * mHitDistanceTraveled + mHitXAdj;
                    float newPosY = curY + dirY * mHitDistanceTraveled + mHitYAdj;
                    mBall.setPosition(newPosX, newPosY);
                    if (DEBUG_COLLISIONS) {
                        Log.d(TAG, "COL: intermediate cx=" + newPosX + " cy=" + newPosY);
                    }

                    // Update the direction vector based on the nature of the surface we
                    // struck.  We will override this for collisions with the paddle.
                    float newDirX = dirX;
                    float newDirY = dirY;
                    switch (mHitFace) {
                        case HIT_FACE_HORIZONTAL:
                            newDirY = -dirY;
                            break;
                        case HIT_FACE_VERTICAL:
                            newDirX = -dirX;
                            break;
                        case HIT_FACE_SHARPCORNER:
                            newDirX = -dirX;
                            newDirY = -dirY;
                            break;
                        case HIT_FACE_NONE:
                        default:
                            Log.e(TAG, "GLITCH: unexpected hit face" + mHitFace);
                            break;
                    }



                    if (hit instanceof Brick) {
                        Brick brick = (Brick) hit;
                        brick.setAlive(false);
                        mLiveBrickCount--;
                        mScore += brick.getScoreValue() * mScoreMultiplier;
                        if (mLiveBrickCount == 0) {
                            Log.d(TAG, "*** won ***");
                            event = EVENT_LAST_BRICK;
                            distance = 0.0f;
                        }
                        SoundResources.play(SoundResources.BRICK_HIT);
                    } else if (hit == mPaddle) {
                        if (mHitFace == HIT_FACE_HORIZONTAL) {
                            float paddleWidth = mPaddle.getXScale();
                            float paddleLeft = mPaddle.getXPosition() - paddleWidth / 2;
                            float hitAdjust = (newPosX - paddleLeft) / paddleWidth;


                            if (hitAdjust < 0.0f) {
                                hitAdjust = 0.0f;
                            }
                            if (hitAdjust > 1.0f) {
                                hitAdjust = 1.0f;
                            }
                            int hitPercent = (int) (hitAdjust * 100.0f);
                            hitAdjust -= 0.5f;
                            if (Math.abs(hitAdjust) > 0.25) {   // outer 25% on each side
                                if (dirX < 0 && hitAdjust > 0 || dirX > 0 && hitAdjust < 0) {
                                    //Log.d(TAG, "outside corner, big jump");
                                    hitAdjust *= 1.6;
                                } else {
                                    //Log.d(TAG, "far corner, modest jump");
                                    hitAdjust *= 1.2;
                                }
                            }
                            hitAdjust *= 1.25;

                            newDirX += hitAdjust;
                            float maxRatio = 3.0f;
                            if (Math.abs(newDirX) > Math.abs(newDirY) * maxRatio) {

                                if (newDirY < 0) {
                                    maxRatio = -maxRatio;
                                }
                                newDirY = Math.abs(newDirX) / maxRatio;
                            }
                        }

                        SoundResources.play(SoundResources.PADDLE_HIT);
                    } else if (hit == mBorders[BOTTOM_BORDER]) {

                        if (!mNeverLoseBall) {
                            event = EVENT_BALL_LOST;
                            distance = 0.0f;
                            SoundResources.play(SoundResources.BALL_LOST);
                        } else {
                            mScore -= 500 * mScoreMultiplier;
                            if (mScore < 0) {
                                mScore = 0;
                            }
                            SoundResources.play(SoundResources.WALL_HIT);
                        }
                    } else {
                        // hit a border or a score digit
                        SoundResources.play(SoundResources.WALL_HIT);
                    }


                    int speed = mBall.getSpeed();
                    speed += (mBallMaximumSpeed - mBallInitialSpeed) * 3 / 100;
                    if (speed > mBallMaximumSpeed) {
                        speed = mBallMaximumSpeed;
                    }
                    mBall.setSpeed(speed);

                    mBall.setDirection(newDirX, newDirY);
                    distance -= mHitDistanceTraveled;

                    if (DEBUG_COLLISIONS) {
                        Log.d(TAG, "COL: remaining dist=" + distance + " new dirX=" +
                                mBall.getXDirection() + " dirY=" + mBall.getYDirection());
                    }
                }
            }

            if (hits == 0) {
                // hit nothing, move ball to final position and bail
                if (DEBUG_COLLISIONS) {
                    Log.d(TAG, "COL: none (dist was " + distance + ")");
                }
                mBall.setPosition(finalX, finalY);
                distance = 0.0f;
            }
        }

        return event;
    }


    private boolean checkCoarseCollision(BaseRect target, float left, float right,
                                         float bottom, float top) {

        float xpos, ypos, xscale, yscale;
        float targLeft, targRight, targBottom, targTop;

        xpos = target.getXPosition();
        ypos = target.getYPosition();
        xscale = target.getXScale();
        yscale = target.getYScale();
        targLeft = xpos - xscale;
        targRight = xpos + xscale;
        targBottom = ypos - yscale;
        targTop = ypos + yscale;

        float checkLeft = targLeft > left ? targLeft : left;
        float checkRight = targRight < right ? targRight : right;
        float checkTop = targBottom > bottom ? targBottom : bottom;
        float checkBottom = targTop < top ? targTop : top;

        if (checkRight > checkLeft && checkBottom > checkTop) {
            return true;
        }
        return false;
    }


    private BaseRect findFirstCollision(BaseRect[] rects, final int numRects, final float curX,
                                        final float curY, final float dirX, final float dirY, final float distance,
                                        final float radius) {

        final float MAX_STEP = 2.0f;


        final float MIN_STEP = 0.001f;

        float radiusSq = radius * radius;
        int faceHit = HIT_FACE_NONE;
        int faceToAdjust = HIT_FACE_NONE;
        float xadj = 0.0f;
        float yadj = 0.0f;
        float traveled = 0.0f;

        while (traveled < distance) {
            // Travel a bit.
            if (distance - traveled > MAX_STEP) {
                traveled += MAX_STEP;
            } else if (distance - traveled < MIN_STEP) {

                break;
            } else {
                traveled = distance;
            }
            float circleXWorld = curX + dirX * traveled;
            float circleYWorld = curY + dirY * traveled;

            for (int i = 0; i < numRects; i++) {
                BaseRect rect = rects[i];
                float rectXWorld = rect.getXPosition();
                float rectYWorld = rect.getYPosition();
                float rectXScaleHalf = rect.getXScale() / 2.0f;
                float rectYScaleHalf = rect.getYScale() / 2.0f;

                float circleX = Math.abs(circleXWorld - rectXWorld);
                float circleY = Math.abs(circleYWorld - rectYWorld);

                if (circleX > rectXScaleHalf + radius || circleY > rectYScaleHalf + radius) {
                    // Circle is too far from rect edge(s) to overlap.  No collision.
                    continue;
                }


                if (circleX <= rectXScaleHalf) {
                    faceToAdjust = faceHit = HIT_FACE_HORIZONTAL;
                } else if (circleY <= rectYScaleHalf) {
                    faceToAdjust = faceHit = HIT_FACE_VERTICAL;
                } else {
                    // Check the distance from rect corner to center of circle.
                    float xdist = circleX - rectXScaleHalf;
                    float ydist = circleY - rectYScaleHalf;
                    if (xdist*xdist + ydist*ydist > radiusSq) {
                        // Not close enough.
                        //Log.d(TAG, "COL: corner miss");
                        continue;
                    }


                    float dirXSign = Math.signum(dirX);
                    float dirYSign = Math.signum(dirY);
                    float cornerXSign = Math.signum(rectXWorld - circleXWorld);
                    float cornerYSign = Math.signum(rectYWorld - circleYWorld);

                    String msg;
                    if (dirXSign == cornerXSign && dirYSign == cornerYSign) {
                        faceHit = HIT_FACE_SHARPCORNER;
                        msg = "sharp";
                        if (DEBUG_COLLISIONS) {

                            mDebugSlowMotionFrames = 240;
                        }
                    } else if (dirXSign == cornerXSign) {
                        faceHit = HIT_FACE_VERTICAL;
                        msg = "vert";
                    } else if (dirYSign == cornerYSign) {
                        faceHit = HIT_FACE_HORIZONTAL;
                        msg = "horiz";
                    } else {

                        Log.w(TAG, "COL: impossible corner hit");
                        faceHit = HIT_FACE_SHARPCORNER;
                        msg = "???";
                    }

                    if (DEBUG_COLLISIONS) {
                        Log.d(TAG, "COL: " + msg + "-corner hit xd=" + xdist + " yd=" + ydist
                                + " dir=" + dirXSign + "," + dirYSign
                                + " cor=" + cornerXSign + "," + cornerYSign);
                    }

                    if (xdist < ydist) {
                        faceToAdjust = HIT_FACE_HORIZONTAL;
                    } else {
                        faceToAdjust = HIT_FACE_VERTICAL;
                    }
                }

                if (DEBUG_COLLISIONS) {
                    String msg = "?";
                    if (faceHit == HIT_FACE_SHARPCORNER) {
                        msg = "corner";
                    } else if (faceHit == HIT_FACE_HORIZONTAL) {
                        msg = "horiz";
                    } else if (faceHit == HIT_FACE_VERTICAL) {
                        msg = "vert";
                    }
                    Log.d(TAG, "COL: " + msg + " hit " + rect.getClass().getSimpleName() +
                            " cx=" + circleXWorld + " cy=" + circleYWorld +
                            " rx=" + rectXWorld + " ry=" + rectYWorld +
                            " rxh=" + rectXScaleHalf + " ryh=" + rectYScaleHalf);
                }


                float hitXAdj, hitYAdj;
                if (faceToAdjust == HIT_FACE_HORIZONTAL) {
                    hitXAdj = 0.0f;
                    hitYAdj = rectYScaleHalf + radius - circleY;
                    if (GameSurfaceRenderer.EXTRA_CHECK && hitYAdj < 0.0f) {
                        Log.e(TAG, "HEY: horiz was neg");
                    }
                    if (circleYWorld < rectYWorld) {
                        // ball is below rect, must be moving up, so adjust it down
                        hitYAdj = -hitYAdj;
                    }
                } else if (faceToAdjust == HIT_FACE_VERTICAL) {
                    hitXAdj = rectXScaleHalf + radius - circleX;
                    hitYAdj = 0.0f;
                    if (GameSurfaceRenderer.EXTRA_CHECK && hitXAdj < 0.0f) {
                        Log.e(TAG, "HEY: vert was neg");
                    }
                    if (circleXWorld < rectXWorld) {
                        // ball is left of rect, must be moving to right, so adjust it left
                        hitXAdj = -hitXAdj;
                    }
                } else {
                    Log.w(TAG, "GLITCH: unexpected faceToAdjust " + faceToAdjust);
                    hitXAdj = hitYAdj = 0.0f;
                }

                if (DEBUG_COLLISIONS) {
                    Log.d(TAG, "COL:  r=" + radius + " trav=" + traveled +
                            " xadj=" + hitXAdj + " yadj=" + hitYAdj);
                }
                mHitFace = faceHit;
                mHitDistanceTraveled = traveled;
                mHitXAdj = hitXAdj;
                mHitYAdj = hitYAdj;
                return rect;
            }
        }


        return null;
    }


    private static class SavedGame {
        public boolean mLiveBricks[];
        public float mBallXDirection, mBallYDirection;
        public float mBallXPosition, mBallYPosition;
        public int mBallSpeed;
        public float mPaddlePosition;
        public int mGamePlayState;
        public int mGameStatusMessageNum;
        public int mLivesRemaining;
        public int mScore;

        public boolean mIsValid = false;        // set when state has been written out
    }
}
package com.example.ehte6848.brickbreaker;

import android.graphics.Rect;
import android.opengl.GLES20;

import java.nio.ByteBuffer;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class Ball extends TexturedAlignedRect {
    private static final String TAG = BreakoutActivity.TAG;

    private static final int TEX_SIZE = 64;        // dimension for square texture (power of 2)
    private static final int DATA_FORMAT = GLES20.GL_RGBA;  // 8bpp RGBA
    private static final int BYTES_PER_PIXEL = 4;

    // Normalized motion vector.
    private float mMotionX;
    private float mMotionY;


    private int mSpeed;

    public Ball() {
        if (true) {
            setTexture(generateBallTexture(), TEX_SIZE, TEX_SIZE, DATA_FORMAT);
            // Ball diameter is an odd number of pixels.
            setTextureCoords(new Rect(0, 0, TEX_SIZE-1, TEX_SIZE-1));
        } else {
            setTexture(generateTestTexture(), TEX_SIZE, TEX_SIZE, DATA_FORMAT);

            setTextureCoords(new Rect(0, 0, TEX_SIZE, TEX_SIZE));
        }
    }


    public float getXDirection() {
        return mMotionX;
    }


    public float getYDirection() {
        return mMotionY;
    }


    public void setDirection(float deltaX, float deltaY) {
        float mag = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        mMotionX = deltaX / mag;
        mMotionY = deltaY / mag;
    }


    public int getSpeed() {
        return mSpeed;
    }


    public void setSpeed(int speed) {
        if (speed <= 0) {
            throw new RuntimeException("speed must be positive (" + speed + ")");
        }
        mSpeed = speed;
    }


    public float getRadius() {
        // The "scale" value indicates diameter.
        return getXScale() / 2.0f;
    }


    private ByteBuffer generateBallTexture() {

        byte[] buf = new byte[TEX_SIZE * TEX_SIZE * BYTES_PER_PIXEL];


        int left[] = new int[TEX_SIZE-1];
        int right[] = new int[TEX_SIZE-1];
        computeCircleEdges(TEX_SIZE/2 - 1, left, right);

        // Render the edge list as a filled circle.
        for (int y = 0; y < left.length; y++) {
            int xleft = left[y];
            int xright = right[y];

            for (int x = xleft ; x <= xright; x++) {
                int offset = (y * TEX_SIZE + x) * BYTES_PER_PIXEL;
                buf[offset]   = (byte) 0xff;    // red
                buf[offset+1] = (byte) 0xff;    // green
                buf[offset+2] = (byte) 0xff;    // blue
                buf[offset+3] = (byte) 0xff;    // alpha
            }
        }

        // Create a ByteBuffer, copy the data over, and (very important) reset the position.
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(buf.length);
        byteBuf.put(buf);
        byteBuf.position(0);
        return byteBuf;
    }


    private static void computeCircleEdges(int rad, int[] left, int[] right) {
        /* (also available in 6502 assembly) */
        int x, y, d;

        d = 1 - rad;
        x = 0;
        y = rad;

        // Walk through one quadrant, setting the other three as reflections.
        while (x <= y) {
            setCircleValues(rad, x, y, left, right);

            if (d < 0) {
                d = d + (x << 2) + 3;
            } else {
                d = d + ((x - y) << 2) + 5;
                y--;
            }
            x++;
        }
    }


    private static void setCircleValues(int rad, int x, int y, int[] left, int[] right) {
        left[rad+y] = left[rad-y] = rad - x;
        left[rad+x] = left[rad-x] = rad - y;
        right[rad+y] = right[rad-y] = rad + x;
        right[rad+x] = right[rad-x] = rad + y;
    }


    // Colors for the test texture, in little-endian RGBA.
    public static final int BLACK = 0x00000000;
    public static final int RED = 0x000000ff;
    public static final int GREEN = 0x0000ff00;
    public static final int BLUE = 0x00ff0000;
    public static final int MAGENTA = RED | BLUE;
    public static final int YELLOW = RED | GREEN;
    public static final int CYAN = GREEN | BLUE;
    public static final int WHITE = RED | GREEN | BLUE;
    public static final int OPAQUE = (int) 0xff000000L;
    public static final int HALF = (int) 0x80000000L;
    public static final int LOW = (int) 0x40000000L;
    public static final int TRANSP = 0;

    public static final int GRID[] = new int[] {    // must be 16 elements
            OPAQUE|RED,     OPAQUE|YELLOW,  OPAQUE|GREEN,   OPAQUE|MAGENTA,
            OPAQUE|WHITE,   LOW|RED,        LOW|GREEN,      OPAQUE|YELLOW,
            OPAQUE|MAGENTA, TRANSP|GREEN,   HALF|RED,       OPAQUE|BLACK,
            OPAQUE|CYAN,    OPAQUE|MAGENTA, OPAQUE|CYAN,    OPAQUE|BLUE,
    };


    private ByteBuffer generateTestTexture() {
        byte[] buf = new byte[TEX_SIZE * TEX_SIZE * BYTES_PER_PIXEL];
        final int scale = TEX_SIZE / 4;        // convert 64x64 --> 4x4

        for (int i = 0; i < buf.length; i += BYTES_PER_PIXEL) {
            int texRow = (i / BYTES_PER_PIXEL) / TEX_SIZE;
            int texCol = (i / BYTES_PER_PIXEL) % TEX_SIZE;

            int gridRow = texRow / scale;  // 0-3
            int gridCol = texCol / scale;  // 0-3
            int gridIndex = (gridRow * 4) + gridCol;  // 0-15

            int color = GRID[gridIndex];

            // override the pixels in two corners to check coverage
            if (i == 0) {
                color = OPAQUE | WHITE;
            } else if (i == buf.length - BYTES_PER_PIXEL) {
                color = OPAQUE | WHITE;
            }

            // extract RGBA; use "int" instead of "byte" to get unsigned values
            int red = color & 0xff;
            int green = (color >> 8) & 0xff;
            int blue = (color >> 16) & 0xff;
            int alpha = (color >> 24) & 0xff;

            // pre-multiply colors and store in buffer
            float alphaM = alpha / 255.0f;
            buf[i] = (byte) (red * alphaM);
            buf[i+1] = (byte) (green * alphaM);
            buf[i+2] = (byte) (blue * alphaM);
            buf[i+3] = (byte) alpha;
        }

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(buf.length);
        byteBuf.put(buf);
        byteBuf.position(0);
        return byteBuf;
    }
}

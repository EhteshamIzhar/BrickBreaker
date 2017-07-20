package com.example.ehte6848.brickbreaker;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class Brick extends BasicAlignedRect {


    private boolean mAlive = false;
    private int mPoints = 0;


    public boolean isAlive() {
        return mAlive;
    }


    public void setAlive(boolean alive) {
        mAlive = alive;
    }

    public int getScoreValue() {
        return mPoints;
    }


    public void setScoreValue(int points) {
        mPoints = points;
    }
}

package com.example.ehte6848.brickbreaker;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by ehte6848 on 19-07-2017.
 */

public class SoundResources implements SoundPool.OnLoadCompleteListener {
    private static final String TAG = BreakoutActivity.TAG;


    public static final int BRICK_HIT = 0;
    public static final int PADDLE_HIT = 1;
    public static final int WALL_HIT = 2;
    public static final int BALL_LOST = 3;
    private static final int NUM_SOUNDS = 4;

    // Parameters for our generated sounds.
    private static final int SAMPLE_RATE = 22050;
    private static final int NUM_CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 8;

    // Singleton instance.
    private static SoundResources sSoundResources;

    // Maximum simultaneous sounds.  Four seems nice.
    private static final int MAX_STREAMS = 4;


    private static boolean sSoundEffectsEnabled = true;

    // The actual sound data.  Must be "final" for immutability guarantees.
    private final Sound[] mSounds = new Sound[NUM_SOUNDS];



    public static synchronized void initialize(Context context) {


        if (sSoundResources == null) {
            File dir = context.getFilesDir();
            sSoundResources = new SoundResources(dir);
        }
    }


    public static void play(int soundNum) {


        if (SoundResources.sSoundEffectsEnabled) {
            SoundResources instance = sSoundResources;
            if (instance != null) {
                instance.mSounds[soundNum].play();
            }
        }
    }


    public static void setSoundEffectsEnabled(boolean enabled) {
        sSoundEffectsEnabled = enabled;
    }


    private SoundResources(File privateDir) {
        SoundPool soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(this);
        generateSoundFiles(soundPool, privateDir);

        if (false) {
            // Sleep briefly to allow SoundPool to finish loading, then play each sound.
            try { Thread.sleep(1000); }
            catch (InterruptedException ie) {}

            for (int i = 0; i < NUM_SOUNDS; i++) {
                mSounds[i].play();
                try { Thread.sleep(800); } catch (InterruptedException ie) {}
            }
        }
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

        if (status != 0) {
            Log.w(TAG, "onLoadComplete: pool=" + soundPool + " sampleId=" + sampleId
                    + " status=" + status);
        }
    }


    private void generateSoundFiles(SoundPool soundPool, File privateDir) {

        mSounds[BRICK_HIT] = generateSound(soundPool, privateDir, "brick", 50 /*ms*/, 900 /*Hz*/);
        mSounds[PADDLE_HIT] = generateSound(soundPool, privateDir, "paddle", 50, 700);
        mSounds[WALL_HIT] = generateSound(soundPool, privateDir, "wall", 50, 300);
        mSounds[BALL_LOST] = generateSound(soundPool, privateDir, "ball_lost", 500, 280);
    }


    private Sound generateSound(SoundPool soundPool, File dir, String name, int lengthMsec,
                                int freqHz) {


        Sound sound = null;

        File outFile = new File(dir, name + ".wav");
        if (!outFile.exists()) {
            try {
                FileOutputStream fos = new FileOutputStream(outFile);

                // Number of samples.  Not worried about int overflow for our short sounds.
                int sampleCount = lengthMsec * SAMPLE_RATE / 1000;

                ByteBuffer buf = generateWavHeader(sampleCount);
                byte[] array = buf.array();
                fos.write(array);

                buf = generateWavData(sampleCount, freqHz);
                array = buf.array();
                fos.write(array);

                fos.close();
                Log.d(TAG, "Wrote sound file " + outFile.toString());

            } catch (IOException ioe) {
                Log.e(TAG, "sound file op failed: " + ioe.getMessage());
                throw new RuntimeException(ioe);
            }
        } else {
            //Log.d(TAG, "Sound '" + outFile.getName() + "' exists, not regenerating");
        }

        int handle = soundPool.load(outFile.toString(), 1);
        return new Sound(name, soundPool, handle);
    }


    private static ByteBuffer generateWavHeader(int sampleCount) {
        final int numDataBytes = sampleCount * NUM_CHANNELS * BITS_PER_SAMPLE / 8;

        ByteBuffer buf = ByteBuffer.allocate(44);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0x46464952);         // 'RIFF'
        buf.putInt(36 + numDataBytes);

        buf.putInt(0x45564157);         // 'WAVE'
        buf.putInt(0x20746d66);         // 'fmt '
        buf.putInt(16);
        buf.putShort((short) 1);        // audio format PCM
        buf.putShort((short) NUM_CHANNELS);
        buf.putInt(SAMPLE_RATE);
        buf.putInt(SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8);
        buf.putShort((short) (NUM_CHANNELS * BITS_PER_SAMPLE / 8));
        buf.putShort((short) BITS_PER_SAMPLE);

        buf.putInt(0x61746164);         // 'data'
        buf.putInt(numDataBytes);

        buf.position(0);
        return buf;
    }


    private static ByteBuffer generateWavData(int sampleCount, int freqHz) {
        final int numDataBytes = sampleCount * NUM_CHANNELS * BITS_PER_SAMPLE / 8;
        final double freq = freqHz;
        ByteBuffer buf = ByteBuffer.allocate(numDataBytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        if (BITS_PER_SAMPLE == 8) {
            final double peak = 127.0;

            for (int i = 0; i < sampleCount; i++) {
                double timeSec = i / (double) SAMPLE_RATE;
                double sinValue = Math.sin(2 * Math.PI * freq * timeSec);
                // 8-bit data is unsigned, 0-255
                if (GameSurfaceRenderer.EXTRA_CHECK) {
                    int output = (int) (peak * sinValue + 127.0);
                    if (output < 0 || output >= 256) {
                        throw new RuntimeException("bad byte gen");
                    }
                }
                buf.put((byte) (peak * sinValue + 127.0));
            }
        } else if (BITS_PER_SAMPLE == 16) {
            final double peak = 32767.0;

            ShortBuffer sbuf = buf.asShortBuffer();
            for (int i = 0; i < sampleCount; i++) {
                double timeSec = i / (double) SAMPLE_RATE;
                double sinValue = Math.sin(2 * Math.PI * freq * timeSec);
                // 16-bit data is signed, +/- 32767
                sbuf.put((short) (peak * sinValue));
            }
        }

        buf.position(0);
        return buf;
    }


    private static class Sound {
        private String mName;   // reference name, useful for debugging
        private SoundPool mSoundPool;
        private int mHandle;    // SoundPool handle
        private float mVolume = 0.5f;


        public Sound(String name, SoundPool soundPool, int handle) {
            mName = name;
            mSoundPool = soundPool;
            mHandle = handle;
        }


        public void play() {

            mSoundPool.play(mHandle, mVolume, mVolume, 1, 0, 1.0f);
        }
    }
}

package io.github.hamzahshamim.reactiontime;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends Activity {

    public static boolean cueIsDetected = false;
    public static int cueIsActive = -1;
    public static int numViolations = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        // Makes sure that the background isn't blinking for the visual cue
        deactivateColorCue();

        // Allows the app to be able to make the phone vibrate
        vibrator = (Vibrator) FullscreenActivity.this.getSystemService(Context.VIBRATOR_SERVICE);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        // button for visual cue
        (findViewById(R.id.button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checks if user has selected visual cue correctly
                if (cueIsActive == 0) {
                    cueIsDetected = true;
                } else {
                    // user is pressing buttons when there is no cue
                    // or has selected wrong cue, so there will be a penalty
                    numViolations++;
                    createUIToast("Strike " + numViolations);
                }
            }
        });

        // button for audio cue
        (findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checks if user has selected audio cue correctly
                if (cueIsActive == 1) {
                    cueIsDetected = true;
                } else {
                    // user is pressing buttons when there is no cue
                    // or has selected wrong cue, so there will be a penalty
                    numViolations++;
                    createUIToast("Strike " + numViolations);
                }
            }
        });

        // button for haptic cue
        (findViewById(R.id.button3)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checks if user has selected haptic cue correctly
                if (cueIsActive == 2) {
                    cueIsDetected = true;
                } else {
                    // user is pressing buttons when there is no cue
                    // or has selected wrong cue, so there will be a penalty
                    numViolations++;
                    createUIToast("Strike " + numViolations);
                }
            }
        });

        // make a default tone so that I don't need to make a new tone every time
        generateTone();

        // the testing thread
        Thread thread = new Thread(new Runnable() {

            private Random generator = new Random();
            private int numMaxRuns = 20;
            private List<List<Double>> dataValues = new ArrayList<>();

            public void run() {

                // for keeping track of my scores
                for (int i = 0; i < 3; i++)
                    dataValues.add(new ArrayList<Double>());

                // will only launch 20 cues max, from 0 to 19
                // and only 3 strikes allowed
                int runNum = 0;
                while (runNum < numMaxRuns && numViolations < 3) {
                    try {
                        int lenSleep = generator.nextInt(4000) + 1000;
                        Thread.sleep(lenSleep);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // select a random cue choice for what to send as the cue
                    // flash, emit sound, vibrate
                    cueIsActive = generator.nextInt(3);
                    //System.err.println("Cue " + cueIsActive);

                    // start the cue
                    cueIsDetected = false;
                    activateCue(cueIsActive);
                    long maxTime = (long) 6000000000.;
                    long timePassedSoFar = 0;
                    long startTime = System.nanoTime();

                    // wait until cue is detected
                    while (!cueIsDetected && timePassedSoFar < maxTime && numViolations < 3) {
                        timePassedSoFar = System.nanoTime() - startTime;
                    }

                    // stop the cue
                    deactivateCue(cueIsActive);

                    // check if correct cue was selected
                    if (cueIsDetected) {
                        createUIToast("Success");
                        dataValues.get(cueIsActive).add((double) timePassedSoFar / 1e6);
                    } else {
                        createUIToast("Failure");
                    }
                    cueIsActive = -1;
                    runNum++;
                }

                // write the final results when I'm done
                appendFinalText("Rxn Time Results (milliseconds)\nVisual :" + average(dataValues.get(0))
                                + "\nAuditory: " + average(dataValues.get(1))
                                + "\nHaptic: " + average(dataValues.get(2))
                );
            }
        });
        thread.start();
    }

    /**
     * Average the reaction times for the different cues
     * @param reactionTimes
     * @return
     */
    private double average(List<Double> reactionTimes) {
        double timeTotal = 0;
        for (double val : reactionTimes) {
            timeTotal += val;
        }
        return timeTotal / Math.max(reactionTimes.size(), 1);
    }

    /**
     * Launch a message on the screen
     * @param s
     */
    private void createUIToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FullscreenActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Add text to main text
     * @param s
     */
    private void appendFinalText(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.textView)).append("\n\n" + s);
            }
        });
    }

    /**
     * Start a cue after randomly selecting one
     * @param nextCue
     */
    private void activateCue(int nextCue) {
        if (nextCue == 0) {
            initiateColorCue();
        } else if (nextCue == 1) {
            playSound();
        } else if (nextCue == 2) {
            // Vibrate for 3 secs
            vibrator.vibrate(3000);
        } else {
            System.err.println("Error selecting cue" + nextCue);
        }
    }

    /**
     * Stop the cue when button has been pressed
     * @param nextCue
     */
    private void deactivateCue(int nextCue) {
        if (nextCue == 0) {
            deactivateColorCue();
        } else if (nextCue == 1) {
            audioTrack.stop();
            resetAudio();
        } else if (nextCue == 2) {
            vibrator.cancel();
        } else {
            System.err.println("Error selecting cue" + nextCue);
        }
    }

    // Vibration for haptic cue is easy, does not need specialized functions
    private Vibrator vibrator;


    /*****************************************************************************************
     *
     * Code for playing an audio sound from an Android app
     *
     * originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
     * and modified by Steve Pomeroy <steve@staticfree.info>
     * and then modified by Hamza Shamim <hamzahshamim.github.io>
     */
    private final int duration = 3; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private final double freqOfTone = 440; // hz
    private final byte generatedSnd[] = new byte[2 * numSamples];
    private AudioTrack audioTrack;

    /**
     * creates an audio tone that can later be played when necessary
     */
    private void generateTone() {
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
        resetAudio();
    }

    private void resetAudio() {
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
    }

    private void playSound() {
        audioTrack.play();
    }

    /*
     * End of code for playing an audio sound from an Android app
     *
     * originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
     * and modified by Steve Pomeroy <steve@staticfree.info>
     * and then modified by Hamza Shamim <hamzahshamim.github.io>
     *****************************************************************************************
     */


    /*****************************************************************************************
     *
     * Code for making the visual cue, which will make the screen
     * blink black and white very quickly
     *
     */
    private Thread colorThread;

    /**
     * Start color cue blinking
     */
    private void initiateColorCue() {
        deactivateColorCue();
        colorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (cueIsActive == 0) {
                    if ((cueIsActive == 0))
                        setColorCue(Color.BLACK);
                    if ((cueIsActive == 0))
                        try {
                            Thread.sleep(200);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    if ((cueIsActive == 0))
                        setColorCue(Color.WHITE);
                    if ((cueIsActive == 0))
                        try {
                            Thread.sleep(200);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        });
        colorThread.start();
    }

    /**
     * Force set the background color
     * @param color
     */
    private void setColorCue(final int color) {
        FullscreenActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.backframe).setBackgroundColor(color);
            }
        });
        this.findViewById(R.id.backframe).postInvalidate();
    }

    /**
     * Makes sure that the background isn't blinking after the visual cue is done
     */
    private void deactivateColorCue() {
        if (colorThread != null && colorThread.isAlive()) {
            colorThread.interrupt();
            colorThread = null;
        }

        FullscreenActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.backframe).setBackgroundColor(Color.GRAY);
            }
        });
        this.findViewById(R.id.backframe).postInvalidate();
    }
    /*
     *
     * Code for making the visual cue, which will make the screen
     * blink black and white very quickly
     *
     *****************************************************************************************
     */
}

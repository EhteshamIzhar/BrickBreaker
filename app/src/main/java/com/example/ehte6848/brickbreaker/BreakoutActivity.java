package com.example.ehte6848.brickbreaker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

public class BreakoutActivity extends Activity implements OnItemSelectedListener {
    public static final String TAG = "breakout";

    public static final String PREFS_NAME = "PrefsAndScores";

    private static final String DIFFICULTY_KEY = "difficulty";
    private static final String NEVER_LOSE_BALL_KEY = "never-lose-ball";
    private static final String SOUND_EFFECTS_ENABLED_KEY = "sound-effects-enabled";
    public static final String HIGH_SCORE_KEY = "high-score";

    private int mHighScore;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "BreakoutActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Spinner spinner = (Spinner) findViewById(R.id.spinner_difficultyLevel);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.difficulty_level_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "BreakoutActivity.onPause");
        super.onPause();

        savePreferences();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "BreakoutActivity.onResume");
        super.onResume();

        restorePreferences();
        updateControls();
    }


    private void updateControls() {
        Spinner difficulty = (Spinner) findViewById(R.id.spinner_difficultyLevel);
        difficulty.setSelection(GameActivity.getDifficultyIndex());

        Button resume = (Button) findViewById(R.id.button_resumeGame);
        resume.setEnabled(GameActivity.canResumeFromSave());

        CheckBox neverLoseBall = (CheckBox) findViewById(R.id.checkbox_neverLoseBall);
        neverLoseBall.setChecked(GameActivity.getNeverLoseBall());

        CheckBox soundEffectsEnabled = (CheckBox) findViewById(R.id.checkbox_soundEffectsEnabled);
        soundEffectsEnabled.setChecked(GameActivity.getSoundEffectsEnabled());

        TextView highScore = (TextView) findViewById(R.id.text_highScore);
        highScore.setText(String.valueOf(mHighScore));
    }


    public void clickNewGame(View view) {
        GameActivity.invalidateSavedGame();
        startGame();
    }

    public void clickResumeGame(View view) {
        startGame();
    }


    private void startGame() {


        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }


    public void clickAbout(View view) {
        AboutBox.display(this);
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        int difficulty = spinner.getSelectedItemPosition();

        GameActivity.setDifficultyIndex(difficulty);
        updateControls();
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    public void clickNeverLoseBall(View view) {


        GameActivity.setNeverLoseBall(((CheckBox) view).isChecked());
        updateControls();
    }


    public void clickSoundEffectsEnabled(View view) {


        GameActivity.setSoundEffectsEnabled(((CheckBox) view).isChecked());
        updateControls();
    }


    private void savePreferences() {

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(DIFFICULTY_KEY, GameActivity.getDifficultyIndex());
        editor.putBoolean(NEVER_LOSE_BALL_KEY, GameActivity.getNeverLoseBall());
        editor.putBoolean(SOUND_EFFECTS_ENABLED_KEY, GameActivity.getSoundEffectsEnabled());
        editor.commit();
    }


    private void restorePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        GameActivity.setDifficultyIndex(prefs.getInt(DIFFICULTY_KEY,
                GameActivity.getDefaultDifficultyIndex()));
        GameActivity.setNeverLoseBall(prefs.getBoolean(NEVER_LOSE_BALL_KEY, false));
        GameActivity.setSoundEffectsEnabled(prefs.getBoolean(SOUND_EFFECTS_ENABLED_KEY, true));

        mHighScore = prefs.getInt(HIGH_SCORE_KEY, 0);
    }
}

package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GamesActivity extends AppCompatActivity {

    private static final String PREFS = "progress_prefs";
    private static final String KEY_PREFIX = "level_";
    private static final int TOTAL_LEVELS = 7;

    private String category = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games);

        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) category = "";

        TextView tvTitle = findViewById(R.id.tvLessonsTitle);
        if (tvTitle != null) {
            tvTitle.setText(category.isEmpty() ? "Игры" : category);
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Обработчики уровней
        findViewById(R.id.level1).setOnClickListener(v -> openLevel(Level1Activity.class));
        findViewById(R.id.level2).setOnClickListener(v -> openLevel(Level2Activity.class));
        findViewById(R.id.level3).setOnClickListener(v -> openLevel(Level3Activity.class));
        findViewById(R.id.level4).setOnClickListener(v -> openLevel(Level4Activity.class));
        findViewById(R.id.level5).setOnClickListener(v -> openLevel(Level5Activity.class));
        findViewById(R.id.level6).setOnClickListener(v -> openLevel(Level6Activity.class));
        findViewById(R.id.level7).setOnClickListener(v -> openLevel(Level7Activity.class));


    }

    private void openLevel(Class<?> activityClass) {
        Intent intent = new Intent(GamesActivity.this, activityClass);
        intent.putExtra("CATEGORY", category);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllLevels();
    }

    private void refreshAllLevels() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        for (int i = 1; i <= TOTAL_LEVELS; i++) {
            updateLevelUI(prefs, i);
        }
    }

    private String buildKey(int levelNumber) {
        return KEY_PREFIX + levelNumber + "_" + category;
    }

    private void updateLevelUI(SharedPreferences prefs, int number) {
        int progress = prefs.getInt(buildKey(number), 0);
        int max = 1;

        int progressBarId = getResources().getIdentifier("level" + number + "ProgressBar", "id", getPackageName());
        ProgressBar progressBar = findViewById(progressBarId);

        if (progressBar != null) {
            progressBar.setProgress(progress * 100 / max);
            if (progress >= max) {
                progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            } else {
                progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E53935")));
            }
        }
    }
}
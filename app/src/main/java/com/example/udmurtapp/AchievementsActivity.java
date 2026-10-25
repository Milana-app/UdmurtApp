package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONObject;

public class AchievementsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        checkAchievements();
    }

    private void checkAchievements() {
        SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences streakPrefs = getSharedPreferences("streak_prefs", MODE_PRIVATE);

        String userId = userPrefs.getString("user_id", "guest");

        int totalLessons = 0;
        boolean perfectLesson = false;

        String historyKey = "stats_history_" + userId;
        String historyJson = prefs.getString(historyKey, "[]");

        try {
            JSONArray historyArray = new JSONArray(historyJson);
            totalLessons = historyArray.length();

            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject record = historyArray.getJSONObject(i);
                int wrong = record.optInt("wrong", 0);
                if (wrong == 0) perfectLesson = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ачивки
        setAchievementStatus(R.id.ach1Status, totalLessons >= 1);


        int streak = streakPrefs.getInt("streak_count_" + userId, 0);
        setAchievementStatus(R.id.ach2Status, streak >= 3);

        setAchievementStatus(R.id.ach3Status, totalLessons >= 5);

        setAchievementStatus(R.id.ach4Status, perfectLesson);

        setAchievementStatus(R.id.ach5Status, totalLessons >= 10);
    }

    private void setAchievementStatus(int iconId, boolean unlocked) {
        ImageView icon = findViewById(iconId);
        if (icon != null) {
            if (unlocked) {
                icon.setVisibility(android.view.View.VISIBLE);
                icon.setImageTintList(null);
            } else {
                icon.setVisibility(android.view.View.GONE);
            }
        }
    }
}
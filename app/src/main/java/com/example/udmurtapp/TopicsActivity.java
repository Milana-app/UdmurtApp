package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class TopicsActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {
            "Вежливые обращения", "Времена года", "Дикие животные", "Домашние животные",
            "Еда", "Моя семья", "Овощи", "Одежда", "Предметы", "Природа",
            "Фразы", "Цвета", "Числа", "Ягоды"
    };

    private static final String[] CIRCLE_COLORS = {
            "#E53935", "#FDD835", "#8E24AA", "#4CAF50",
            "#FDD835", "#8E24AA", "#E53935", "#4CAF50",
            "#FDD835", "#4CAF50", "#8E24AA", "#E53935",
            "#4CAF50", "#8E24AA"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topics);

        setActiveTab("lessons");

        int[] topicIds = {
                R.id.topic1, R.id.topic2, R.id.topic3, R.id.topic4,
                R.id.topic5, R.id.topic6, R.id.topic7, R.id.topic8,
                R.id.topic9, R.id.topic10, R.id.topic11, R.id.topic12,
                R.id.topic13, R.id.topic14
        };

        for (int i = 0; i < topicIds.length; i++) {
            final String category = CATEGORIES[i];
            View topicView = findViewById(topicIds[i]);
            if (topicView != null) {
                topicView.setOnClickListener(v -> {
                    Intent intent = new Intent(TopicsActivity.this, LessonsActivity.class);
                    intent.putExtra("CATEGORY", category);
                    startActivity(intent);
                });
            }
        }

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(TopicsActivity.this, HomeActivity.class));
            finish();
        });

        findViewById(R.id.navLessons).setOnClickListener(v ->
                Toast.makeText(this, "Вы уже на странице уроков", Toast.LENGTH_SHORT).show());

        findViewById(R.id.navDictionary).setOnClickListener(v -> {
            startActivity(new Intent(TopicsActivity.this, DictionaryActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(TopicsActivity.this, ProfileActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllProgress();
    }

    private void updateAllProgress() {
        SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Получаем ID текущего пользователя
        String userId = userPrefs.getString("user_id", "guest");

        for (int i = 0; i < CATEGORIES.length; i++) {
            String category = CATEGORIES[i];
            int topicNumber = i + 1;

            boolean lesson1 = prefs.getBoolean("lesson_1_passed_" + category + "_" + userId, false);
            boolean lesson2 = prefs.getBoolean("lesson_2_passed_" + category + "_" + userId, false);

            int passedCount = 0;
            if (lesson1) passedCount++;
            if (lesson2) passedCount++;

            int progressTextId = getResources().getIdentifier("topic" + topicNumber + "ProgressText", "id", getPackageName());
            int progressBarId = getResources().getIdentifier("topic" + topicNumber + "ProgressBar", "id", getPackageName());
            int iconId = getResources().getIdentifier("topic" + topicNumber + "Icon", "id", getPackageName());

            TextView tvProgress = findViewById(progressTextId);
            ProgressBar pbProgress = findViewById(progressBarId);
            TextView tvIcon = findViewById(iconId);

            if (tvProgress != null && pbProgress != null) {
                tvProgress.setText(passedCount + "/2");
                pbProgress.setMax(2);
                pbProgress.setProgress(passedCount);

                int color = Color.parseColor(CIRCLE_COLORS[i]);
                pbProgress.setProgressTintList(ColorStateList.valueOf(color));

                if (passedCount > 0) {
                    tvProgress.setTextColor(Color.parseColor("#2E7D32"));
                } else {
                    tvProgress.setTextColor(Color.parseColor("#757575"));
                }

                if (tvIcon != null) {
                    if (passedCount == 2) {
                        tvIcon.setText("");
                        tvIcon.setBackgroundResource(R.drawable.ic_check_circle);
                    } else {
                        tvIcon.setText("›");
                        tvIcon.setTextColor(Color.parseColor("#757575"));
                        tvIcon.setBackground(null);
                    }
                }
            }
        }
    }
    // нижняя навигация
    private void setActiveTab(String activeTab) {
        int activeColor = 0xFFD32F2F;
        int inactiveColor = 0xFF777777;

        setTabIconAndText(R.id.iconHome, R.id.textHome, R.drawable.ic_home_grey, inactiveColor);
        setTabIconAndText(R.id.iconLessons, R.id.textLessons, R.drawable.ic_book_grey, inactiveColor);
        setTabIconAndText(R.id.iconDictionary, R.id.textDictionary, R.drawable.ic_dictionary_grey, inactiveColor);
        setTabIconAndText(R.id.iconProfile, R.id.textProfile, R.drawable.ic_profile_grey, inactiveColor);

        switch (activeTab) {
            case "home":
                setTabIconAndText(R.id.iconHome, R.id.textHome, R.drawable.ic_home_red, activeColor);
                break;
            case "lessons":
                setTabIconAndText(R.id.iconLessons, R.id.textLessons, R.drawable.ic_book_red, activeColor);
                break;
            case "dictionary":
                setTabIconAndText(R.id.iconDictionary, R.id.textDictionary, R.drawable.ic_dictionary_red, activeColor);
                break;
            case "profile":
                setTabIconAndText(R.id.iconProfile, R.id.textProfile, R.drawable.ic_profile_red, activeColor);
                break;
        }
    }

    private void setTabIconAndText(int iconId, int textId, int drawableResId, int color) {
        ImageView icon = findViewById(iconId);
        TextView text = findViewById(textId);

        if (icon != null) {
            icon.setImageResource(drawableResId);
            icon.setImageTintList(null);
        }
        if (text != null) {
            text.setTextColor(color);
        }
    }
}
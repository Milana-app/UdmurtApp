package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class LessonsActivity extends AppCompatActivity {

    private String category = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lessons);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setActiveTab("lessons");

        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) category = "";

        // Обработчик для урока 1
        View card1 = findViewById(R.id.cardLesson1);
        if (card1 != null) {
            card1.setOnClickListener(v -> {
                Intent intent = new Intent(LessonsActivity.this, LessonActivity.class);
                intent.putExtra("CATEGORY", category);
                intent.putExtra("LESSON_NUMBER", 1);
                startActivity(intent);
            });
        }

        // обработчик урока 2
        View card2 = findViewById(R.id.cardLesson2);
        if (card2 != null) {
            card2.setOnClickListener(v -> {
                Intent intent = new Intent(LessonsActivity.this, LessonActivity.class);
                intent.putExtra("CATEGORY", category);
                intent.putExtra("LESSON_NUMBER", 2);
                startActivity(intent);
            });
        }

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(LessonsActivity.this, HomeActivity.class));
            finish();
        });

        findViewById(R.id.navLessons).setOnClickListener(v -> {
        });

        findViewById(R.id.navDictionary).setOnClickListener(v -> {
            startActivity(new Intent(LessonsActivity.this, DictionaryActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(LessonsActivity.this, ProfileActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkProgress();
    }

    private void checkProgress() {
        SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        updateCardUI(prefs, 1);
        updateCardUI(prefs, 2);
    }

    private void updateCardUI(SharedPreferences prefs, int lessonNumber) {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "guest");

        // проверяем прогресс для конкретного пользователя
        boolean isPassed = prefs.getBoolean("lesson_" + lessonNumber + "_passed_" + category + "_" + userId, false);

        int textStartId = getResources().getIdentifier("textStartLesson" + lessonNumber, "id", getPackageName());
        int textDescId = getResources().getIdentifier("textLessonDesc" + lessonNumber, "id", getPackageName());
        int cardId = getResources().getIdentifier("cardLesson" + lessonNumber, "id", getPackageName());
        int ivPassedId = getResources().getIdentifier("ivPassedIcon" + lessonNumber, "id", getPackageName());
        int ivLessonId = getResources().getIdentifier("ivLessonIcon" + lessonNumber, "id", getPackageName());

        TextView textStart = findViewById(textStartId);
        if (textStart != null) {
            if (isPassed) {
                textStart.setText("Урок пройден!\nНажмите, чтобы повторить");
            } else {
                textStart.setText("Начать урок " + lessonNumber);
            }
        }

        TextView textDesc = findViewById(textDescId);
        if (textDesc != null) {
            if (isPassed) {
                textDesc.setText("Вы отлично справились!");
            } else {
                if (lessonNumber == 1) textDesc.setText("4 слова: изучение, перевод, предложения и мемо");
                else textDesc.setText("Другие 4 слова для закрепления");
            }
        }

        androidx.cardview.widget.CardView card = findViewById(cardId);
        if (card != null) {
            if (isPassed) card.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            else card.setCardBackgroundColor(Color.WHITE);
        }

        ImageView ivPassed = findViewById(ivPassedId);
        if (ivPassed != null) {
            ivPassed.setVisibility(isPassed ? View.VISIBLE : View.GONE);
        }

        ImageView ivLesson = findViewById(ivLessonId);
        if (ivLesson != null) {
            // Книжка всегда видна, меняем только цвет
            ivLesson.setVisibility(View.VISIBLE);
            if (isPassed) {
                ivLesson.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            } else {
                ivLesson.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D32F2F")));
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
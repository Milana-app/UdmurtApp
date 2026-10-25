package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level6Activity extends AppCompatActivity {

    private static final String TAG = "LEVEL6";

    // Цвета
    private static final int COLOR_CORRECT = Color.parseColor("#33786C");
    private static final int COLOR_WRONG = Color.parseColor("#DF3844");
    private static final int COLOR_DEFAULT = Color.WHITE;

    private String category = "";

    private ImageView ivWordImage;
    private TextView tvWord;
    private TextView tvExplanation;
    private Button btnTrue, btnFalse, btnCheck;

    private final List<Word> allWords = new ArrayList<>();

    private Word currentWord;
    private boolean isStatementTrue;
    private Boolean userAnswer = null;
    private boolean isAnswered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level6);

        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) category = "";

        initViews();
        loadWordsFromRoom();
    }

    private void initViews() {
        ivWordImage = findViewById(R.id.ivWordImage);
        tvWord = findViewById(R.id.tvWord);
        tvExplanation = findViewById(R.id.tvExplanation);
        btnTrue = findViewById(R.id.btnTrue);
        btnFalse = findViewById(R.id.btnFalse);
        btnCheck = findViewById(R.id.btnCheck);

        TextView tvHint = findViewById(R.id.tvTranslationHint);
        if (tvHint != null) tvHint.setVisibility(View.GONE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnTrue.setOnClickListener(v -> selectAnswer(true));
        btnFalse.setOnClickListener(v -> selectAnswer(false));
        btnCheck.setOnClickListener(v -> checkAnswer());
    }

    private void loadWordsFromRoom() {
        new Thread(() -> {
            List<WordEntity> entities = AppDatabase.getInstance(this).wordDao().getAllWords();
            List<Word> parsed = new ArrayList<>();
            for (WordEntity e : entities) {
                if (category.isEmpty() || (e.category != null && e.category.equalsIgnoreCase(category))) {
                    parsed.add(new Word(
                            e.serverId != null ? e.serverId : "",
                            e.udmurt, e.russian, e.category,
                            e.imageUrl, e.audioUrl, e.example, e.exampleRus
                    ));
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (parsed.size() >= 2) {
                    allWords.clear();
                    allWords.addAll(parsed);
                    nextQuestion();
                } else if (entities.isEmpty()) {
                    loadWordsFromSupabase();
                } else {
                    String msg = category.isEmpty()
                            ? "В базе недостаточно слов (нужно минимум 2)"
                            : "В категории «" + category + "» меньше 2 слов";
                    Toast.makeText(Level6Activity.this, msg, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }).start();
    }

    private void loadWordsFromSupabase() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", "");

        if (accessToken.isEmpty()) {
            Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        SupabaseAuth.loadWords(accessToken, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String responseBody) {
                new Thread(() -> {
                    try {
                        JSONArray arr = new JSONArray(responseBody);
                        List<WordEntity> entities = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            WordEntity e = new WordEntity();
                            e.serverId = obj.optString("id");
                            e.udmurt = obj.optString("udmurt");
                            e.russian = obj.optString("russian");
                            e.category = obj.optString("category");
                            e.imageUrl = obj.optString("image_url");
                            e.audioUrl = obj.optString("audio_url");
                            e.example = obj.optString("example");
                            e.exampleRus = obj.optString("example_rus");
                            entities.add(e);
                        }

                        WordDao dao = AppDatabase.getInstance(Level6Activity.this).wordDao();
                        dao.deleteAll();
                        dao.insertAll(entities);

                        List<WordEntity> saved = dao.getAllWords();
                        List<Word> parsed = new ArrayList<>();
                        for (WordEntity e : saved) {
                            if (category.isEmpty() || (e.category != null && e.category.equalsIgnoreCase(category))) {
                                parsed.add(new Word(
                                        e.serverId != null ? e.serverId : "",
                                        e.udmurt, e.russian, e.category,
                                        e.imageUrl, e.audioUrl, e.example, e.exampleRus
                                ));
                            }
                        }

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (parsed.size() < 2) {
                                String msg = category.isEmpty()
                                        ? "В базе недостаточно слов (нужно минимум 2)"
                                        : "В категории «" + category + "» меньше 2 слов";
                                Toast.makeText(Level6Activity.this, msg, Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }
                            allWords.clear();
                            allWords.addAll(parsed);
                            nextQuestion();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка парсинга: " + e.getMessage());
                        runOnUiThread(() -> {
                            Toast.makeText(Level6Activity.this, "Ошибка загрузки слов", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                }).start();
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(Level6Activity.this,
                        "Ошибка загрузки: " + message, Toast.LENGTH_LONG).show());
            }
        });
    }

    // Новый вопрос
    private void nextQuestion() {
        userAnswer = null;
        isAnswered = false;

        if (tvExplanation != null) {
            tvExplanation.setVisibility(View.GONE);
            tvExplanation.setText("");
        }

        resetButtons();

        Collections.shuffle(allWords);
        Word correctWord = allWords.get(0);

        boolean showCorrect = Math.random() < 0.5;

        if (showCorrect) {
            currentWord = correctWord;
            isStatementTrue = true;
        } else {
            List<Word> others = new ArrayList<>(allWords);
            others.remove(correctWord);
            if (others.isEmpty()) {
                currentWord = correctWord;
                isStatementTrue = true;
            } else {
                Collections.shuffle(others);
                currentWord = others.get(0);
                isStatementTrue = false;
            }
        }

        // загружаем картинку(сначала локально, потом сеть)
        if (correctWord.getImageUrl() != null && !correctWord.getImageUrl().isEmpty()) {
            ivWordImage.setVisibility(View.VISIBLE);

            java.io.File localFile = ImageCacheManager.getLocalFile(this, correctWord.getImageUrl());

            if (localFile.exists()) {
                coil.Coil.imageLoader(this).enqueue(
                        new coil.request.ImageRequest.Builder(this)
                                .data(localFile)
                                .target(ivWordImage)
                                .crossfade(true)
                                .build()
                );
            } else {
                coil.Coil.imageLoader(this).enqueue(
                        new coil.request.ImageRequest.Builder(this)
                                .data(correctWord.getImageUrl())
                                .target(ivWordImage)
                                .crossfade(true)
                                .build()
                );
            }
        } else {
            ivWordImage.setVisibility(View.GONE);
        }

        tvWord.setText(currentWord.getUdmurt());
    }

    // выбор
    private void selectAnswer(boolean answer) {
        if (isAnswered) return;

        userAnswer = answer;

        // Сначала сбрасываем обе кнопки в белый
        resetButtons();

        // Выбранную кнопку делаем красной
        Button selectedBtn = answer ? btnTrue : btnFalse;
        setButtonColor(selectedBtn, COLOR_WRONG);
    }

    private void resetButtons() {
        setButtonColor(btnTrue, COLOR_DEFAULT);
        setButtonColor(btnFalse, COLOR_DEFAULT);
    }

    private void setButtonColor(Button btn, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(24)); // Скругление как в вашем bg_card_open
        bg.setColor(color);
        btn.setBackground(bg);

        if (color == COLOR_DEFAULT) {
            btn.setTextColor(Color.parseColor("#333333"));
        } else {
            btn.setTextColor(Color.WHITE);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void checkAnswer() {
        if (userAnswer == null) {
            Toast.makeText(this, "Выберите «Верно» или «Неверно»", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isAnswered) return;

        isAnswered = true;

        boolean correct = (userAnswer == isStatementTrue);

        if (correct) {
            //верно - зеленый
            Button selectedBtn = userAnswer ? btnTrue : btnFalse;
            setButtonColor(selectedBtn, COLOR_CORRECT);

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setText("Верно! " + currentWord.getUdmurt() + " — это " + currentWord.getRussian());
                tvExplanation.setTextColor(COLOR_CORRECT);
            }

            SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
            prefs.edit().putInt("level_6_" + category, 1).apply();

            btnCheck.postDelayed(this::nextQuestion, 2000);
        } else {
            // неверно - красный, верно - зеленый
            Button selectedBtn = userAnswer ? btnTrue : btnFalse;
            Button correctBtn = isStatementTrue ? btnTrue : btnFalse;

            setButtonColor(selectedBtn, COLOR_WRONG);
            setButtonColor(correctBtn, COLOR_CORRECT);

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                String trueAnswer = isStatementTrue ? "Верно" : "Неверно";
                tvExplanation.setText("Неверно! Правильный ответ: " + trueAnswer + "\n" + currentWord.getUdmurt() + " — это " + currentWord.getRussian());
                tvExplanation.setTextColor(COLOR_WRONG);
            }

            btnCheck.postDelayed(this::nextQuestion, 3000);
        }
    }
}
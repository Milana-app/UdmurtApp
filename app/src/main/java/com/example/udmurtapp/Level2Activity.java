package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level2Activity extends AppCompatActivity {

    private static final String TAG = "LEVEL2";
    private static final int OPTIONS_COUNT = 4;
    private static final int COLOR_CORRECT = Color.parseColor("#33786C");
    private static final int COLOR_WRONG = Color.parseColor("#DF3844");

    private String category = "";

    private TextView tvQuestion;
    private TextView tvExplanation;
    private final TextView[] optionViews = new TextView[OPTIONS_COUNT];
    private Button btnCheck;

    private final List<Word> allWords = new ArrayList<>();

    private Word currentWord;
    private String selectedOption = null;
    private int selectedIndex = -1;
    private boolean isAnswered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level2);

        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) category = "";

        initViews();
        loadWordsFromRoom(); // берем слова сначала из локальной бд
    }

    private void initViews() {
        tvQuestion = findViewById(R.id.tvQuestion);
        tvExplanation = findViewById(R.id.tvExplanation);
        optionViews[0] = findViewById(R.id.tvOption1);
        optionViews[1] = findViewById(R.id.tvOption2);
        optionViews[2] = findViewById(R.id.tvOption3);
        optionViews[3] = findViewById(R.id.tvOption4);

        for (int i = 0; i < optionViews.length; i++) {
            final int index = i;
            optionViews[i].setOnClickListener(v -> onOptionClick(index));
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnCheck = findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(v -> checkAnswer());
    }

    // смотрим слова из локальной базы, если слов не хватает - из бд(с интернетом)
    private void loadWordsFromRoom() {
        new Thread(() -> {
            List<WordEntity> entities = AppDatabase.getInstance(this).wordDao().getAllWords();
            List<Word> parsed = new ArrayList<>();
            for (WordEntity e : entities) {
                // Фильтр по категории, если задана
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

                if (parsed.size() >= OPTIONS_COUNT) {
                    // Слов достаточно — начинаем игру
                    allWords.clear();
                    allWords.addAll(parsed);
                    nextQuestion();
                } else if (entities.isEmpty()) {
                    // если база пуста - скачиваем из бд(если есть интернет)
                    loadWordsFromSupabase();
                } else {
                    // слов меньше, чем нужно для уровня
                    String msg = category.isEmpty()
                            ? "В базе недостаточно слов (нужно минимум " + OPTIONS_COUNT + ")"
                            : "В категории «" + category + "» меньше " + OPTIONS_COUNT + " слов";
                    Toast.makeText(Level2Activity.this, msg, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }).start();
    }

    // загрузка слов
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

                        WordDao dao = AppDatabase.getInstance(Level2Activity.this).wordDao();
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
                            if (parsed.size() < OPTIONS_COUNT) {
                                String msg = category.isEmpty()
                                        ? "В базе недостаточно слов (нужно минимум " + OPTIONS_COUNT + ")"
                                        : "В категории «" + category + "» меньше " + OPTIONS_COUNT + " слов";
                                Toast.makeText(Level2Activity.this, msg, Toast.LENGTH_LONG).show();
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
                            Toast.makeText(Level2Activity.this, "Ошибка загрузки слов", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                }).start();
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(Level2Activity.this,
                        "Ошибка загрузки: " + message, Toast.LENGTH_LONG).show());
            }
        });
    }

    // новый вопрос
    private void nextQuestion() {
        selectedOption = null;
        selectedIndex = -1;
        isAnswered = false;

        if (tvExplanation != null) {
            tvExplanation.setVisibility(View.GONE);
        }

        resetAllOptions();

        Collections.shuffle(allWords);
        currentWord = allWords.get(0);

        tvQuestion.setText(currentWord.getUdmurt());

        List<String> options = new ArrayList<>();
        options.add(currentWord.getRussian());

        List<Word> others = new ArrayList<>(allWords);
        others.remove(currentWord);
        Collections.shuffle(others);

        for (int i = 0; i < OPTIONS_COUNT - 1 && i < others.size(); i++) {
            options.add(others.get(i).getRussian());
        }

        Collections.shuffle(options);

        for (int i = 0; i < OPTIONS_COUNT; i++) {
            optionViews[i].setText(options.get(i));
        }
    }

    // выбор варианта
    private void onOptionClick(int index) {
        if (isAnswered) return;

        selectedIndex = index;
        selectedOption = optionViews[index].getText().toString();

        resetAllOptions();

        optionViews[index].setBackgroundResource(R.drawable.bg_card_open);
        optionViews[index].setTextColor(Color.parseColor("#E53935"));
        optionViews[index].setTypeface(null, Typeface.BOLD);
    }

    private void resetAllOptions() {
        for (TextView tv : optionViews) {
            tv.setBackgroundResource(R.drawable.bg_word_right);
            tv.setTextColor(Color.parseColor("#555555"));
            tv.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void setOptionBackground(TextView tv, int colorHex) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(16));
        bg.setColor(colorHex);
        tv.setBackground(bg);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // Проверка
    private void checkAnswer() {
        if (selectedOption == null) {
            Toast.makeText(this, "Выберите вариант ответа", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isAnswered) return;

        isAnswered = true;

        if (selectedOption.equals(currentWord.getRussian())) {
            // Правильно
            setOptionBackground(optionViews[selectedIndex], COLOR_CORRECT);
            optionViews[selectedIndex].setTextColor(Color.WHITE);

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setText("Верно! " + currentWord.getUdmurt() + " — это " + currentWord.getRussian());
                tvExplanation.setTextColor(COLOR_CORRECT);
            }

            SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
            prefs.edit().putInt("level_2_" + category, 1).apply();

            btnCheck.postDelayed(this::nextQuestion, 1500);

        } else {
            // Неправильно
            for (int i = 0; i < OPTIONS_COUNT; i++) {
                if (optionViews[i].getText().toString().equals(currentWord.getRussian())) {
                    // Правильный вариант — зеленая заливка
                    setOptionBackground(optionViews[i], COLOR_CORRECT);
                    optionViews[i].setTextColor(Color.WHITE);
                    optionViews[i].setTypeface(null, Typeface.BOLD);
                }
            }
            setOptionBackground(optionViews[selectedIndex], COLOR_WRONG);
            optionViews[selectedIndex].setTextColor(Color.WHITE);
            optionViews[selectedIndex].setTypeface(null, Typeface.BOLD);

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setText("Неверно! Правильный ответ: " + currentWord.getRussian() + "\n(" + currentWord.getUdmurt() + ")");
                tvExplanation.setTextColor(COLOR_WRONG);
            }

            btnCheck.postDelayed(this::nextQuestion, 2500);
        }
    }
}
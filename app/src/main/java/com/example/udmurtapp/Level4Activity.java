package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Level4Activity extends AppCompatActivity {

    private static final String TAG = "LEVEL4";
    private static final int OPTIONS_COUNT = 4;
    private static final int SAME_CATEGORY_WORDS = 3;
    private static final int COLOR_CORRECT = Color.parseColor("#33786C"); // Зеленый
    private static final int COLOR_WRONG = Color.parseColor("#DF3844");   // Красный

    private String chosenCategory = "";

    private final TextView[] optionViews = new TextView[OPTIONS_COUNT];
    private TextView tvExplanation;
    private Button btnCheck;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<Word> allWords = new ArrayList<>();

    private String correctAnswer = null;
    private int selectedIndex = -1;
    private boolean isAnswered = false;

    private final List<String> currentMainWords = new ArrayList<>();
    private String currentMainCategory = "";
    private String currentOddCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level4);

        chosenCategory = getIntent().getStringExtra("CATEGORY");
        if (chosenCategory == null) chosenCategory = "";

        initViews();
        loadWordsFromRoom();
    }

    private void initViews() {
        optionViews[0] = findViewById(R.id.tvOption1);
        optionViews[1] = findViewById(R.id.tvOption2);
        optionViews[2] = findViewById(R.id.tvOption3);
        optionViews[3] = findViewById(R.id.tvOption4);

        tvExplanation = findViewById(R.id.tvExplanation);

        // обработчики нажатий на варианты ответов
        for (int i = 0; i < optionViews.length; i++) {
            final int index = i;
            optionViews[i].setOnClickListener(v -> onOptionClick(index));
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnCheck = findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(v -> checkAnswer());
    }

    private void loadWordsFromRoom() {
        new Thread(() -> {
            List<WordEntity> entities = AppDatabase.getInstance(this).wordDao().getAllWords();
            List<Word> parsed = new ArrayList<>();
            for (WordEntity e : entities) {
                parsed.add(new Word(
                        e.serverId != null ? e.serverId : "",
                        e.udmurt, e.russian, e.category,
                        e.imageUrl, e.audioUrl, e.example, e.exampleRus
                ));
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (parsed.size() >= OPTIONS_COUNT) {
                    allWords.clear();
                    allWords.addAll(parsed);
                    nextQuestion();
                } else if (entities.isEmpty()) {
                    loadWordsFromSupabase();
                } else {
                    finish();
                }
            });
        }).start();
    }

    private void loadWordsFromSupabase() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", "");

        if (accessToken.isEmpty()) {
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

                        WordDao dao = AppDatabase.getInstance(Level4Activity.this).wordDao();
                        dao.deleteAll();
                        dao.insertAll(entities);

                        List<WordEntity> saved = dao.getAllWords();
                        List<Word> parsed = new ArrayList<>();
                        for (WordEntity e : saved) {
                            parsed.add(new Word(
                                    e.serverId != null ? e.serverId : "",
                                    e.udmurt, e.russian, e.category,
                                    e.imageUrl, e.audioUrl, e.example, e.exampleRus
                            ));
                        }

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (parsed.size() < OPTIONS_COUNT) {
                                finish();
                                return;
                            }
                            allWords.clear();
                            allWords.addAll(parsed);
                            nextQuestion();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка парсинга: " + e.getMessage());
                        runOnUiThread(() -> finish());
                    }
                }).start();
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> finish());
            }
        });
    }

    private void nextQuestion() {
        selectedIndex = -1;
        correctAnswer = null;
        isAnswered = false;

        if (tvExplanation != null) {
            tvExplanation.setText("");
            tvExplanation.setVisibility(View.GONE);
        }

        resetAllOptions();
        currentMainWords.clear();

        // группируем слова по категориям
        Map<String, List<Word>> byCategory = new HashMap<>();
        for (Word w : allWords) {
            if (!byCategory.containsKey(w.getCategory())) {
                byCategory.put(w.getCategory(), new ArrayList<>());
            }
            byCategory.get(w.getCategory()).add(w);
        }

        List<String> validCategories = new ArrayList<>();
        for (Map.Entry<String, List<Word>> e : byCategory.entrySet()) {
            if (e.getValue().size() >= SAME_CATEGORY_WORDS) {
                validCategories.add(e.getKey());
            }
        }

        if (validCategories.size() < 2) {
            finish();
            return;
        }

        String mainCategory;
        String oddCategory;

        if (!chosenCategory.isEmpty() && validCategories.contains(chosenCategory)) {
            mainCategory = chosenCategory;

            String similar = findSimilarCategory(chosenCategory, validCategories);
            if (similar != null) {
                oddCategory = similar;
            } else {
                List<String> others = new ArrayList<>(validCategories);
                others.remove(mainCategory);
                Collections.shuffle(others);
                oddCategory = others.get(0);
            }
        } else {
            Collections.shuffle(validCategories);
            mainCategory = validCategories.get(0);
            oddCategory = validCategories.get(1);
        }

        currentMainCategory = mainCategory;
        currentOddCategory = oddCategory;

        // выбираем 3 слова из основной категории и 1 из другой (лишнее)
        List<Word> mainWords = new ArrayList<>(byCategory.get(mainCategory));
        Collections.shuffle(mainWords);
        List<Word> chosen = new ArrayList<>(mainWords.subList(0, SAME_CATEGORY_WORDS));

        for (Word w : chosen) {
            currentMainWords.add(w.getUdmurt());
        }

        List<Word> oddWords = new ArrayList<>(byCategory.get(oddCategory));
        Collections.shuffle(oddWords);
        Word oddWord = oddWords.get(0);
        chosen.add(oddWord);

        correctAnswer = oddWord.getUdmurt();
        Collections.shuffle(chosen);

        for (int i = 0; i < OPTIONS_COUNT; i++) {
            optionViews[i].setText(chosen.get(i).getUdmurt());
        }
    }

    private String findSimilarCategory(String chosen, List<String> available) {
        String[][] similarPairs = {
                {"Овощи", "Ягоды"}, {"Ягоды", "Овощи"},
                {"Дикие животные", "Домашние животные"},
                {"Домашние животные", "Дикие животные"},
                {"Еда", "Овощи"}, {"Еда", "Ягоды"},
                {"Одежда", "Предметы"}, {"Предметы", "Одежда"},
                {"Природа", "Времена года"}, {"Времена года", "Природа"},
                {"Моя семья", "Фразы"}, {"Фразы", "Моя семья"},
                {"Числа", "Цвета"}, {"Цвета", "Числа"},
                {"Природа", "Дикие животные"},
                {"Природа", "Домашние животные"},
                {"Вежливые обращения", "Фразы"},
                {"Фразы", "Вежливые обращения"}
        };

        for (String[] pair : similarPairs) {
            if (pair[0].equalsIgnoreCase(chosen)
                    && available.contains(pair[1])
                    && !pair[1].equalsIgnoreCase(chosen)) {
                return pair[1];
            }
        }
        return null;
    }

    private void onOptionClick(int index) {
        if (isAnswered) return;

        selectedIndex = index;
        resetAllOptions();

        // выделяем выбранный вариант красным
        setOptionBackground(optionViews[index], COLOR_WRONG);
        optionViews[index].setTextColor(Color.WHITE);
        optionViews[index].setTypeface(null, Typeface.BOLD);
    }

    private void resetAllOptions() {
        for (TextView tv : optionViews) {
            tv.setBackgroundResource(R.drawable.bg_card_closed);
            tv.setTextColor(Color.parseColor("#333333"));
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

    private void checkAnswer() {
        if (selectedIndex == -1) {
            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setTextColor(COLOR_WRONG);
                tvExplanation.setText("Выберите лишнее слово");

                handler.postDelayed(() -> {
                    if (tvExplanation != null) {
                        tvExplanation.setVisibility(View.GONE);
                        tvExplanation.setText("");
                    }
                }, 2000);
            }
            return;
        }
        if (isAnswered) return;

        isAnswered = true;

        String selectedText = optionViews[selectedIndex].getText().toString();

        if (selectedText.equals(correctAnswer)) {
            // верно
            setOptionBackground(optionViews[selectedIndex], COLOR_CORRECT);
            optionViews[selectedIndex].setTextColor(Color.WHITE);

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setText("Верно! Слово «" + correctAnswer + "» — лишнее, так как относится к категории «" + currentOddCategory + "».");
                tvExplanation.setTextColor(COLOR_CORRECT);
            }

            SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
            prefs.edit().putInt("level_4_" + chosenCategory, 1).apply();

            handler.postDelayed(this::nextQuestion, 2500);
        } else {
            // неверно
            for (int i = 0; i < OPTIONS_COUNT; i++) {
                if (optionViews[i].getText().toString().equals(correctAnswer)) {
                    setOptionBackground(optionViews[i], COLOR_CORRECT);
                    optionViews[i].setTextColor(Color.WHITE);
                    optionViews[i].setTypeface(null, Typeface.BOLD);
                }
            }

            setOptionBackground(optionViews[selectedIndex], COLOR_WRONG);
            optionViews[selectedIndex].setTextColor(Color.WHITE);

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);

                StringBuilder wordsList = new StringBuilder();
                for (String word : currentMainWords) {
                    if (!word.equals(selectedText)) {
                        if (wordsList.length() > 0) wordsList.append(", ");
                        wordsList.append("«").append(word).append("»");
                    }
                }

                String explanation = "Неверно!\n\n"
                        + "Слова " + wordsList.toString() + " относятся к категории «" + currentMainCategory + "».\n\n"
                        + "А слово «" + correctAnswer + "» — лишнее, оно из категории «" + currentOddCategory + "».";

                tvExplanation.setText(explanation);
                tvExplanation.setTextColor(COLOR_WRONG);
            }

            handler.postDelayed(this::nextQuestion, 5000);
        }
    }
}
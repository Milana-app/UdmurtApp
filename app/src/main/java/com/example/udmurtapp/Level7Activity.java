package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level7Activity extends AppCompatActivity {

    private static final String TAG = "LEVEL7";
    private static final int EXTRA_LETTERS = 4;

    private String category = "";

    private TextView tvRussianWord;
    private LinearLayout answerContainer;
    private LinearLayout lettersContainer;
    private Button btnCheck;
    private TextView tvExplanation;

    private final List<Word> allWords = new ArrayList<>();

    private Word currentWord;
    private String correctUdmurt;
    private final List<Character> availableLetters = new ArrayList<>();
    private final List<Integer> availableIndices = new ArrayList<>();
    private final StringBuilder userAnswer = new StringBuilder();
    private final List<Integer> userAnswerIndices = new ArrayList<>();
    private List<Character> allGeneratedLetters = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level7);

        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) category = "";

        initViews();
        loadWordsFromRoom();
    }

    private void initViews() {
        tvRussianWord = findViewById(R.id.tvRussianWord);
        answerContainer = findViewById(R.id.answerContainer);
        lettersContainer = findViewById(R.id.lettersContainer);
        btnCheck = findViewById(R.id.btnCheck);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCheck.setOnClickListener(v -> checkAnswer());
        tvExplanation = findViewById(R.id.tvExplanation);
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

                if (parsed.size() >= 1) {
                    allWords.clear();
                    allWords.addAll(parsed);
                    nextQuestion();
                } else if (entities.isEmpty()) {
                    loadWordsFromSupabase();
                } else {
                    // если слова есть, но для уровня их не хватает
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

                        WordDao dao = AppDatabase.getInstance(Level7Activity.this).wordDao();
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
                            if (parsed.size() < 1) {
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
        userAnswer.setLength(0);
        userAnswerIndices.clear();
        availableLetters.clear();
        availableIndices.clear();
        allGeneratedLetters.clear();
        answerContainer.removeAllViews();
        lettersContainer.removeAllViews();

        Collections.shuffle(allWords);
        currentWord = allWords.get(0);

        correctUdmurt = currentWord.getUdmurt().toLowerCase().trim();

        tvRussianWord.setText(currentWord.getRussian());

        // буквы правильного слова
        List<Character> correctLetters = new ArrayList<>();
        for (char c : correctUdmurt.toCharArray()) {
            if (c != ' ') correctLetters.add(c);
        }

        // дополнительные буквы
        List<Character> extras = new ArrayList<>();
        for (Word w : allWords) {
            if (w == currentWord) continue;
            String s = w.getUdmurt().toLowerCase();
            for (char c : s.toCharArray()) {
                if (c != ' ' && !correctLetters.contains(c)) {
                    extras.add(c);
                }
            }
        }

        Collections.shuffle(extras);
        for (int i = 0; i < EXTRA_LETTERS && i < extras.size(); i++) {
            correctLetters.add(extras.get(i));
        }

        Collections.shuffle(correctLetters);

        // сохранение букв с их индексом
        for (int i = 0; i < correctLetters.size(); i++) {
            allGeneratedLetters.add(correctLetters.get(i));
            availableLetters.add(correctLetters.get(i));
            availableIndices.add(i); // Исходный индекс буквы
        }

        renderLetters();
        renderAnswer();

        if (tvExplanation != null) {
            tvExplanation.setVisibility(android.view.View.GONE);
            tvExplanation.setText("");
        }
    }

    private void renderLetters() {
        lettersContainer.removeAllViews();

        LinearLayout currentRow = null;
        for (int i = 0; i < availableLetters.size(); i++) {
            if (i % 6 == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 8, 0, 8);
                currentRow.setLayoutParams(rowParams);
                lettersContainer.addView(currentRow);
            }

            final char letter = availableLetters.get(i);
            TextView tv = new TextView(this);
            tv.setText(String.valueOf(letter));
            tv.setTextSize(22);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.parseColor("#E53935"));
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundResource(R.drawable.bg_card_open);
            tv.setPadding(dp(12), dp(8), dp(12), dp(8));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(4), 0, dp(4), 0);
            tv.setLayoutParams(params);

            final int index = i;
            tv.setOnClickListener(v -> onLetterClick(index));

            currentRow.addView(tv);
        }
    }

    private void renderAnswer() {
        answerContainer.removeAllViews();

        if (userAnswer.length() == 0) {
            TextView placeholder = new TextView(this);
            placeholder.setText("Нажми на буквы");
            placeholder.setTextColor(Color.parseColor("#CCCCCC"));
            placeholder.setTextSize(16);
            answerContainer.addView(placeholder);
        } else {
            for (int i = 0; i < userAnswer.length(); i++) {
                final char c = userAnswer.charAt(i);
                final int answerIndex = i;

                TextView tv = new TextView(this);
                tv.setText(String.valueOf(c));
                tv.setTextSize(22);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setTextColor(Color.parseColor("#E53935"));
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(dp(8), dp(4), dp(8), dp(4));
                // Клик по букве в ответе — возвращаем её обратно
                tv.setOnClickListener(v -> onRemoveLetter(answerIndex));

                answerContainer.addView(tv);
            }
        }
    }

    // клик по букве(добавить)
    private void onLetterClick(int index) {
        char letter = availableLetters.get(index);
        int originalIndex = availableIndices.get(index);

        // добавляем букву в ответ
        userAnswer.append(letter);
        userAnswerIndices.add(originalIndex);

        // удаление из доступных букв
        availableLetters.remove(index);
        availableIndices.remove(index);

        renderLetters();
        renderAnswer();
    }

    // клик по букве(удалить)
    private void onRemoveLetter(int answerIndex) {
        // получаем букву и ее индекс
        char letter = userAnswer.charAt(answerIndex);
        int originalIndex = userAnswerIndices.get(answerIndex);

        // удвлвение из ответа
        userAnswer.deleteCharAt(answerIndex);
        userAnswerIndices.remove(answerIndex);

        // возвращаем слово в конец
        availableLetters.add(letter);
        availableIndices.add(originalIndex);

        renderLetters();
        renderAnswer();
    }

    private void checkAnswer() {
        if (userAnswer.length() == 0) {
            if (tvExplanation != null) {
                tvExplanation.setVisibility(android.view.View.VISIBLE);
                tvExplanation.setTextColor(Color.parseColor("#DF3844"));
                tvExplanation.setText("Собери слово!");
            }
            return;
        }

        String userStr = userAnswer.toString().trim().toLowerCase();
        String correct = correctUdmurt.replace(" ", "").toLowerCase();

        if (userStr.equals(correct)) {
            if (tvExplanation != null) {
                tvExplanation.setVisibility(android.view.View.VISIBLE);
                tvExplanation.setTextColor(Color.parseColor("#33786C"));
                tvExplanation.setText("Верно! " + currentWord.getUdmurt() + " — это " + currentWord.getRussian());
            }

            SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
            prefs.edit().putInt("level_7_" + category, 1).apply();

            btnCheck.postDelayed(this::nextQuestion, 1500);
        } else {
            if (tvExplanation != null) {
                tvExplanation.setVisibility(android.view.View.VISIBLE);
                tvExplanation.setTextColor(Color.parseColor("#DF3844"));
                tvExplanation.setText("Неверно. Правильно: " + currentWord.getUdmurt());
            }

            btnCheck.postDelayed(this::nextQuestion, 2500);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
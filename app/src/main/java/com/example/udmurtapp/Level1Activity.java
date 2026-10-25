package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
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

public class Level1Activity extends AppCompatActivity {

    private static final String TAG = "LEVEL1";
    private static final int WORDS_COUNT = 4;

    private static final int COLOR_CORRECT = Color.parseColor("#33786C");
    private static final int COLOR_WRONG = Color.parseColor("#D32F2F");

    private String category = "";

    private final TextView[] leftViews = new TextView[WORDS_COUNT];
    private final TextView[] rightViews = new TextView[WORDS_COUNT];

    // массивы для точек
    private final View[] dotLeftViews = new View[WORDS_COUNT];
    private final View[] dotRightViews = new View[WORDS_COUNT];

    private LineOverlayView lineOverlay;
    private TextView tvExplanation;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private List<Word> udmurtSide = new ArrayList<>();
    private List<Word> russianSide = new ArrayList<>();

    private int selectedLeftIndex = -1;
    private final Map<Integer, Integer> userPairs = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) category = "";

        initViews();
        loadWordsFromRoom(); // сначала пробуем взять слова из локальной базы (оффлайн)
    }

    private void initViews() {
        leftViews[0] = findViewById(R.id.tvLeft1);
        leftViews[1] = findViewById(R.id.tvLeft2);
        leftViews[2] = findViewById(R.id.tvLeft3);
        leftViews[3] = findViewById(R.id.tvLeft4);

        rightViews[0] = findViewById(R.id.tvRight1);
        rightViews[1] = findViewById(R.id.tvRight2);
        rightViews[2] = findViewById(R.id.tvRight3);
        rightViews[3] = findViewById(R.id.tvRight4);

        dotLeftViews[0] = findViewById(R.id.dotLeft1);
        dotLeftViews[1] = findViewById(R.id.dotLeft2);
        dotLeftViews[2] = findViewById(R.id.dotLeft3);
        dotLeftViews[3] = findViewById(R.id.dotLeft4);

        dotRightViews[0] = findViewById(R.id.dotRight1);
        dotRightViews[1] = findViewById(R.id.dotRight2);
        dotRightViews[2] = findViewById(R.id.dotRight3);
        dotRightViews[3] = findViewById(R.id.dotRight4);

        lineOverlay = findViewById(R.id.lineOverlay);
        tvExplanation = findViewById(R.id.tvExplanation);

        for (int i = 0; i < leftViews.length; i++) {
            final int index = i;
            leftViews[i].setOnClickListener(v -> onLeftWordClick(index));
            dotLeftViews[i].setOnClickListener(v -> onLeftWordClick(index));
        }
        for (int i = 0; i < rightViews.length; i++) {
            final int index = i;
            rightViews[i].setOnClickListener(v -> onRightWordClick(index));
            dotRightViews[i].setOnClickListener(v -> onRightWordClick(index));
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnCheck = findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(v -> checkAnswers());
    }

    // берет слова из локальной базы, если слов нет - берем из SupaBase(при наличии интернета)
    private void loadWordsFromRoom() {
        new Thread(() -> {
            List<WordEntity> entities = AppDatabase.getInstance(this).wordDao().getAllWords();
            List<Word> allWords = new ArrayList<>();
            for (WordEntity e : entities) {
                // Фильтр по категории
                if (category.isEmpty() || (e.category != null && e.category.equalsIgnoreCase(category))) {
                    allWords.add(new Word(
                            e.serverId != null ? e.serverId : "",
                            e.udmurt, e.russian, e.category,
                            e.imageUrl, e.audioUrl, e.example, e.exampleRus
                    ));
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (allWords.size() >= WORDS_COUNT) {
                    // если слов достаточно можно запустить уровень
                    setupLevel(allWords);
                } else if (entities.isEmpty()) {
                    // если база пуста, пробуем скачать из SupaBase(только с наличием интернета)
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

                        WordDao dao = AppDatabase.getInstance(Level1Activity.this).wordDao();
                        dao.deleteAll();
                        dao.insertAll(entities);

                        // Перечитываем из локальной базы (Room) и фильтруем по категории
                        List<WordEntity> saved = dao.getAllWords();
                        List<Word> allWords = new ArrayList<>();
                        for (WordEntity e : saved) {
                            if (category.isEmpty() || (e.category != null && e.category.equalsIgnoreCase(category))) {
                                allWords.add(new Word(
                                        e.serverId != null ? e.serverId : "",
                                        e.udmurt, e.russian, e.category,
                                        e.imageUrl, e.audioUrl, e.example, e.exampleRus
                                ));
                            }
                        }

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (allWords.size() < WORDS_COUNT) {
                                finish();
                                return;
                            }
                            setupLevel(allWords);
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


    private void setupLevel(List<Word> allWords) {
        Collections.shuffle(allWords);
        List<Word> selected = new ArrayList<>(allWords.subList(0, WORDS_COUNT));

        udmurtSide = new ArrayList<>(selected);
        russianSide = new ArrayList<>(selected);

        Collections.shuffle(udmurtSide);
        Collections.shuffle(russianSide);

        for (int i = 0; i < WORDS_COUNT; i++) {
            leftViews[i].setText(udmurtSide.get(i).getUdmurt());
            rightViews[i].setText(russianSide.get(i).getRussian());
        }

        userPairs.clear();
        selectedLeftIndex = -1;
        if (lineOverlay != null) lineOverlay.clearConnections();

        for (int i = 0; i < WORDS_COUNT; i++) {
            resetLeftVisual(i);
            resetRightVisual(i);
        }

        if (tvExplanation != null) {
            tvExplanation.setVisibility(View.GONE);
            tvExplanation.setText("");
        }
    }

    private void onLeftWordClick(int index) {
        if (userPairs.containsKey(index)) {
            int rightIdx = userPairs.remove(index);
            resetRightVisual(rightIdx);
            resetLeftVisual(index);
            selectedLeftIndex = -1;
            redrawLines();
            return;
        }
        if (selectedLeftIndex != -1 && selectedLeftIndex != index) {
            resetLeftVisual(selectedLeftIndex);
        }
        selectedLeftIndex = index;
        highlightLeft(index);
    }

    private void onRightWordClick(int index) {
        if (selectedLeftIndex == -1) {
            return;
        }
        Integer oldLeft = null;
        for (Map.Entry<Integer, Integer> e : userPairs.entrySet()) {
            if (e.getValue() == index) {
                oldLeft = e.getKey();
                break;
            }
        }
        if (oldLeft != null) {
            userPairs.remove(oldLeft);
            resetLeftVisual(oldLeft);
        }

        userPairs.put(selectedLeftIndex, index);
        highlightRight(index);
        resetLeftVisual(selectedLeftIndex);
        selectedLeftIndex = -1;

        redrawLines();
    }

    private void redrawLines() {
        if (lineOverlay == null) return;
        lineOverlay.clearConnections();

        int[] overlayLoc = new int[2];
        lineOverlay.getLocationOnScreen(overlayLoc);

        for (Map.Entry<Integer, Integer> entry : userPairs.entrySet()) {
            int leftIdx = entry.getKey();
            int rightIdx = entry.getValue();

            View leftDot = dotLeftViews[leftIdx];
            View rightDot = dotRightViews[rightIdx];

            int[] leftLoc = new int[2];
            leftDot.getLocationOnScreen(leftLoc);
            int[] rightLoc = new int[2];
            rightDot.getLocationOnScreen(rightLoc);

            float startX = (leftLoc[0] - overlayLoc[0]) + (leftDot.getWidth() / 2f);
            float startY = (leftLoc[1] - overlayLoc[1]) + (leftDot.getHeight() / 2f);

            float endX = (rightLoc[0] - overlayLoc[0]) + (rightDot.getWidth() / 2f);
            float endY = (rightLoc[1] - overlayLoc[1]) + (rightDot.getHeight() / 2f);

            lineOverlay.addConnection(startX, startY, endX, endY);
        }
    }

    private void checkAnswers() {
        if (userPairs.size() < WORDS_COUNT) {
            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setTextColor(COLOR_WRONG);
                tvExplanation.setText("Соедините все слова!");

                // скрываем объяснение через 2 сек
                handler.postDelayed(() -> {
                    if (tvExplanation != null) {
                        tvExplanation.setVisibility(View.GONE);
                        tvExplanation.setText("");
                    }
                }, 2000);
            }
            return;
        }

        boolean allCorrect = true;
        StringBuilder errorText = new StringBuilder();
        errorText.append("Есть ошибки:\n");

        for (Map.Entry<Integer, Integer> entry : userPairs.entrySet()) {
            int leftIdx = entry.getKey();
            int rightIdx = entry.getValue();

            Word leftWord = udmurtSide.get(leftIdx);
            Word rightWord = russianSide.get(rightIdx);

            if (!leftWord.getId().equals(rightWord.getId())) {
                allCorrect = false;
                errorText.append("• «").append(leftWord.getUdmurt())
                        .append("» — это не «").append(rightWord.getRussian()).append("»\n");
            }
        }

        if (allCorrect) {
            for (int i = 0; i < WORDS_COUNT; i++) {
                setCardBackground(leftViews[i], COLOR_CORRECT);
                leftViews[i].setTextColor(Color.WHITE);
                setCardBackground(rightViews[i], COLOR_CORRECT);
                rightViews[i].setTextColor(Color.WHITE);
            }

            SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
            prefs.edit().putInt("level_1_" + category, 1).apply();

            // автоматический переход к другому уровню игры
            handler.postDelayed(
                    this::loadWordsFromRoom,
                    2000
            );
        } else {
            // если есть ошибки - показываем какие пары не совпали
            userPairs.clear();
            selectedLeftIndex = -1;

            for (int i = 0; i < WORDS_COUNT; i++) {
                resetLeftVisual(i);
                resetRightVisual(i);
            }
            if (lineOverlay != null) lineOverlay.clearConnections();

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setTextColor(COLOR_WRONG);
                tvExplanation.setText(errorText.toString().trim());

                // скрываем объяснение через 3 секунды
                handler.postDelayed(() -> {
                    if (tvExplanation != null) {
                        tvExplanation.setVisibility(View.GONE);
                        tvExplanation.setText("");
                    }
                }, 3000);
            }
        }
    }

    private void setCardBackground(TextView tv, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(16));
        bg.setColor(color);
        tv.setBackground(bg);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // Визуал

    private void highlightLeft(int index) {
        setCardBackground(leftViews[index], COLOR_WRONG);
        leftViews[index].setTextColor(Color.WHITE);
        dotLeftViews[index].setBackgroundResource(R.drawable.bg_dot_red);
    }

    private void resetLeftVisual(int index) {
        setCardBackground(leftViews[index], Color.WHITE);
        leftViews[index].setTextColor(Color.parseColor("#333333"));
        dotLeftViews[index].setBackgroundResource(R.drawable.bg_dot_red);
    }

    private void highlightRight(int index) {
        setCardBackground(rightViews[index], COLOR_WRONG);
        rightViews[index].setTextColor(Color.WHITE);
        dotRightViews[index].setBackgroundResource(R.drawable.bg_dot_red);
    }

    private void resetRightVisual(int index) {
        setCardBackground(rightViews[index], Color.WHITE);
        rightViews[index].setTextColor(Color.parseColor("#333333"));
        boolean isConnected = userPairs.containsValue(index);
        if (!isConnected) {
            dotRightViews[index].setBackgroundResource(R.drawable.bg_dot_grey);
        } else {
            dotRightViews[index].setBackgroundResource(R.drawable.bg_dot_red);
        }
    }
}
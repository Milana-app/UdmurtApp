package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Level3Activity extends AppCompatActivity {

    private static final String TAG = "LEVEL3";
    private static final int WORDS_PER_CATEGORY = 3;
    private static final int TOTAL_WORDS = WORDS_PER_CATEGORY * 2;

    private static final int COLOR_CORRECT = Color.parseColor("#2D2D2D");
    private static final int COLOR_WRONG = Color.parseColor("#D32F2F");

    private String chosenCategory = "";

    private CardView categoryBox1, categoryBox2;
    private LinearLayout categoryContainer1, categoryContainer2;
    private TextView categoryTitle1, categoryTitle2;
    private LinearLayout wordsContainer;
    private TextView tvExplanation;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<Word> allWords = new ArrayList<>();

    private String category1Name, category2Name;
    private final Map<String, String> wordToCategory = new HashMap<>();
    private final Map<String, String> userPlacement = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level3);

        chosenCategory = getIntent().getStringExtra("CATEGORY");
        if (chosenCategory == null) chosenCategory = "";

        initViews();
        loadWordsFromRoom();
    }

    private void initViews() {
        categoryBox1 = findViewById(R.id.categoryBox1);
        categoryBox2 = findViewById(R.id.categoryBox2);
        categoryContainer1 = findViewById(R.id.categoryContainer1);
        categoryContainer2 = findViewById(R.id.categoryContainer2);
        categoryTitle1 = findViewById(R.id.categoryTitle1);
        categoryTitle2 = findViewById(R.id.categoryTitle2);
        wordsContainer = findViewById(R.id.wordsContainer);
        tvExplanation = findViewById(R.id.tvExplanation);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        Button btnCheck = findViewById(R.id.btnCheck);
        if (btnCheck != null) btnCheck.setOnClickListener(v -> checkAnswers());

        setupDropTarget(categoryBox1, 1);
        setupDropTarget(categoryBox2, 2);
        setupDropTargetForWordsContainer();
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

                if (parsed.size() >= TOTAL_WORDS) {
                    allWords.clear();
                    allWords.addAll(parsed);
                    setupLevel();
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

                        WordDao dao = AppDatabase.getInstance(Level3Activity.this).wordDao();
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
                            if (parsed.size() < TOTAL_WORDS) {
                                finish();
                                return;
                            }
                            allWords.clear();
                            allWords.addAll(parsed);
                            setupLevel();
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

    private void setupLevel() {
        Map<String, List<Word>> byCategory = new HashMap<>();
        for (Word w : allWords) {
            if (!byCategory.containsKey(w.getCategory())) {
                byCategory.put(w.getCategory(), new ArrayList<>());
            }
            byCategory.get(w.getCategory()).add(w);
        }

        List<String> validCategories = new ArrayList<>();
        for (Map.Entry<String, List<Word>> e : byCategory.entrySet()) {
            if (e.getValue().size() >= WORDS_PER_CATEGORY) {
                validCategories.add(e.getKey());
            }
        }

        if (validCategories.size() < 2) {
            finish();
            return;
        }

        if (!chosenCategory.isEmpty() && validCategories.contains(chosenCategory)) {
            category1Name = chosenCategory;
            String pairCategory = findSimilarCategory(chosenCategory, validCategories);
            if (pairCategory != null) {
                category2Name = pairCategory;
            } else {
                List<String> others = new ArrayList<>(validCategories);
                others.remove(category1Name);
                Collections.shuffle(others);
                category2Name = others.get(0);
            }
        } else {
            String[] pair = pickSimilarPair(validCategories);
            if (pair == null) {
                Collections.shuffle(validCategories);
                category1Name = validCategories.get(0);
                category2Name = validCategories.get(1);
            } else {
                category1Name = pair[0];
                category2Name = pair[1];
            }
        }

        categoryTitle1.setText(category1Name);
        categoryTitle2.setText(category2Name);

        wordToCategory.clear();
        userPlacement.clear();
        wordsContainer.removeAllViews();
        categoryContainer1.removeAllViews();
        categoryContainer2.removeAllViews();

        List<Word> selectedWords = new ArrayList<>();
        selectedWords.addAll(pickRandom(byCategory.get(category1Name), WORDS_PER_CATEGORY));
        selectedWords.addAll(pickRandom(byCategory.get(category2Name), WORDS_PER_CATEGORY));

        for (Word w : selectedWords) {
            wordToCategory.put(w.getUdmurt(), w.getCategory());
        }

        Collections.shuffle(selectedWords);

        buildWordsGrid(selectedWords);

        if (tvExplanation != null) {
            tvExplanation.setVisibility(View.GONE);
            tvExplanation.setText("");
        }
    }

    private void buildWordsGrid(List<Word> words) {
        wordsContainer.removeAllViews();

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padding = dp(40); // Отступы по бокам
        int availableWidth = screenWidth - padding;

        LinearLayout currentRow = createNewRow();
        int currentWidth = 0;

        for (Word w : words) {
            String wordText = w.getUdmurt();
            CardView chip = createWordChipView(wordText);

            chip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int chipWidth = chip.getMeasuredWidth() + dp(8); // + отступы

            if (currentWidth + chipWidth > availableWidth && currentWidth > 0) {
                wordsContainer.addView(currentRow);
                currentRow = createNewRow();
                currentWidth = 0;
            }

            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(48));
            chipParams.setMargins(dp(4), 0, dp(4), 0);

            chip.setLayoutParams(chipParams);
            currentRow.addView(chip);
            currentWidth += chipWidth;
        }

        if (currentRow.getChildCount() > 0) {
            wordsContainer.addView(currentRow);
        }
    }

    private LinearLayout createNewRow() {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_HORIZONTAL);
        return row;
    }

    private CardView createWordChipView(String wordText) {
        CardView chipCard = new CardView(this);
        chipCard.setRadius(dp(12));
        chipCard.setCardElevation(0);
        chipCard.setCardBackgroundColor(Color.parseColor("#F8F1EA"));

        TextView chip = new TextView(this);
        chip.setText(wordText);
        chip.setTextSize(16);
        chip.setTextColor(Color.parseColor("#1A1A1A"));
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);

        // центрирование слова
        chip.setPadding(dp(16), dp(8), dp(16), dp(8));

        chipCard.addView(chip, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        chipCard.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(chipCard);
                chipCard.startDragAndDrop(null, shadow, chipCard, 0);
                return true;
            }
            return false;
        });

        return chipCard;
    }

    private void setupDropTarget(CardView box, int categoryNumber) {
        box.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    box.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    box.setCardBackgroundColor(Color.WHITE);
                    return true;

                case DragEvent.ACTION_DROP: {
                    box.setCardBackgroundColor(Color.WHITE);
                    View dragged = (View) event.getLocalState();

                    if (dragged instanceof CardView) {
                        CardView card = (CardView) dragged;
                        TextView innerText = (TextView) card.getChildAt(0);
                        String wordText = innerText.getText().toString();

                        ViewGroup oldParent = (ViewGroup) card.getParent();
                        if (oldParent != null) oldParent.removeView(card);

                        LinearLayout targetContainer = (categoryNumber == 1)
                                ? categoryContainer1 : categoryContainer2;

                        userPlacement.put(wordText,
                                categoryNumber == 1 ? category1Name : category2Name);

                        CardView newChip = new CardView(this);
                        newChip.setRadius(dp(12));
                        newChip.setCardElevation(0);
                        newChip.setCardBackgroundColor(Color.parseColor("#F8F1EA"));

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 0, dp(8));
                        newChip.setLayoutParams(params);

                        TextView chipText = new TextView(this);
                        chipText.setText(wordText);
                        chipText.setTextSize(16);
                        chipText.setTextColor(Color.parseColor("#1A1A1A"));
                        chipText.setTypeface(null, android.graphics.Typeface.BOLD);
                        chipText.setGravity(Gravity.CENTER);
                        // отступы для длинных слов
                        chipText.setPadding(dp(12), dp(12), dp(12), dp(12));

                        newChip.addView(chipText);

                        newChip.setOnTouchListener((v2, e) -> {
                            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                                View.DragShadowBuilder shadow = new View.DragShadowBuilder(newChip);
                                newChip.startDragAndDrop(null, shadow, newChip, 0);
                                return true;
                            }
                            return false;
                        });

                        targetContainer.addView(newChip);
                    }
                    return true;
                }

                case DragEvent.ACTION_DRAG_ENDED:
                    box.setCardBackgroundColor(Color.WHITE);
                    return true;
            }
            return false;
        });
    }

    private void setupDropTargetForWordsContainer() {
        wordsContainer.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    wordsContainer.setBackgroundColor(Color.parseColor("#E0E0E0"));
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    wordsContainer.setBackgroundColor(Color.TRANSPARENT);
                    return true;

                case DragEvent.ACTION_DROP: {
                    wordsContainer.setBackgroundColor(Color.TRANSPARENT);
                    View dragged = (View) event.getLocalState();

                    if (dragged instanceof CardView) {
                        CardView card = (CardView) dragged;
                        TextView innerText = (TextView) card.getChildAt(0);
                        String wordText = innerText.getText().toString();

                        ViewGroup oldParent = (ViewGroup) card.getParent();
                        if (oldParent != null) oldParent.removeView(card);

                        userPlacement.remove(wordText);

                        List<Word> currentWords = new ArrayList<>();
                        for (String w : wordToCategory.keySet()) {
                            if (!userPlacement.containsKey(w)) {
                                for (Word aw : allWords) {
                                    if (aw.getUdmurt().equals(w)) {
                                        currentWords.add(aw);
                                        break;
                                    }
                                }
                            }
                        }
                        buildWordsGrid(currentWords);
                    }
                    return true;
                }

                case DragEvent.ACTION_DRAG_ENDED:
                    wordsContainer.setBackgroundColor(Color.TRANSPARENT);
                    return true;
            }
            return false;
        });
    }

    // проверка
    private void checkAnswers() {
        if (userPlacement.size() < TOTAL_WORDS) {
            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setTextColor(COLOR_WRONG);
                tvExplanation.setText("Распределите все слова!");

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

        for (Map.Entry<String, String> entry : userPlacement.entrySet()) {
            String word = entry.getKey();
            String userCategory = entry.getValue();
            String correctCategory = wordToCategory.get(word);
            if (!userCategory.equals(correctCategory)) {
                allCorrect = false;
                errorText.append("• «").append(word).append("» — это не «").append(userCategory).append("»\n");
            }
        }

        if (allCorrect) {
            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setTextColor(COLOR_CORRECT);
                tvExplanation.setText("Всё верно! Уровень пройден");
            }

            SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
            prefs.edit().putInt("level_3_" + chosenCategory, 1).apply();

            handler.postDelayed(this::recreate, 2000);
        } else {
            userPlacement.clear();
            categoryContainer1.removeAllViews();
            categoryContainer2.removeAllViews();
            wordsContainer.removeAllViews();

            List<Word> words = new ArrayList<>();
            for (String w : wordToCategory.keySet()) {
                for (Word aw : allWords) {
                    if (aw.getUdmurt().equals(w)) {
                        words.add(aw);
                        break;
                    }
                }
            }
            Collections.shuffle(words);
            buildWordsGrid(words);

            if (tvExplanation != null) {
                tvExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setTextColor(COLOR_WRONG);
                tvExplanation.setText(errorText.toString().trim());

                handler.postDelayed(() -> {
                    if (tvExplanation != null) {
                        tvExplanation.setVisibility(View.GONE);
                        tvExplanation.setText("");
                    }
                }, 3000);
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
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

    private String[] pickSimilarPair(List<String> available) {
        String[][] similarPairs = {
                {"Овощи", "Ягоды"},
                {"Дикие животные", "Домашние животные"},
                {"Еда", "Овощи"},
                {"Еда", "Ягоды"},
                {"Одежда", "Предметы"},
                {"Природа", "Времена года"},
                {"Моя семья", "Фразы"},
                {"Числа", "Цвета"},
                {"Природа", "Дикие животные"},
                {"Природа", "Домашние животные"},
                {"Предметы", "Одежда"},
                {"Фразы", "Моя семья"}
        };

        List<String[]> shuffled = new ArrayList<>();
        Collections.addAll(shuffled, similarPairs);
        Collections.shuffle(shuffled);

        for (String[] p : shuffled) {
            if (available.contains(p[0]) && available.contains(p[1])) {
                return p;
            }
        }
        return null;
    }

    private List<Word> pickRandom(List<Word> list, int count) {
        if (list == null) return new ArrayList<>();
        List<Word> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.subList(0, Math.min(count, copy.size()));
    }
}
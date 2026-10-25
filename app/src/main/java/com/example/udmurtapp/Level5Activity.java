package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Level5Activity extends AppCompatActivity {

    private static final String TAG = "LEVEL5";
    private static final int PAIRS_COUNT = 6;
    private static final int CARD_COUNT = PAIRS_COUNT * 2;

    private String chosenCategory = "";

    private final CardView[] cards = new CardView[CARD_COUNT];
    private final List<Word> allWords = new ArrayList<>();
    private final List<CardData> cardDataList = new ArrayList<>();
    private TextView tvExplanation;

    private final List<Integer> openedCards = new ArrayList<>();
    private final List<Integer> matchedCards = new ArrayList<>();

    private boolean isBusy = false;
    private final Handler handler = new Handler();

    // класс для хранения данных одной карточки
    static class CardData {
        String word;
        int pairId;
        boolean isUdmurt;
        CardData(String word, int pairId, boolean isUdmurt) {
            this.word = word;
            this.pairId = pairId;
            this.isUdmurt = isUdmurt;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level5);

        chosenCategory = getIntent().getStringExtra("CATEGORY");
        if (chosenCategory == null) chosenCategory = "";

        initViews();
        loadWordsFromRoom();
    }

    private void initViews() {
        cards[0] = new CardView(findViewById(R.id.card1), findViewById(R.id.card1Image), findViewById(R.id.card1Text));
        cards[1] = new CardView(findViewById(R.id.card2), findViewById(R.id.card2Image), findViewById(R.id.card2Text));
        cards[2] = new CardView(findViewById(R.id.card3), findViewById(R.id.card3Image), findViewById(R.id.card3Text));
        cards[3] = new CardView(findViewById(R.id.card4), findViewById(R.id.card4Image), findViewById(R.id.card4Text));
        cards[4] = new CardView(findViewById(R.id.card5), findViewById(R.id.card5Image), findViewById(R.id.card5Text));
        cards[5] = new CardView(findViewById(R.id.card6), findViewById(R.id.card6Image), findViewById(R.id.card6Text));
        cards[6] = new CardView(findViewById(R.id.card7), findViewById(R.id.card7Image), findViewById(R.id.card7Text));
        cards[7] = new CardView(findViewById(R.id.card8), findViewById(R.id.card8Image), findViewById(R.id.card8Text));
        cards[8] = new CardView(findViewById(R.id.card9), findViewById(R.id.card9Image), findViewById(R.id.card9Text));
        cards[9] = new CardView(findViewById(R.id.card10), findViewById(R.id.card10Image), findViewById(R.id.card10Text));
        cards[10] = new CardView(findViewById(R.id.card11), findViewById(R.id.card11Image), findViewById(R.id.card11Text));
        cards[11] = new CardView(findViewById(R.id.card12), findViewById(R.id.card12Image), findViewById(R.id.card12Text));

        // обработчик нажатия на каждую карточку
        for (int i = 0; i < cards.length; i++) {
            final int index = i;
            cards[i].root.setOnClickListener(v -> onCardClick(index));
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvExplanation = findViewById(R.id.tvExplanation);
    }

    private void loadWordsFromRoom() {
        new Thread(() -> {
            List<WordEntity> entities = AppDatabase.getInstance(this).wordDao().getAllWords();
            List<Word> parsed = new ArrayList<>();
            for (WordEntity e : entities) {
                if (chosenCategory.isEmpty() || (e.category != null && e.category.equalsIgnoreCase(chosenCategory))) {
                    parsed.add(new Word(
                            e.serverId != null ? e.serverId : "",
                            e.udmurt, e.russian, e.category,
                            e.imageUrl, e.audioUrl, e.example, e.exampleRus
                    ));
                }
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (parsed.size() >= PAIRS_COUNT) {
                    allWords.clear();
                    allWords.addAll(parsed);
                    setupGame();
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

                        WordDao dao = AppDatabase.getInstance(Level5Activity.this).wordDao();
                        dao.deleteAll();
                        dao.insertAll(entities);

                        List<WordEntity> saved = dao.getAllWords();
                        List<Word> parsed = new ArrayList<>();
                        for (WordEntity e : saved) {
                            if (chosenCategory.isEmpty() || (e.category != null && e.category.equalsIgnoreCase(chosenCategory))) {
                                parsed.add(new Word(
                                        e.serverId != null ? e.serverId : "",
                                        e.udmurt, e.russian, e.category,
                                        e.imageUrl, e.audioUrl, e.example, e.exampleRus
                                ));
                            }
                        }

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (parsed.size() < PAIRS_COUNT) {
                                finish();
                                return;
                            }
                            allWords.clear();
                            allWords.addAll(parsed);
                            setupGame();
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

    private void setupGame() {
        // перемешиваем все слова
        Collections.shuffle(allWords);
        List<Word> selected = new ArrayList<>(allWords.subList(0, PAIRS_COUNT));

        cardDataList.clear();
        for (int i = 0; i < selected.size(); i++) {
            Word w = selected.get(i);
            cardDataList.add(new CardData(w.getUdmurt(), i, true));
            cardDataList.add(new CardData(w.getRussian(), i, false));
        }

        Collections.shuffle(cardDataList);

        openedCards.clear();
        matchedCards.clear();
        isBusy = false;

        // закрываем все карточки в начале игры
        for (int i = 0; i < CARD_COUNT; i++) {
            closeCard(i);
        }

        if (tvExplanation != null) {
            tvExplanation.setVisibility(View.GONE);
            tvExplanation.setText("");
        }
    }

    // закрыть карточку
    private void closeCard(int index) {
        CardView card = cards[index];
        card.image.setVisibility(View.VISIBLE);
        card.text.setVisibility(View.GONE);
        card.text.setText("");
        card.root.setBackgroundResource(R.drawable.bg_card_closed);
    }

    // открыть карточку
    private void openCard(int index, boolean highlightGreen) {
        CardView card = cards[index];
        CardData data = cardDataList.get(index);

        card.image.setVisibility(View.GONE);
        card.text.setVisibility(View.VISIBLE);
        card.text.setText(data.word);

        if (highlightGreen) {
            card.root.setBackgroundResource(R.drawable.bg_card_matched);
        } else {
            card.root.setBackgroundResource(R.drawable.bg_card_open);
        }
    }

    // обработка клика
    private void onCardClick(int index) {
        if (isBusy) return; // ожидаем завершения анимации
        if (matchedCards.contains(index)) return; // Карточка уже найдена
        if (openedCards.contains(index)) return; // Карточка уже открыта

        openCard(index, false);
        openedCards.add(index);

        // проверяем на совпадение
        if (openedCards.size() == 2) {
            isBusy = true;
            int first = openedCards.get(0);
            int second = openedCards.get(1);

            CardData d1 = cardDataList.get(first);
            CardData d2 = cardDataList.get(second);

            // карточки совпадают (слово + перевод)
            if (d1.pairId == d2.pairId && d1.isUdmurt != d2.isUdmurt) {
                handler.postDelayed(() -> {
                    openCard(first, true);
                    openCard(second, true);
                    matchedCards.add(first);
                    matchedCards.add(second);
                    openedCards.clear();
                    isBusy = false;

                    if (tvExplanation != null) {
                        tvExplanation.setVisibility(View.VISIBLE);
                        tvExplanation.setTextColor(Color.parseColor("#33786C"));
                        tvExplanation.setText("Пара найдена! " + d1.word + " — " + d2.word);
                    }

                    if (matchedCards.size() == CARD_COUNT) {
                        onAllPairsFound();
                    }
                }, 600);
            } else {
                // не совпали - переворачиваем назад
                handler.postDelayed(() -> {
                    closeCard(first);
                    closeCard(second);
                    openedCards.clear();
                    isBusy = false;

                    if (tvExplanation != null) {
                        tvExplanation.setVisibility(View.VISIBLE);
                        tvExplanation.setTextColor(Color.parseColor("#DF3844"));
                        tvExplanation.setText("Не совпало. Попробуйте другую пару.");
                    }
                }, 1000);
            }
        }
    }

    private void onAllPairsFound() {
        if (tvExplanation != null) {
            tvExplanation.setVisibility(View.VISIBLE);
            tvExplanation.setTextColor(Color.parseColor("#33786C"));
            tvExplanation.setText("Все пары найдены! 🎉");
        }

        SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        prefs.edit().putInt("level_5_" + chosenCategory, 1).apply();

        // автоматический переход к следующему уровню(через 2 сек)
        handler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                loadWordsFromRoom();
            }
        }, 2000);
    }

    // вспомогательный класс для хранения ссылок
    static class CardView {
        View root;
        ImageView image;
        TextView text;
        CardView(View root, ImageView image, TextView text) {
            this.root = root;
            this.image = image;
            this.text = text;
        }
    }
}
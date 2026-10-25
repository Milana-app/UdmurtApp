package com.example.udmurtapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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

public class LessonActivity extends AppCompatActivity {

    private static final String TAG = "LESSON";
    private String category = "";
    private int lessonNumber = 1;
    private List<Word> lessonWords = new ArrayList<>();
    private int currentWordIndex = 0;

    private int correctCount = 0;
    private int wrongCount = 0;

    private String selectedOption = null;
    private int selectedIndex = -1;
    private boolean isAnswered = false;

    private final TextView[] leftViews = new TextView[4];
    private final TextView[] rightViews = new TextView[4];

    private final View[] dotLeftViews = new View[4];
    private final View[] dotRightViews = new View[4];

    private LineOverlayView lineOverlay;
    private int selectedLeftIndex = -1;
    private final Map<Integer, Integer> userPairs = new HashMap<>();
    private List<Word> udmurtSide = new ArrayList<>();
    private List<Word> russianSide = new ArrayList<>();

    private TextView tvWordTrueFalse;
    private TextView tvTranslationHint;
    private Button btnTrue, btnFalse, btnActionTF;
    private boolean isStatementTrue;
    private Boolean userAnswerTF = null;

    private final List<Integer> openedCards = new ArrayList<>();
    private final List<Integer> matchedCards = new ArrayList<>();
    private boolean isBusy = false;
    private final Handler handler = new Handler();
    private final List<MemeCard> memeCards = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        category = getIntent().getStringExtra("CATEGORY");
        if (category == null) category = "";

        lessonNumber = getIntent().getIntExtra("LESSON_NUMBER", 1);

        loadWords();
    }

    private void loadWords() {
        WordRepository repository = new WordRepository(this);

        repository.getWords(category, new WordRepository.WordsCallback() {
            @Override
            public void onSuccess(List<WordEntity> wordEntities) {
                if (wordEntities.size() < 4) {
                    finish(); return;
                }

                List<Word> allWords = new ArrayList<>();
                for (WordEntity e : wordEntities) {
                    allWords.add(new Word(e.serverId, e.udmurt, e.russian, e.category, e.imageUrl, e.audioUrl, e.example, e.exampleRus));
                }

                getOrCreateLessonWords(allWords);
                startStep1();
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> finish());
            }
        });
    }

    private void getOrCreateLessonWords(List<Word> allWords) {
        SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        String key = "lesson_words_" + category + "_" + lessonNumber;
        String savedIds = prefs.getString(key, "");

        lessonWords.clear();

        if (savedIds.isEmpty()) {
            Collections.shuffle(allWords);
            for (int i = 0; i < 4 && i < allWords.size(); i++) {
                lessonWords.add(allWords.get(i));
            }

            StringBuilder sb = new StringBuilder();
            for (Word w : lessonWords) sb.append(w.getId()).append(",");
            prefs.edit().putString(key, sb.toString()).apply();
        } else {
            String[] ids = savedIds.split(",");
            for (String id : ids) {
                if (id.isEmpty()) continue;
                for (Word w : allWords) {
                    if (w.getId().equals(id)) {
                        lessonWords.add(w);
                        break;
                    }
                }
            }
            if (lessonWords.size() < 4) {
                lessonWords.clear();
                Collections.shuffle(allWords);
                for (int i = 0; i < 4 && i < allWords.size(); i++) {
                    lessonWords.add(allWords.get(i));
                }
                StringBuilder sb = new StringBuilder();
                for (Word w : lessonWords) sb.append(w.getId()).append(",");
                prefs.edit().putString(key, sb.toString()).apply();
            }
        }
    }

    // 1. изучение слов
    private void startStep1() {
        setContentView(R.layout.activity_lesson_study);
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        TextView[] udmurt = {findViewById(R.id.studyUdmurt1), findViewById(R.id.studyUdmurt2), findViewById(R.id.studyUdmurt3), findViewById(R.id.studyUdmurt4)};
        TextView[] russian = {findViewById(R.id.studyRussian1), findViewById(R.id.studyRussian2), findViewById(R.id.studyRussian3), findViewById(R.id.studyRussian4)};

        for (int i = 0; i < 4; i++) {
            if (i < lessonWords.size()) {
                if (udmurt[i] != null) udmurt[i].setText(lessonWords.get(i).getUdmurt());
                if (russian[i] != null) russian[i].setText(lessonWords.get(i).getRussian());
            }
        }

        View btnContinue = findViewById(R.id.btnContinue);
        if (btnContinue != null) btnContinue.setOnClickListener(v -> startStep2());
    }

    // 2. соединение слова с переводом
    private void startStep2() {
        setContentView(R.layout.activity_level1);
        ImageView back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());

        lineOverlay = findViewById(R.id.lineOverlay);
        if (lineOverlay != null) lineOverlay.clearConnections();

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

        udmurtSide = new ArrayList<>(lessonWords);
        russianSide = new ArrayList<>(lessonWords);
        Collections.shuffle(udmurtSide);
        Collections.shuffle(russianSide);

        for (int i = 0; i < 4; i++) {
            if (i < udmurtSide.size()) {
                leftViews[i].setText(udmurtSide.get(i).getUdmurt());
                rightViews[i].setText(russianSide.get(i).getRussian());
            }
            leftViews[i].setBackgroundResource(R.drawable.bg_card_closed);
            leftViews[i].setTextColor(Color.parseColor("#333333"));
            rightViews[i].setBackgroundResource(R.drawable.bg_card_closed);
            rightViews[i].setTextColor(Color.parseColor("#333333"));

            if (dotLeftViews[i] != null) dotLeftViews[i].setBackgroundResource(R.drawable.bg_dot_red);
            if (dotRightViews[i] != null) dotRightViews[i].setBackgroundResource(R.drawable.bg_dot_grey);

            final int index = i;
            leftViews[i].setOnClickListener(v -> onLeftWordClick(index));
            rightViews[i].setOnClickListener(v -> onRightWordClick(index));
        }

        userPairs.clear();
        selectedLeftIndex = -1;

        Button btn = findViewById(R.id.btnCheck);
        if (btn != null) {
            btn.setText("Проверить");
            btn.setOnClickListener(v -> checkStep2());
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
        leftViews[index].setBackgroundResource(R.drawable.bg_button_check);
        leftViews[index].setTextColor(Color.WHITE);
    }

    private void onRightWordClick(int index) {
        if (selectedLeftIndex == -1) return;

        Integer oldLeft = null;
        for (Map.Entry<Integer, Integer> e : userPairs.entrySet()) {
            if (e.getValue() == index) { oldLeft = e.getKey(); break; }
        }
        if (oldLeft != null) {
            userPairs.remove(oldLeft);
            resetLeftVisual(oldLeft);
        }
        userPairs.put(selectedLeftIndex, index);
        rightViews[index].setBackgroundResource(R.drawable.bg_button_check);
        rightViews[index].setTextColor(Color.WHITE);

        if (dotRightViews[index] != null) {
            dotRightViews[index].setBackgroundResource(R.drawable.bg_dot_red);
        }

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

            if (leftDot == null || rightDot == null) continue;

            int[] leftLoc = new int[2]; leftDot.getLocationOnScreen(leftLoc);
            int[] rightLoc = new int[2]; rightDot.getLocationOnScreen(rightLoc);

            float startX = (leftLoc[0] - overlayLoc[0]) + (leftDot.getWidth() / 2f);
            float startY = (leftLoc[1] - overlayLoc[1]) + (leftDot.getHeight() / 2f);
            float endX = (rightLoc[0] - overlayLoc[0]) + (rightDot.getWidth() / 2f);
            float endY = (rightLoc[1] - overlayLoc[1]) + (rightDot.getHeight() / 2f);

            lineOverlay.addConnection(startX, startY, endX, endY);
        }
    }

    private void checkStep2() {
        if (userPairs.size() < 4) return;

        boolean allCorrect = true;
        for (Map.Entry<Integer, Integer> entry : userPairs.entrySet()) {
            if (!udmurtSide.get(entry.getKey()).getId().equals(russianSide.get(entry.getValue()).getId())) {
                allCorrect = false; break;
            }
        }

        if (allCorrect) {
            correctCount++;
            startStep3();
        } else {
            wrongCount++;
            for (int i = 0; i < 4; i++) {
                boolean isCorrect = userPairs.containsKey(i) &&
                        udmurtSide.get(i).getId().equals(russianSide.get(userPairs.get(i)).getId());
                if (isCorrect) {
                    leftViews[i].setBackgroundResource(R.drawable.bg_card_matched);
                    rightViews[userPairs.get(i)].setBackgroundResource(R.drawable.bg_card_matched);
                } else if (userPairs.containsKey(i)) {
                    leftViews[i].setBackgroundResource(R.drawable.bg_button_check);
                    rightViews[userPairs.get(i)].setBackgroundResource(R.drawable.bg_button_check);
                }
            }

            handler.postDelayed(() -> {
                userPairs.clear();
                selectedLeftIndex = -1;
                for (int i = 0; i < 4; i++) {
                    resetLeftVisual(i);
                    resetRightVisual(i);
                }
                redrawLines();
            }, 1500);
        }
    }

    private void resetLeftVisual(int index) {
        leftViews[index].setBackgroundResource(R.drawable.bg_card_closed);
        leftViews[index].setTextColor(Color.parseColor("#333333"));
        if (dotLeftViews[index] != null) dotLeftViews[index].setBackgroundResource(R.drawable.bg_dot_red);
    }

    private void resetRightVisual(int index) {
        rightViews[index].setBackgroundResource(R.drawable.bg_card_closed);
        rightViews[index].setTextColor(Color.parseColor("#333333"));
        if (dotRightViews[index] != null) {
            boolean isConnected = userPairs.containsValue(index);
            dotRightViews[index].setBackgroundResource(isConnected ? R.drawable.bg_dot_red : R.drawable.bg_dot_grey);
        }
    }

    // 3. Верно/Неверно
    private void startStep3() {
        setContentView(R.layout.activity_level6);
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvWordTrueFalse = findViewById(R.id.tvWord);
        tvTranslationHint = findViewById(R.id.tvTranslationHint);
        btnTrue = findViewById(R.id.btnTrue);
        btnFalse = findViewById(R.id.btnFalse);
        btnActionTF = findViewById(R.id.btnCheck);

        currentWordIndex = 0;
        showStep3Question();
    }

    private void showStep3Question() {
        userAnswerTF = null;

        if (btnTrue != null) {
            btnTrue.setBackgroundResource(R.drawable.bg_card_closed);
            btnTrue.setTextColor(Color.parseColor("#333333"));
        }
        if (btnFalse != null) {
            btnFalse.setBackgroundResource(R.drawable.bg_card_closed);
            btnFalse.setTextColor(Color.parseColor("#333333"));
        }
        if (btnActionTF != null) btnActionTF.setText("Проверить");

        TextView tvExplanation = findViewById(R.id.tvExplanation);
        if (tvExplanation != null) tvExplanation.setVisibility(View.GONE);

        ImageView ivWord = findViewById(R.id.ivWordImage);
        TextView tvWord = findViewById(R.id.tvWord);

        Word correctWord = lessonWords.get(currentWordIndex);

        boolean showCorrect = Math.random() < 0.5;
        isStatementTrue = showCorrect;

        Word textWord;
        if (showCorrect) {
            textWord = correctWord;
        } else {
            textWord = lessonWords.get((currentWordIndex + 1) % 4);
        }

        // Загрузка картинки: сначала локальный кэш, потом сеть
        if (ivWord != null) {
            if (correctWord.getImageUrl() != null && !correctWord.getImageUrl().isEmpty()) {
                ivWord.setVisibility(View.VISIBLE);

                java.io.File localFile = ImageCacheManager.getLocalFile(this, correctWord.getImageUrl());

                if (localFile.exists()) {
                    coil.Coil.imageLoader(this).enqueue(
                            new coil.request.ImageRequest.Builder(this)
                                    .data(localFile)
                                    .target(ivWord)
                                    .crossfade(true)
                                    .build()
                    );
                } else {
                    coil.Coil.imageLoader(this).enqueue(
                            new coil.request.ImageRequest.Builder(this)
                                    .data(correctWord.getImageUrl())
                                    .target(ivWord)
                                    .crossfade(true)
                                    .build()
                    );
                }
            } else {
                ivWord.setVisibility(View.GONE);
            }
        }

        if (tvWord != null) {
            tvWord.setText(textWord.getUdmurt());
        }

        if (tvTranslationHint != null) {
            tvTranslationHint.setVisibility(View.GONE);
        }

        if (btnTrue != null) {
            btnTrue.setOnClickListener(v -> {
                userAnswerTF = true;
                btnTrue.setBackgroundResource(R.drawable.bg_button_check);
                btnTrue.setTextColor(Color.WHITE);
                btnFalse.setBackgroundResource(R.drawable.bg_card_closed);
                btnFalse.setTextColor(Color.parseColor("#333333"));
            });
        }

        if (btnFalse != null) {
            btnFalse.setOnClickListener(v -> {
                userAnswerTF = false;
                btnFalse.setBackgroundResource(R.drawable.bg_button_check);
                btnFalse.setTextColor(Color.WHITE);
                btnTrue.setBackgroundResource(R.drawable.bg_card_closed);
                btnTrue.setTextColor(Color.parseColor("#333333"));
            });
        }

        if (btnActionTF != null) {
            final Word finalTextWord = textWord;
            final Word finalCorrectWord = correctWord;

            btnActionTF.setOnClickListener(v -> {
                if (btnActionTF.getText().toString().equals("Проверить")) {
                    if (userAnswerTF == null) return;

                    boolean correct = (userAnswerTF == isStatementTrue);

                    if (correct) {
                        correctCount++;
                        if (userAnswerTF) {
                            btnTrue.setBackgroundResource(R.drawable.bg_card_matched);
                            btnTrue.setTextColor(Color.BLACK);
                        } else {
                            btnFalse.setBackgroundResource(R.drawable.bg_card_matched);
                            btnFalse.setTextColor(Color.BLACK);
                        }

                        if (tvExplanation != null) {
                            tvExplanation.setVisibility(View.VISIBLE);
                            tvExplanation.setText("Верно! " + finalCorrectWord.getUdmurt() + " — " + finalCorrectWord.getRussian());
                        }
                    } else {
                        wrongCount++;
                        if (userAnswerTF) {
                            btnTrue.setBackgroundResource(R.drawable.bg_button_check);
                        } else {
                            btnFalse.setBackgroundResource(R.drawable.bg_button_check);
                        }

                        if (isStatementTrue) {
                            btnTrue.setBackgroundResource(R.drawable.bg_card_matched);
                            btnTrue.setTextColor(Color.BLACK);
                        } else {
                            btnFalse.setBackgroundResource(R.drawable.bg_card_matched);
                            btnFalse.setTextColor(Color.BLACK);
                        }

                        if (tvExplanation != null) {
                            tvExplanation.setVisibility(View.VISIBLE);
                            if (isStatementTrue) {
                                tvExplanation.setText("Неверно! Это " + finalCorrectWord.getUdmurt() + " — " + finalCorrectWord.getRussian() + ". Правильный ответ: «Верно».");
                            } else {
                                tvExplanation.setText("Неверно! Это " + finalCorrectWord.getUdmurt() + " — " + finalCorrectWord.getRussian() + ", а слово «" + finalTextWord.getUdmurt() + "» — это " + finalTextWord.getRussian() + ". Правильный ответ: «Неверно».");
                            }
                        }
                    }

                    btnActionTF.setText("Продолжить");
                } else {
                    currentWordIndex++;
                    if (currentWordIndex < 4) showStep3Question();
                    else startStep4();
                }
            });
        }
    }

    // 4. Предложения
    private void startStep4() {
        setContentView(R.layout.activity_lesson_step4);
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        currentWordIndex = 0;
        showStep4Question();
    }

    private void showStep4Question() {
        isAnswered = false;
        selectedOption = null;
        selectedIndex = -1;

        TextView tvSentence = findViewById(R.id.tvSentence);
        TextView[] opts = {findViewById(R.id.tvOpt1), findViewById(R.id.tvOpt2), findViewById(R.id.tvOpt3), findViewById(R.id.tvOpt4)};
        TextView tvExplanation = findViewById(R.id.tvExplanation);

        if (tvExplanation != null) {
            tvExplanation.setText("");
            tvExplanation.setVisibility(View.GONE);
        }

        com.google.android.material.card.MaterialCardView[] cards = {
                findViewById(R.id.cardOption1), findViewById(R.id.cardOption2),
                findViewById(R.id.cardOption3), findViewById(R.id.cardOption4)
        };

        Word current = lessonWords.get(currentWordIndex);

        String udmurtSentence = current.getExample();
        if (udmurtSentence == null || udmurtSentence.isEmpty() || udmurtSentence.equals("null")) udmurtSentence = current.getUdmurt();
        if (tvSentence != null) tvSentence.setText(udmurtSentence);

        String correctRussian = current.getExampleRus();
        if (correctRussian == null || correctRussian.isEmpty() || correctRussian.equals("null")) correctRussian = current.getRussian();

        List<String> options = new ArrayList<>();
        options.add(correctRussian);

        for (Word w : lessonWords) {
            if (w != current) {
                String wrongRussian = w.getExampleRus();
                if (wrongRussian == null || wrongRussian.isEmpty() || wrongRussian.equals("null")) wrongRussian = w.getRussian();
                if (wrongRussian != null && !wrongRussian.equals("null") && !options.contains(wrongRussian)) options.add(wrongRussian);
            }
        }

        if (options.size() < 4) {
            for (Word w : lessonWords) {
                String extra = w.getRussian();
                if (extra != null && !extra.equals("null") && !options.contains(extra)) options.add(extra);
                if (options.size() == 4) break;
            }
        }

        Collections.shuffle(options);
        List<String> finalOptions = options.subList(0, Math.min(options.size(), 4));

        for (int i = 0; i < 4; i++) {
            if (i < finalOptions.size()) {
                opts[i].setText(finalOptions.get(i));
                cards[i].setVisibility(View.VISIBLE);
                cards[i].setCardBackgroundColor(Color.WHITE);
                opts[i].setTextColor(Color.parseColor("#2D2D2D"));
            } else {
                cards[i].setVisibility(View.GONE);
            }

            final int idx = i;

            cards[i].setOnClickListener(v -> {
                if (isAnswered) return;
                selectedIndex = idx;
                selectedOption = opts[idx].getText().toString();

                for (int j = 0; j < 4; j++) {
                    cards[j].setCardBackgroundColor(Color.WHITE);
                    opts[j].setTextColor(Color.parseColor("#2D2D2D"));
                }

                cards[idx].setCardBackgroundColor(Color.parseColor("#DF3844"));
                opts[idx].setTextColor(Color.WHITE);
            });
        }

        Button btn = findViewById(R.id.btnAction);
        if (btn != null) btn.setText("Проверить");
        final String finalCorrectAnswer = correctRussian;
        final Word finalCurrent = current;

        if (btn != null) {
            btn.setOnClickListener(v -> {
                if (!isAnswered) {
                    if (selectedOption == null) return;
                    isAnswered = true;

                    for (int j = 0; j < 4; j++) {
                        if (opts[j].getText().toString().equals(finalCorrectAnswer)) {
                            cards[j].setCardBackgroundColor(Color.parseColor("#33786C"));
                            opts[j].setTextColor(Color.BLACK);
                        } else if (j == selectedIndex) {
                            cards[j].setCardBackgroundColor(Color.parseColor("#DF3844"));
                            opts[j].setTextColor(Color.WHITE);
                        }
                    }

                    boolean isCorrect = selectedOption.equals(finalCorrectAnswer);
                    String explanationText;

                    if (isCorrect) {
                        correctCount++;
                        explanationText = "Верно! " + finalCurrent.getExample() + " — " + finalCurrent.getExampleRus();
                    } else {
                        wrongCount++;
                        explanationText = "Неверно! Правильно: " + finalCorrectAnswer
                                + "\n" + finalCurrent.getExample() + " — " + finalCurrent.getExampleRus();
                    }

                    if (tvExplanation != null) {
                        tvExplanation.setText(explanationText);
                        tvExplanation.setVisibility(View.VISIBLE);
                        tvExplanation.requestLayout();
                        tvExplanation.invalidate();
                    }

                    btn.setText("Продолжить");
                } else {
                    currentWordIndex++;
                    if (currentWordIndex < 4) showStep4Question();
                    else startStep5();
                }
            });
        }
    }

    // 5. карточки для запоминания
    private void startStep5() {
        setContentView(R.layout.activity_level5);
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnCheck = findViewById(R.id.btnCheck);
        if (btnCheck != null) btnCheck.setVisibility(View.GONE);

        for (int i = 9; i <= 12; i++) {
            int id = getResources().getIdentifier("card" + i, "id", getPackageName());
            View c = findViewById(id);
            if (c != null) c.setVisibility(View.GONE);
        }
        setupMemeGame();
    }

    private void setupMemeGame() {
        memeCards.clear();
        openedCards.clear();
        matchedCards.clear();
        isBusy = false;

        for (int i = 0; i < 4; i++) {
            memeCards.add(new MemeCard(lessonWords.get(i).getUdmurt(), i, true));
            memeCards.add(new MemeCard(lessonWords.get(i).getRussian(), i, false));
        }
        Collections.shuffle(memeCards);

        for (int i = 1; i <= 8; i++) {
            int cardId = getResources().getIdentifier("card" + i, "id", getPackageName());
            int imgId = getResources().getIdentifier("card" + i + "Image", "id", getPackageName());
            int txtId = getResources().getIdentifier("card" + i + "Text", "id", getPackageName());

            View cardView = findViewById(cardId);
            ImageView img = findViewById(imgId);
            TextView txt = findViewById(txtId);

            if (cardView == null || img == null || txt == null) continue;

            img.setVisibility(View.VISIBLE);
            txt.setVisibility(View.GONE);
            cardView.setBackgroundResource(R.drawable.bg_card_closed);

            final int index = i - 1;
            cardView.setOnClickListener(v -> onMemeCardClick(index, cardView, img, txt));
        }
    }

    private void onMemeCardClick(int index, View cardView, ImageView img, TextView txt) {
        if (isBusy || matchedCards.contains(index) || openedCards.contains(index)) return;

        img.setVisibility(View.GONE);
        txt.setVisibility(View.VISIBLE);
        txt.setText(memeCards.get(index).word);
        cardView.setBackgroundResource(R.drawable.bg_card_open);
        openedCards.add(index);

        if (openedCards.size() == 2) {
            isBusy = true;
            int first = openedCards.get(0);
            int second = openedCards.get(1);
            MemeCard c1 = memeCards.get(first);
            MemeCard c2 = memeCards.get(second);

            if (c1.pairId == c2.pairId && c1.isUdmurt != c2.isUdmurt) {
                handler.postDelayed(() -> {
                    int id1 = getResources().getIdentifier("card" + (first + 1), "id", getPackageName());
                    int id2 = getResources().getIdentifier("card" + (second + 1), "id", getPackageName());
                    View v1 = findViewById(id1); View v2 = findViewById(id2);
                    if (v1 != null) v1.setBackgroundResource(R.drawable.bg_card_matched);
                    if (v2 != null) v2.setBackgroundResource(R.drawable.bg_card_matched);
                    matchedCards.add(first); matchedCards.add(second);
                    openedCards.clear(); isBusy = false;

                    if (matchedCards.size() == 8) {
                        showFinishScreen();
                    }
                }, 500);
            } else {
                handler.postDelayed(() -> {
                    int id1 = getResources().getIdentifier("card" + (first + 1), "id", getPackageName());
                    int id2 = getResources().getIdentifier("card" + (second + 1), "id", getPackageName());
                    ImageView img1 = findViewById(getResources().getIdentifier("card" + (first + 1) + "Image", "id", getPackageName()));
                    TextView txt1 = findViewById(getResources().getIdentifier("card" + (first + 1) + "Text", "id", getPackageName()));
                    ImageView img2 = findViewById(getResources().getIdentifier("card" + (second + 1) + "Image", "id", getPackageName()));
                    TextView txt2 = findViewById(getResources().getIdentifier("card" + (second + 1) + "Text", "id", getPackageName()));

                    if (img1 != null) img1.setVisibility(View.VISIBLE);
                    if (txt1 != null) txt1.setVisibility(View.GONE);
                    if (img2 != null) img2.setVisibility(View.VISIBLE);
                    if (txt2 != null) txt2.setVisibility(View.GONE);
                    View v1 = findViewById(id1); View v2 = findViewById(id2);
                    if (v1 != null) v1.setBackgroundResource(R.drawable.bg_card_closed);
                    if (v2 != null) v2.setBackgroundResource(R.drawable.bg_card_closed);
                    openedCards.clear(); isBusy = false;
                }, 1000);
            }
        }
    }

    // финальный экран
    private void showFinishScreen() {
        SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        String userId = userPrefs.getString("user_id", "guest");
        prefs.edit().putBoolean("lesson_" + lessonNumber + "_passed_" + category + "_" + userId, true).apply();

        saveAttemptToHistory(prefs);

        setContentView(R.layout.activity_lesson_finish);

        TextView tvCorrect = findViewById(R.id.tvCorrectCount);
        TextView tvWrong = findViewById(R.id.tvWrongCount);
        if (tvCorrect != null) tvCorrect.setText(String.valueOf(correctCount));
        if (tvWrong != null) tvWrong.setText(String.valueOf(wrongCount));

        View btnFinish = findViewById(R.id.btnFinish);
        if (btnFinish != null) btnFinish.setOnClickListener(v -> finish());

        checkAchievementsAndNotify();
    }

    private void checkAchievementsAndNotify() {
        SharedPreferences prefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "guest");

        String firstStepKey = "achievement_first_step_" + userId;
        if (!prefs.getBoolean(firstStepKey, false)) {
            prefs.edit().putBoolean(firstStepKey, true).apply();
            AchievementNotification.show(this, "Первый шаг", "Вы прошли свой первый урок!");
        }

        String perfectKey = "achievement_perfect_" + userId;
        if (!prefs.getBoolean(perfectKey, false) && wrongCount == 0) {
            prefs.edit().putBoolean(perfectKey, true).apply();
            AchievementNotification.show(this, "Без единой ошибки", "Вы прошли урок без ошибок!");
        }

        String historyKey = "stats_history_" + userId;
        String historyJson = prefs.getString(historyKey, "[]");
        try {
            JSONArray historyArray = new JSONArray(historyJson);
            if (historyArray.length() >= 5) {
                String fiveKey = "achievement_five_lessons_" + userId;
                if (!prefs.getBoolean(fiveKey, false)) {
                    prefs.edit().putBoolean(fiveKey, true).apply();
                    AchievementNotification.show(this, "Пять уроков", "Вы прошли 5 уроков!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAttemptToHistory(SharedPreferences prefs) {
        try {
            SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String userId = userPrefs.getString("user_id", "guest");

            String historyKey = "stats_history_" + userId;

            String historyJson = prefs.getString(historyKey, "[]");
            JSONArray historyArray = new JSONArray(historyJson);

            JSONObject record = new JSONObject();
            record.put("date", System.currentTimeMillis());
            record.put("category", category);
            record.put("lesson", lessonNumber);
            record.put("correct", correctCount);
            record.put("wrong", wrongCount);

            JSONArray wordsArray = new JSONArray();
            for (Word w : lessonWords) {
                wordsArray.put(w.getUdmurt());
            }
            record.put("words", wordsArray);

            historyArray.put(record);
            prefs.edit().putString(historyKey, historyArray.toString()).apply();

        } catch (Exception e) {
            Log.e("STATS", "Ошибка сохранения истории: " + e.getMessage());
        }
    }

    static class MemeCard {
        String word; int pairId; boolean isUdmurt;
        MemeCard(String word, int pairId, boolean isUdmurt) { this.word = word; this.pairId = pairId; this.isUdmurt = isUdmurt; }
    }
}
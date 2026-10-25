package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import coil.Coil;
import coil.request.ImageRequest;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HOME_ACTIVITY";
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setActiveTab("home");

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = prefs.getString("email", null);
        String name = prefs.getString("name", "");
        String userId = prefs.getString("user_id", "guest");

        if (tvGreeting != null) {
            if (email != null && !name.isEmpty()) {
                tvGreeting.setText("Ӟечбур, " + name + "!");
            } else {
                tvGreeting.setText("Привет!");
            }
        }

        // настройки
        ImageView ivSettings = findViewById(R.id.ivSettings);
        if (ivSettings != null) {
            ivSettings.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            });
        }

        // карточки
        View cardLessons = findViewById(R.id.cardLessons);
        if (cardLessons != null) cardLessons.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, TopicsActivity.class)));

        View cardDictionary = findViewById(R.id.cardDictionary);
        if (cardDictionary != null) cardDictionary.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, DictionaryActivity.class)));

        View cardGames = findViewById(R.id.cardGames);
        if (cardGames != null) cardGames.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, GamesActivity.class)));

        View cardAchievements = findViewById(R.id.cardAchievements);
        if (cardAchievements != null) cardAchievements.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AchievementsActivity.class)));

        // нижняя навигация
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) navHome.setOnClickListener(v -> Toast.makeText(this, "Вы уже на главной", Toast.LENGTH_SHORT).show());

        View navLessons = findViewById(R.id.navLessons);
        if (navLessons != null) navLessons.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, TopicsActivity.class));
            finish();
        });

        View navDictionary = findViewById(R.id.navDictionary);
        if (navDictionary != null) navDictionary.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, DictionaryActivity.class));
            finish();
        });

        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) navProfile.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            finish();
        });

        // слово дня + синхронизация слов
        String accessToken = prefs.getString("access_token", "");
        Word savedWord = WordOfTheDayManager.loadWordFromPrefs(this);
        boolean isNewDay = WordOfTheDayManager.shouldShowWordOfDay(this);

        if (savedWord != null) {
            showWordOfDayText(savedWord);
        }

        if (isNewDay) {
            // фиксация даты до появляения сети
            WordOfTheDayManager.markWordOfDayShown(this);

            if (!accessToken.isEmpty()) {
                syncWordsInBackground(accessToken, savedWord, true);
            } else if (savedWord == null) {
                showFallbackWord();
            }
        } else {
            // сегодня уже было показано слово дня
            if (!accessToken.isEmpty()) {
                syncWordsInBackground(accessToken, savedWord, false);
            }
        }
    }

    private void syncWordsInBackground(String accessToken, Word savedWord, boolean showDialog) {
        if (accessToken == null || accessToken.isEmpty()) return;

        SupabaseAuth.loadWords(accessToken, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String responseBody) {
                dbExecutor.execute(() -> {
                    try {
                        org.json.JSONArray arr = new org.json.JSONArray(responseBody);
                        java.util.List<WordEntity> entities = new java.util.ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            org.json.JSONObject obj = arr.getJSONObject(i);
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

                        if (!entities.isEmpty()) {
                            Log.d("IMAGE_TEST", "Слово дня сохраняется: " + entities.get(0).udmurt + ", URL: " + entities.get(0).imageUrl);
                        }

                        WordDao dao = AppDatabase.getInstance(HomeActivity.this).wordDao();
                        dao.deleteAll();
                        dao.insertAll(entities);

                        Log.d(TAG, "Синхронизировано слов: " + entities.size());

                        for (WordEntity entity : entities) {
                            if (entity.imageUrl != null && !entity.imageUrl.isEmpty()) {
                                ImageCacheManager.downloadImage(HomeActivity.this, entity.imageUrl);
                            }
                        }

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            if (showDialog) {
                                Word word = WordOfTheDayManager.getWordOfDay(HomeActivity.this, responseBody);
                                if (word != null) {
                                    showWordOfDayDialog(word);
                                } else if (savedWord != null) {
                                    showWordOfDayText(savedWord);
                                } else {
                                    showFallbackWord();
                                }
                            }
                        });

                    } catch (Exception ex) {
                        Log.e(TAG, "Ошибка парсинга/сохранения слов", ex);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (savedWord != null) showWordOfDayText(savedWord);
                            else if (showDialog) showFallbackWord();
                        });
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Ошибка загрузки слов: " + message);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (savedWord != null) showWordOfDayText(savedWord);
                    else if (showDialog) showFallbackWord();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }

    private void showFallbackWord() {
        if (isFinishing() || isDestroyed()) return;

        Word fallback = new Word("", "Корка", "Дом", "Дом и быт", "", "", "", "");
        showWordOfDayText(fallback);
        WordOfTheDayManager.saveWordToPrefs(this, fallback);
    }

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

    private void showWordOfDayDialog(Word word) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_word_of_day);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = (int) (displayMetrics.widthPixels * 0.85);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.TextView tvUdmurt = dialog.findViewById(R.id.tvUdmurt);
        android.widget.TextView tvRussian = dialog.findViewById(R.id.tvRussian);
        android.widget.TextView tvExampleUdmurt = dialog.findViewById(R.id.tvExampleUdmurt);
        android.widget.TextView tvExampleRussian = dialog.findViewById(R.id.tvExampleRussian);
        android.widget.ImageView ivWordImage = dialog.findViewById(R.id.ivWordImage);
        android.widget.ImageView btnPlayAudio = dialog.findViewById(R.id.btnPlayAudio);
        android.widget.Button btnClose = dialog.findViewById(R.id.btnClose);

        if (tvUdmurt != null) tvUdmurt.setText(word.getUdmurt());
        if (tvRussian != null) tvRussian.setText(word.getRussian());

        if (tvExampleUdmurt != null) {
            if (word.getExample() != null && !word.getExample().isEmpty()) {
                tvExampleUdmurt.setText(word.getExample());
                tvExampleUdmurt.setVisibility(View.VISIBLE);
            } else {
                tvExampleUdmurt.setVisibility(View.GONE);
            }
        }

        if (tvExampleRussian != null) {
            if (word.getExampleRus() != null && !word.getExampleRus().isEmpty()) {
                tvExampleRussian.setText(word.getExampleRus());
                tvExampleRussian.setVisibility(View.VISIBLE);
            } else {
                tvExampleRussian.setVisibility(View.GONE);
            }
        }

        if (ivWordImage != null) {
            Log.d("IMAGE_TEST", "В диалог пришла ссылка: " + word.getImageUrl());

            if (word.getImageUrl() != null && !word.getImageUrl().isEmpty()) {
                java.io.File localFile = ImageCacheManager.getLocalFile(this, word.getImageUrl());

                if (localFile.exists()) {
                    Coil.imageLoader(this).enqueue(
                            new ImageRequest.Builder(this)
                                    .data(localFile)
                                    .target(ivWordImage)
                                    .crossfade(true)
                                    .build()
                    );
                } else {
                    Coil.imageLoader(this).enqueue(
                            new ImageRequest.Builder(this)
                                    .data(word.getImageUrl())
                                    .target(ivWordImage)
                                    .crossfade(true)
                                    .build()
                    );
                }
            } else {
                ivWordImage.setImageResource(R.drawable.ic_sun_placeholder);
            }
        }

        if (btnPlayAudio != null) {
            btnPlayAudio.setOnClickListener(v -> {
                String audioUrl = word.getAudioUrl();
                if (audioUrl != null && !audioUrl.isEmpty()) {
                    try {
                        android.media.MediaPlayer mp = new android.media.MediaPlayer();
                        mp.setDataSource(audioUrl);
                        mp.prepareAsync();
                        mp.setOnPreparedListener(android.media.MediaPlayer::start);
                        mp.setOnCompletionListener(android.media.MediaPlayer::release);
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка аудио", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Нет озвучки", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                dialog.dismiss();
                showWordOfDayText(word);
            });
        }

        dialog.setCancelable(false);
        dialog.show();
    }

    private void showWordOfDayText(Word word) {
        if (isFinishing() || isDestroyed()) return;

        TextView tvWordOfDayText = findViewById(R.id.tvWordOfDayText);
        if (tvWordOfDayText != null) {
            tvWordOfDayText.setVisibility(View.VISIBLE);
            tvWordOfDayText.setText("Слово дня: " + word.getUdmurt() + " (" + word.getRussian() + ")");
        }
    }
}
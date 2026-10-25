package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryActivity extends AppCompatActivity {

    private static final String TAG = "DICTIONARY";
    private WordAdapter adapter;
    private final List<Word> words = new ArrayList<>();
    private LinearLayout llCategories;
    private TextView selectedCategoryView = null;
    private RecyclerView rvWords;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);

        setActiveTab("dictionary");
        llCategories = findViewById(R.id.llCategories);

        rvWords = findViewById(R.id.rvWords);
        rvWords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WordAdapter(this, words);
        rvWords.setAdapter(adapter);
        adapter.setOnItemClickListener(word -> openWordDetail(word));

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(DictionaryActivity.this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.navLessons).setOnClickListener(v -> {
            startActivity(new Intent(DictionaryActivity.this, TopicsActivity.class));
            finish();
        });
        findViewById(R.id.navDictionary).setOnClickListener(v ->
                Toast.makeText(this, "Вы уже в словаре", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(DictionaryActivity.this, ProfileActivity.class));
            finish();
        });


        loadWordsFromRoom();

        refreshWordsFromSupabase();
    }

    // чтение слов из локальной базы
    private void loadWordsFromRoom() {
        dbExecutor.execute(() -> {
            List<WordEntity> entities = AppDatabase.getInstance(this).wordDao().getAllWords();
            List<Word> localWords = new ArrayList<>();
            for (WordEntity e : entities) {
                localWords.add(new Word(
                        e.serverId != null ? e.serverId : "",
                        e.udmurt, e.russian, e.category,
                        e.imageUrl, e.audioUrl, e.example, e.exampleRus
                ));
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                words.clear();
                words.addAll(localWords);
                adapter = new WordAdapter(DictionaryActivity.this, words);
                rvWords.setAdapter(adapter);
                adapter.setOnItemClickListener(word -> openWordDetail(word));
                buildCategoryButtons(words);

                if (words.isEmpty()) {
                    Toast.makeText(this, "Словарь пуст. Подключитесь к интернету.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // обновление слов из бд
    private void refreshWordsFromSupabase() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", "");

        if (accessToken.isEmpty()) return;

        SupabaseAuth.loadWords(accessToken, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                dbExecutor.execute(() -> {
                    try {
                        JSONArray arr = new JSONArray(message);
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

                        WordDao dao = AppDatabase.getInstance(DictionaryActivity.this).wordDao();
                        dao.deleteAll();
                        dao.insertAll(entities);

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            loadWordsFromRoom();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка парсинга: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Не удалось обновить из Supabase (работаем из кэша): " + message);
            }
        });
    }

    private void openWordDetail(Word word) {
        Intent intent = new Intent(DictionaryActivity.this, WordDetailActivity.class);
        intent.putExtra(WordDetailActivity.EXTRA_UDMURT, word.getUdmurt());
        intent.putExtra(WordDetailActivity.EXTRA_RUSSIAN, word.getRussian());
        intent.putExtra(WordDetailActivity.EXTRA_IMAGE_URL, word.getImageUrl());
        intent.putExtra(WordDetailActivity.EXTRA_CATEGORY, word.getCategory());
        intent.putExtra(WordDetailActivity.EXTRA_EXAMPLE, word.getExample());
        intent.putExtra(WordDetailActivity.EXTRA_EXAMPLE_RUS, word.getExampleRus());
        intent.putExtra(WordDetailActivity.EXTRA_AUDIO_URL, word.getAudioUrl());

        int index = words.indexOf(word);
        intent.putExtra(WordDetailActivity.EXTRA_WORDS_LIST, new ArrayList<>(words));
        intent.putExtra(WordDetailActivity.EXTRA_CURRENT_INDEX, index);

        startActivity(intent);
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

    private void buildCategoryButtons(List<Word> words) {
        llCategories.removeAllViews();
        selectedCategoryView = null;

        Set<String> categories = new LinkedHashSet<>();
        for (Word w : words) {
            categories.add(w.getCategory());
        }

        createCategoryButton("Все", true);

        for (String cat : categories) {
            createCategoryButton(cat, false);
        }
    }

    private void createCategoryButton(String title, boolean isSelected) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.NORMAL);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(18), 0, dp(18), 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
        params.setMargins(0, 0, dp(8), 0);
        tv.setLayoutParams(params);

        setCategoryBackground(tv, isSelected);

        tv.setOnClickListener(v -> {
            if (selectedCategoryView != null) {
                setCategoryBackground(selectedCategoryView, false);
            }
            setCategoryBackground(tv, true);
            selectedCategoryView = tv;
            adapter.setCategory(title);
        });

        if (isSelected) {
            selectedCategoryView = tv;
        }

        llCategories.addView(tv);
    }

    private void setCategoryBackground(TextView tv, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(selected ? 0xFFD32F2F : 0xFFEEEEEE);
        tv.setBackground(bg);
        tv.setTextColor(selected ? Color.WHITE : 0xFF555555);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
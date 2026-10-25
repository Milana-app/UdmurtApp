package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class WordDetailActivity extends AppCompatActivity {

    public static final String EXTRA_UDMURT = "extra_udmurt";
    public static final String EXTRA_RUSSIAN = "extra_russian";
    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_CATEGORY = "extra_category";
    public static final String EXTRA_EXAMPLE = "extra_example";
    public static final String EXTRA_EXAMPLE_RUS = "extra_example_rus";
    public static final String EXTRA_AUDIO_URL = "extra_audio_url";

    public static final String EXTRA_WORDS_LIST = "extra_words_list";
    public static final String EXTRA_CURRENT_INDEX = "extra_current_index";

    private ArrayList<Word> wordsList;
    private int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_detail);

        wordsList = (ArrayList<Word>) getIntent().getSerializableExtra(EXTRA_WORDS_LIST);
        currentIndex = getIntent().getIntExtra(EXTRA_CURRENT_INDEX, -1);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvUdmurt = findViewById(R.id.tvUdmurt);
        TextView tvRussian = findViewById(R.id.tvRussian);
        ImageView ivImage = findViewById(R.id.ivWordImage);
        TextView tvExampleUdmurt = findViewById(R.id.tvExampleUdmurt);
        TextView tvExampleRussian = findViewById(R.id.tvExampleRussian);
        ImageView btnPlayAudio = findViewById(R.id.btnPlayAudio);

        String udmurt = getIntent().getStringExtra(EXTRA_UDMURT);
        String russian = getIntent().getStringExtra(EXTRA_RUSSIAN);
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String example = getIntent().getStringExtra(EXTRA_EXAMPLE);
        String exampleRus = getIntent().getStringExtra(EXTRA_EXAMPLE_RUS);
        String audioUrl = getIntent().getStringExtra(EXTRA_AUDIO_URL);

        if (udmurt == null) udmurt = "";
        if (russian == null) russian = "";

        tvUdmurt.setText(udmurt);
        tvRussian.setText(russian);

        Log.d("IMAGE_TEST", "WordDetail пришла ссылка: " + imageUrl); // ЛОГ

        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Проверяем, есть ли файл в локальном кэше
            java.io.File localFile = ImageCacheManager.getLocalFile(this, imageUrl);

            if (localFile.exists()) {
                // загружаем локальный файл
                Glide.with(this)
                        .load(localFile)
                        .placeholder(R.drawable.ic_sun_placeholder)
                        .error(R.drawable.ic_sun_placeholder)
                        .into(ivImage);
            } else {
                // локального файла нет - загружаем из сети
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_sun_placeholder)
                        .error(R.drawable.ic_sun_placeholder)
                        .into(ivImage);
            }
        } else {
            ivImage.setImageResource(R.drawable.ic_sun_placeholder);
        }

        if (example != null && !example.isEmpty()) {
            tvExampleUdmurt.setText(example);
            tvExampleUdmurt.setVisibility(View.VISIBLE);
        } else {
            tvExampleUdmurt.setVisibility(View.GONE);
        }

        if (exampleRus != null && !exampleRus.isEmpty()) {
            tvExampleRussian.setText(exampleRus);
            tvExampleRussian.setVisibility(View.VISIBLE);
        } else {
            tvExampleRussian.setVisibility(View.GONE);
        }

        if (btnPlayAudio != null) {
            btnPlayAudio.setOnClickListener(v -> {
                SharedPreferences settingsPrefs = getSharedPreferences("app_settings", MODE_PRIVATE);
                boolean isSoundEnabled = settingsPrefs.getBoolean("sound_enabled", true);

                if (!isSoundEnabled) {
                    Toast.makeText(this, "Звук выключен в настройках", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (audioUrl != null && !audioUrl.isEmpty()) {
                    try {
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(audioUrl);
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка аудио", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Нет озвучки", Toast.LENGTH_SHORT).show();
                }
            });
        }

        Button btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            if (wordsList != null && currentIndex >= 0 && currentIndex < wordsList.size() - 1) {
                int nextIndex = currentIndex + 1;
                Word nextWord = wordsList.get(nextIndex);
                openWordDetail(nextWord, wordsList, nextIndex);
            } else {
                Toast.makeText(this, "Это последнее слово в списке", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openWordDetail(Word word, ArrayList<Word> list, int index) {
        Intent intent = new Intent(this, WordDetailActivity.class);
        intent.putExtra(EXTRA_UDMURT, word.getUdmurt());
        intent.putExtra(EXTRA_RUSSIAN, word.getRussian());
        intent.putExtra(EXTRA_IMAGE_URL, word.getImageUrl());
        intent.putExtra(EXTRA_CATEGORY, word.getCategory());
        intent.putExtra(EXTRA_EXAMPLE, word.getExample());
        intent.putExtra(EXTRA_EXAMPLE_RUS, word.getExampleRus());
        intent.putExtra(EXTRA_AUDIO_URL, word.getAudioUrl());

        intent.putExtra(EXTRA_WORDS_LIST, list);
        intent.putExtra(EXTRA_CURRENT_INDEX, index);

        startActivity(intent);
        finish();
    }
}
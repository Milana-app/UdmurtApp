package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvFavorites = findViewById(R.id.rvFavorites);
        TextView tvEmpty = findViewById(R.id.tvEmpty);

        rvFavorites.setLayoutManager(new LinearLayoutManager(this));

        // Загружаем избранное
        List<Word> favoriteWords = FavoritesManager.getFavorites(this);

        if (favoriteWords.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);

            WordAdapter adapter = new WordAdapter(this, favoriteWords);
            rvFavorites.setAdapter(adapter);


            adapter.setOnItemClickListener(word -> {
                Intent intent = new Intent(FavoritesActivity.this, WordDetailActivity.class);
                intent.putExtra(WordDetailActivity.EXTRA_UDMURT, word.getUdmurt());
                intent.putExtra(WordDetailActivity.EXTRA_RUSSIAN, word.getRussian());
                intent.putExtra(WordDetailActivity.EXTRA_IMAGE_URL, word.getImageUrl());
                intent.putExtra(WordDetailActivity.EXTRA_CATEGORY, word.getCategory());
                intent.putExtra(WordDetailActivity.EXTRA_EXAMPLE, word.getExample());
                intent.putExtra(WordDetailActivity.EXTRA_EXAMPLE_RUS, word.getExampleRus());
                intent.putExtra(WordDetailActivity.EXTRA_AUDIO_URL, word.getAudioUrl());
                startActivity(intent);
            });
        }
    }
}
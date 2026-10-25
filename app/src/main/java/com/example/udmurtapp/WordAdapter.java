package com.example.udmurtapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Word word);
    }

    private OnItemClickListener itemClickListener;
    private MediaPlayer currentMediaPlayer;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    private final Context context;
    private final List<Word> allWords;
    private final List<Word> filtered = new ArrayList<>();
    private String currentCategory = "Все";
    private String currentQuery = "";

    public WordAdapter(Context context, List<Word> words) {
        this.context = context;
        this.allWords = words;
        applyFilters();
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_word, parent, false);
        return new WordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        Word word = filtered.get(position);
        holder.tvUdmurt.setText(word.getUdmurt());
        holder.tvRussian.setText(word.getRussian());

        // Проверка избранного через FavoritesManager
        holder.ivFavorite.setImageResource(
                FavoritesManager.isFavorite(context, word) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        // Клик по самому элементу — открытие детали
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(word);
            }
        });

        // сохранение в избранное
        holder.ivFavorite.setOnClickListener(v -> {
            FavoritesManager.toggleFavorite(context, word);
            word.setFavorite(!word.isFavorite());
            notifyItemChanged(position);

            Toast.makeText(context,
                    word.isFavorite() ? "Добавлено в избранное" : "Удалено из избранного",
                    Toast.LENGTH_SHORT).show();
        });

        // озвучка с проверкой настроек
        holder.ivSound.setOnClickListener(v -> {
            SharedPreferences settingsPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
            boolean isSoundEnabled = settingsPrefs.getBoolean("sound_enabled", true);

            if (!isSoundEnabled) {
                Toast.makeText(context, "Звук выключен в настройках", Toast.LENGTH_SHORT).show();
                return;
            }

            String audioUrl = word.getAudioUrl();
            if (audioUrl != null && !audioUrl.isEmpty()) {
                playAudio(audioUrl, word);
            } else {
                Toast.makeText(context, "Для этого слова нет озвучки", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Единый метод воспроизведения с корректным освобождением плеера
    private void playAudio(String audioUrl, Word word) {
        try {
            // Освобождаем предыдущий плеер, если он ещё играет
            if (currentMediaPlayer != null) {
                currentMediaPlayer.release();
                currentMediaPlayer = null;
            }

            currentMediaPlayer = new MediaPlayer();
            currentMediaPlayer.setDataSource(audioUrl);
            currentMediaPlayer.prepareAsync();

            currentMediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                Toast.makeText(context, "Воспроизведение: " + word.getUdmurt(), Toast.LENGTH_SHORT).show();
            });

            currentMediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                currentMediaPlayer = null;
            });

            currentMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(context, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
                mp.release();
                currentMediaPlayer = null;
                return true;
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    private void applyFilters() {
        filtered.clear();
        String lower = currentQuery.toLowerCase();
        for (Word w : allWords) {
            boolean matchesQuery = currentQuery.isEmpty()
                    || w.getUdmurt().toLowerCase().contains(lower)
                    || w.getRussian().toLowerCase().contains(lower);
            boolean matchesCategory = currentCategory.equals("Все")
                    || w.getCategory().equalsIgnoreCase(currentCategory);
            if (matchesQuery && matchesCategory) {
                filtered.add(w);
            }
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        this.currentQuery = query;
        applyFilters();
    }

    public void setCategory(String category) {
        this.currentCategory = category;
        applyFilters();
    }

    public static class WordViewHolder extends RecyclerView.ViewHolder {
        TextView tvUdmurt, tvRussian;
        ImageView ivFavorite, ivSound;


        public WordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUdmurt = itemView.findViewById(R.id.tvUdmurt);
            tvRussian = itemView.findViewById(R.id.tvRussian);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            ivSound = itemView.findViewById(R.id.ivSound);
        }
    }
}
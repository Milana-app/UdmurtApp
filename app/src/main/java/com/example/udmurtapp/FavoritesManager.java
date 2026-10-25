package com.example.udmurtapp;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {
    private static final String PREFS_NAME = "favorites_prefs";
    private static final String KEY_FAVORITES = "favorite_words_list";

    private static String wordToString(Word w) {
        return w.getUdmurt() + "|" + w.getRussian() + "|" + w.getCategory() + "|" +
                w.getImageUrl() + "|" + w.getAudioUrl() + "|" + w.getExample() + "|" + w.getExampleRus();
    }

    private static Word stringToWord(String str) {
        String[] parts = str.split("\\|", -1);
        if (parts.length >= 7) {
            return new Word(parts[0], parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
        }
        return null;
    }

    public static void saveFavorites(Context context, List<Word> words) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = new HashSet<>();
        for (Word w : words) {
            set.add(wordToString(w));
        }
        prefs.edit().putStringSet(KEY_FAVORITES, set).apply();
    }

    // Возвращает список избранных слов
    public static List<Word> getFavorites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
        List<Word> favorites = new ArrayList<>();
        for (String str : set) {
            Word w = stringToWord(str);
            if (w != null) {
                w.setFavorite(true);
                favorites.add(w);
            }
        }
        return favorites;
    }

    public static boolean isFavorite(Context context, Word word) {
        List<Word> favorites = getFavorites(context);
        for (Word w : favorites) {
            if (w.getUdmurt().equals(word.getUdmurt())) {
                return true;
            }
        }
        return false;
    }

    // Добавляем или удаляем слово из избранного
    public static void toggleFavorite(Context context, Word word) {
        List<Word> favorites = getFavorites(context);
        boolean removed = false;

        for (int i = 0; i < favorites.size(); i++) {
            if (favorites.get(i).getUdmurt().equals(word.getUdmurt())) {
                favorites.remove(i);
                removed = true;
                break;
            }
        }

        if (!removed) {
            favorites.add(word);
        }
        saveFavorites(context, favorites);
    }
}
package com.example.udmurtapp;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

public class WordOfTheDayManager {

    private static final String PREFS_NAME = "word_of_day_prefs";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_WORD_DATA = "word_data";

    public static boolean shouldShowWordOfDay(Context context) {
        SharedPreferences settings = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean isWordOfDayEnabled = settings.getBoolean("word_of_day_enabled", true);
        if (!isWordOfDayEnabled) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastDate = prefs.getInt(KEY_LAST_DATE, 0);

        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.YEAR) * 10000 +
                (calendar.get(Calendar.MONTH) + 1) * 100 +
                calendar.get(Calendar.DAY_OF_MONTH);

        return lastDate != today;
    }

    // слово дня уже показано
    public static void markWordOfDayShown(Context context) {
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.YEAR) * 10000 +
                (calendar.get(Calendar.MONTH) + 1) * 100 +
                calendar.get(Calendar.DAY_OF_MONTH);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_LAST_DATE, today).apply();
    }

    // сохранение слова в память
    public static void saveWordToPrefs(Context context, Word word) {
        if (word == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            JSONObject obj = new JSONObject();
            obj.put("id", word.getId() != null ? word.getId() : "");
            obj.put("udmurt", word.getUdmurt() != null ? word.getUdmurt() : "");
            obj.put("russian", word.getRussian() != null ? word.getRussian() : "");
            obj.put("category", word.getCategory() != null ? word.getCategory() : "");
            obj.put("imageUrl", word.getImageUrl() != null ? word.getImageUrl() : "");
            obj.put("audioUrl", word.getAudioUrl() != null ? word.getAudioUrl() : "");
            obj.put("example", word.getExample() != null ? word.getExample() : "");
            obj.put("exampleRus", word.getExampleRus() != null ? word.getExampleRus() : "");

            prefs.edit().putString(KEY_WORD_DATA, obj.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // загрузка сохраненного слова дня из памяти
    public static Word loadWordFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String data = prefs.getString(KEY_WORD_DATA, "");
        if (data.isEmpty()) return null;

        try {
            JSONObject obj = new JSONObject(data);
            return new Word(
                    obj.optString("id", ""),
                    obj.optString("udmurt", ""),
                    obj.optString("russian", ""),
                    obj.optString("category", ""),
                    obj.optString("imageUrl", ""),
                    obj.optString("audioUrl", ""),
                    obj.optString("example", ""),
                    obj.optString("exampleRus", "")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // получаем слово дня из базы, сохраняем в память
    public static Word getWordOfDay(Context context, String jsonWords) {
        try {
            JSONArray arr = new JSONArray(jsonWords);
            if (arr.length() == 0) return null;

            Calendar calendar = Calendar.getInstance();
            int today = calendar.get(Calendar.YEAR) * 10000 +
                    (calendar.get(Calendar.MONTH) + 1) * 100 +
                    calendar.get(Calendar.DAY_OF_MONTH);

            int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
            int index = dayOfYear % arr.length();

            JSONObject obj = arr.getJSONObject(index);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_LAST_DATE, today).apply();

            String id = obj.optString("id", String.valueOf(index));
            String udmurt = obj.optString("udmurt", "");
            String russian = obj.optString("russian", "");
            String category = obj.optString("category", "Другое");
            String imageUrl = obj.optString("image_url", "");
            String audioUrl = obj.optString("audio_url", "");
            String example = obj.optString("example", "");
            String exampleRus = obj.optString("example_rus", "");

            Word word = new Word(id, udmurt, russian, category, imageUrl, audioUrl, example, exampleRus);

            saveWordToPrefs(context, word);

            return word;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
package com.example.udmurtapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordRepository {

    private static final String TAG = "WORD_REPO";
    private final WordDao wordDao;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public WordRepository(Context context) {
        this.context = context;
        this.wordDao = AppDatabase.getInstance(context).wordDao();
    }

    public interface WordsCallback {
        void onSuccess(List<WordEntity> words);
        void onError(String message);
    }

    public void getWords(String category, WordsCallback callback) {
        executor.execute(() -> {
            List<WordEntity> localWords;
            if (category == null || category.isEmpty()) {
                localWords = wordDao.getAllWords();
            } else {
                localWords = wordDao.getWordsByCategory(category);
            }

            Log.d(TAG, "Запрос категории: [" + category + "]");
            Log.d(TAG, "Найдено слов в Room: " + localWords.size());

            if (!localWords.isEmpty()) {
                final List<WordEntity> result = localWords;
                mainHandler.post(() -> callback.onSuccess(result));
            } else {
                Log.d(TAG, "В Room пусто, идем в сеть...");
                loadFromNetwork(category, callback);
            }
        });
    }

    // Загрузка из Supabase
    private void loadFromNetwork(String category, WordsCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", "");

        if (accessToken.isEmpty()) {
            Log.e(TAG, "Токен пуст! Нужно войти в аккаунт.");
            mainHandler.post(() -> callback.onError("Нужно войти в аккаунт"));
            return;
        }

        SupabaseAuth.loadWords(accessToken, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String responseBody) {
                Log.d(TAG, "Сеть ответила успешно. Парсим...");
                executor.execute(() -> {
                    List<WordEntity> entities = parseJson(responseBody);

                    if (entities.isEmpty()) {
                        Log.e(TAG, "Сеть вернула пустой список слов.");
                        mainHandler.post(() -> callback.onError("В базе нет слов"));
                        return;
                    }

                    Log.d("IMAGE_TEST", "Из сети: " + entities.get(0).udmurt + ", URL: " + entities.get(0).imageUrl);

                    wordDao.insertAll(entities);
                    Log.d(TAG, "Сохранено в Room: " + entities.size());

                    List<WordEntity> result = new ArrayList<>();
                    for (WordEntity w : entities) {
                        if (category == null || category.isEmpty() || w.category.equalsIgnoreCase(category)) {
                            result.add(w);
                        }
                    }

                    Log.d(TAG, "Отфильтровано для возврата: " + result.size());
                    final List<WordEntity> finalResult = result;
                    mainHandler.post(() -> callback.onSuccess(finalResult));
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Ошибка сети: " + message);
                mainHandler.post(() -> callback.onError("Ошибка сети: " + message));
            }
        });
    }

    private List<WordEntity> parseJson(String json) {
        List<WordEntity> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                WordEntity w = new WordEntity();
                w.serverId = obj.optString("id", String.valueOf(i));
                w.udmurt = obj.optString("udmurt", "");
                w.russian = obj.optString("russian", "");
                w.category = obj.optString("category", "");
                w.imageUrl = obj.optString("image_url", "");
                w.audioUrl = obj.optString("audio_url", "");
                w.example = obj.optString("example", "");
                w.exampleRus = obj.optString("example_rus", "");
                list.add(w);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга JSON: " + e.getMessage());
        }
        return list;
    }
}
package com.example.udmurtapp;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuth {

    private static final String TAG = "SUPABASE_AUTH";
    private static final String BASE_URL = "https://zwbngyjlixwqltugtktt.supabase.co";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp3Ym5neWpsaXh3cWx0dWd0a3R0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyMDA0MDMsImV4cCI6MjEwNjc3NjQwM30.kK0jCE3ZXlHrvHSKhjMd24JSt6SUNybiqEMbiXkXEUA";

    private static final String REDIRECT_URL = "udmurtapp://login-callback";

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    // Регистрация
    public static void signUp(String email, String password, AuthCallback callback) {
        String url = BASE_URL + "/auth/v1/signup";

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);

            // Добавляем emailRedirectTo, чтобы письмо вело обратно в приложение
            JSONObject options = new JSONObject();
            options.put("emailRedirectTo", REDIRECT_URL);
            json.put("options", options);

        } catch (Exception e) {
            callback.onError("Ошибка формирования запроса");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "signUp onFailure: " + e.getMessage());
                callback.onError("Ошибка сети: проверьте интернет");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError(parseError(responseBody));
                }
            }
        });
    }

    // Вход
    public static void signIn(String email, String password, AuthCallback callback) {
        String url = BASE_URL + "/auth/v1/token?grant_type=password";

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (Exception e) {
            callback.onError("Ошибка формирования запроса");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "signIn onFailure: " + e.getMessage());
                callback.onError("Ошибка сети: проверьте интернет");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError(parseError(responseBody));
                }
            }
        });
    }

    public static void saveUserName(String accessToken, String userId, String name, AuthCallback callback) {
        String url = BASE_URL + "/rest/v1/user_profiles";

        JSONObject json = new JSONObject();
        try {
            json.put("user_id", userId);
            json.put("name", name);
        } catch (Exception e) {
            callback.onError("Ошибка формирования запроса");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "saveUserName onFailure: " + e.getMessage());
                callback.onError("Ошибка сети");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    callback.onSuccess("Имя сохранено");
                } else {
                    callback.onError(parseError(responseBody));
                }
            }
        });
    }

    // Загрузка имени из user_profiles
    public static void loadUserName(String accessToken, String userId, AuthCallback callback) {
        String url = BASE_URL + "/rest/v1/user_profiles?user_id=eq." + userId + "&select=name";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loadUserName onFailure: " + e.getMessage());
                callback.onError("Ошибка сети");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError(parseError(responseBody));
                }
            }
        });
    }

    // Загрузка всех слов
    public static void loadWords(String accessToken, AuthCallback callback) {
        String url = BASE_URL + "/rest/v1/words?select=*&order=category.asc,udmurt.asc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "loadWords onFailure: " + e.getMessage());
                callback.onError("Ошибка сети");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError(parseError(responseBody));
                }
            }
        });
    }
    private static String parseError(String body) {
        try {
            JSONObject json = new JSONObject(body);
            if (json.has("error_description")) return json.getString("error_description");
            if (json.has("msg")) return json.getString("msg");
            if (json.has("message")) return json.getString("message");
            if (json.has("error")) return json.getString("error");
        } catch (Exception ignored) {}
        return "Ошибка: " + body;
    }

    // Обновление имени пользователя в таблице user_profiles
    public static void updateUserName(String accessToken, String userId, String newName, AuthCallback callback) {
        String url = BASE_URL + "/rest/v1/user_profiles?user_id=eq." + userId;

        JSONObject json = new JSONObject();
        try {
            json.put("name", newName);
        } catch (Exception e) {
            callback.onError("Ошибка формирования запроса");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body) // Используем PATCH для обновления
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка сети");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Имя обновлено");
                } else {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    callback.onError(parseError(responseBody));
                }
            }
        });
    }
    public static void refreshToken(String refreshToken, AuthCallback callback) {
        String url = BASE_URL + "/auth/v1/token?grant_type=refresh_token";

        JSONObject json = new JSONObject();
        try {
            json.put("refresh_token", refreshToken);
        } catch (Exception e) {
            callback.onError("Ошибка формирования запроса");
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка сети при обновлении токена");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError(parseError(responseBody));
                }
            }
        });
    }
}
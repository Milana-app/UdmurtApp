package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN";
    EditText etEmail, etPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Ссылка "Зарегистрироваться"
        findViewById(R.id.linkRegister).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim().toLowerCase();
                String password = etPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }

                SupabaseAuth.signIn(email, password, new SupabaseAuth.AuthCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            try {
                                org.json.JSONObject json = new org.json.JSONObject(message);
                                String accessToken = json.getString("access_token");
                                String refreshToken = json.optString("refresh_token", "");
                                String userId = json.optJSONObject("user") != null
                                        ? json.getJSONObject("user").optString("id", "")
                                        : "";

                                // Сохраняем email
                                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                prefs.edit()
                                        .putString("email", email)
                                        .putString("access_token", accessToken)
                                        .putString("refresh_token", refreshToken)
                                        .putString("user_id", userId)
                                        .apply();

                                // загружаем имя из SupaBase
                                if (!userId.isEmpty()) {
                                    SupabaseAuth.loadUserName(accessToken, userId,
                                            new SupabaseAuth.AuthCallback() {
                                                @Override
                                                public void onSuccess(String nameJson) {
                                                    try {
                                                        org.json.JSONArray arr = new org.json.JSONArray(nameJson);
                                                        if (arr.length() > 0) {
                                                            String name = arr.getJSONObject(0)
                                                                    .optString("name", "");
                                                            if (!name.isEmpty()) {
                                                                prefs.edit().putString("name", name).apply();
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Ошибка парсинга имени: " + e.getMessage());
                                                    }
                                                    goToHome();
                                                }

                                                @Override
                                                public void onError(String message) {
                                                    Log.e(TAG, "loadUserName error: " + message);
                                                    goToHome();  //  на главную
                                                }
                                            });
                                } else {
                                    goToHome();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка обработки: " + e.getMessage());
                                Toast.makeText(LoginActivity.this,
                                        "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show());
                    }
                });
            }
        });
    }

    private void goToHome() {
        runOnUiThread(() -> {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        });
    }
}
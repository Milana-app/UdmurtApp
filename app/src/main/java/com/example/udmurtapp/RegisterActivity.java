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

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "REGISTER";
    EditText etName, etEmail, etPassword;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.linkLogin).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim().toLowerCase();
                String password = etPassword.getText().toString().trim();

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "Пароль минимум 6 символов", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(RegisterActivity.this, "Регистрация...", Toast.LENGTH_SHORT).show();

                SupabaseAuth.signUp(email, password, new SupabaseAuth.AuthCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> {
                            try {
                                org.json.JSONObject json = new org.json.JSONObject(message);
                                String accessToken = json.optString("access_token", "");
                                String userId = json.optJSONObject("user") != null
                                        ? json.getJSONObject("user").optString("id", "")
                                        : "";

                                Log.d(TAG, "accessToken=" + accessToken);
                                Log.d(TAG, "userId=" + userId);

                                // Сохраняем локально
                                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                prefs.edit()
                                        .putString("email", email)
                                        .putString("name", name)
                                        .putString("access_token", accessToken)
                                        .putString("user_id", userId)
                                        .apply();

                                // Сохраняем имя в Supabase
                                if (!accessToken.isEmpty() && !userId.isEmpty()) {
                                    SupabaseAuth.saveUserName(accessToken, userId, name,
                                            new SupabaseAuth.AuthCallback() {
                                                @Override
                                                public void onSuccess(String message) {
                                                    Log.d(TAG, "Имя сохранено в Supabase");
                                                }
                                                @Override
                                                public void onError(String message) {
                                                    Log.e(TAG, "Ошибка сохранения имени: " + message);
                                                }
                                            });
                                }

                                Toast.makeText(RegisterActivity.this,
                                        "Добро пожаловать!",
                                        Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();

                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка парсинга: " + e.getMessage());
                                Toast.makeText(RegisterActivity.this,
                                        "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() ->
                                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show());
                    }
                });
            }
        });
    }
}
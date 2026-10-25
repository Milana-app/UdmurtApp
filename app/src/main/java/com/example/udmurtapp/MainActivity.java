package com.example.udmurtapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // проверка сессии
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", "");

        if (accessToken != null && !accessToken.isEmpty()) {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // если сессии нет - показываем кнопки
        Button btnStart = findViewById(R.id.btn_start);
        Button btnLogin = findViewById(R.id.btn_login);

        btnLogin.setPaintFlags(btnLogin.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });
    }
}
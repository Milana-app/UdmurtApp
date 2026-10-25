package com.example.udmurtapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_LOCATION = "location_enabled";
    private static final String KEY_WORD_OF_DAY = "word_of_day_enabled";

    private SwitchCompat switchSound;
    private SwitchCompat switchLocation;
    private SwitchCompat switchWordOfDay;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        switchSound = findViewById(R.id.switchSound);
        switchLocation = findViewById(R.id.switchLocation);
        switchWordOfDay = findViewById(R.id.switchWordOfDay);

        boolean soundEnabled = prefs.getBoolean(KEY_SOUND, true);
        boolean locationEnabled = prefs.getBoolean(KEY_LOCATION, false);
        boolean wordOfDayEnabled = prefs.getBoolean(KEY_WORD_OF_DAY, true);

        switchSound.setChecked(soundEnabled);
        switchLocation.setChecked(locationEnabled);
        switchWordOfDay.setChecked(wordOfDayEnabled);

        // Звук
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Звук включён" : "Звук выключен",
                    Toast.LENGTH_SHORT).show();
        });

        // Слово дня
        switchWordOfDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_WORD_OF_DAY, isChecked).apply();
            Toast.makeText(this,
                    isChecked ? "Слово дня включено" : "Слово дня выключено",
                    Toast.LENGTH_SHORT).show();
        });

        // Геолокация
        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (hasLocationPermission()) {
                    prefs.edit().putBoolean(KEY_LOCATION, true).apply();
                    Toast.makeText(this, "Геолокация включена", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            LOCATION_PERMISSION_REQUEST
                    );
                    switchLocation.setChecked(false);
                }
            } else {
                prefs.edit().putBoolean(KEY_LOCATION, false).apply();
                Toast.makeText(this, "Геолокация выключена", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Обработка ответа пользователя на запрос использования геолокации
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                prefs.edit().putBoolean(KEY_LOCATION, true).apply();
                switchLocation.setChecked(true);
                Toast.makeText(this, "Геолокация включена", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putBoolean(KEY_LOCATION, false).apply();
                switchLocation.setChecked(false);
                Toast.makeText(this,
                        "Разрешение на геолокацию не выдано",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
package com.example.udmurtapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView ivProfileAvatar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setActiveTab("profile");

        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvEmail = findViewById(R.id.tvProfileEmail);
        TextView tvStreak = findViewById(R.id.tvStreakProfile);
        LinearLayout btnFavorites = findViewById(R.id.btnFavorites);
        LinearLayout btnAboutLanguage = findViewById(R.id.btnAboutLanguage);
        LinearLayout btnSettings = findViewById(R.id.btnSettings);
        Button btnLogout = findViewById(R.id.btnLogout);
        ImageView btnEditName = findViewById(R.id.btnEditName);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);

        // данные пользователя
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String name = prefs.getString("name", "Пользователь");
        String email = prefs.getString("email", "email@example.com");

        if (tvName != null) tvName.setText(name);
        if (tvEmail != null) tvEmail.setText(email);

        loadProfileImage();

        loadStatistics();

        SharedPreferences streakPrefs = getSharedPreferences("streak_prefs", MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.YEAR) * 10000 +
                (calendar.get(Calendar.MONTH) + 1) * 100 +
                calendar.get(Calendar.DAY_OF_MONTH);

        int lastVisit = streakPrefs.getInt("last_visit", 0);
        int streakCount = streakPrefs.getInt("streak_count", 0);

        if (lastVisit != today) {
            if (lastVisit == today - 1 || (lastVisit == today - 2 && calendar.get(Calendar.DAY_OF_MONTH) == 1)) {
                streakCount++;
            } else {
                streakCount = 1;
            }
            streakPrefs.edit().putInt("last_visit", today).putInt("streak_count", streakCount).apply();
        }
        if (tvStreak != null) {
            tvStreak.setText("🔥 Вы с нами: " + streakCount + " дн. подряд");
        }

        // Клик по аватарке — открытие галереи
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE_REQUEST);
            });
        }

        // клик по карандашу — смена имени
        if (btnEditName != null && tvName != null) {
            btnEditName.setOnClickListener(v -> showEditNameDialog(tvName));
        }

        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, FavoritesActivity.class)));
        }

        if (btnAboutLanguage != null) {
            btnAboutLanguage.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, AboutUdmurtActivity.class)));
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SettingsActivity.class)));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                prefs.edit().clear().apply();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
                finish();
            });
        }

        View navLessons = findViewById(R.id.navLessons);
        if (navLessons != null) {
            navLessons.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, TopicsActivity.class));
                finish();
            });
        }

        View navDictionary = findViewById(R.id.navDictionary);
        if (navDictionary != null) {
            navDictionary.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, DictionaryActivity.class));
                finish();
            });
        }

        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> Toast.makeText(this, "Вы уже на странице профиля", Toast.LENGTH_SHORT).show());
        }
    }

    private void setActiveTab(String activeTab) {
        int activeColor = 0xFFD32F2F;
        int inactiveColor = 0xFF777777;

        setTabIconAndText(R.id.iconHome, R.id.textHome, R.drawable.ic_home_grey, inactiveColor);
        setTabIconAndText(R.id.iconLessons, R.id.textLessons, R.drawable.ic_book_grey, inactiveColor);
        setTabIconAndText(R.id.iconDictionary, R.id.textDictionary, R.drawable.ic_dictionary_grey, inactiveColor);
        setTabIconAndText(R.id.iconProfile, R.id.textProfile, R.drawable.ic_profile_grey, inactiveColor);

        switch (activeTab) {
            case "home":
                setTabIconAndText(R.id.iconHome, R.id.textHome, R.drawable.ic_home_red, activeColor);
                break;
            case "lessons":
                setTabIconAndText(R.id.iconLessons, R.id.textLessons, R.drawable.ic_book_red, activeColor);
                break;
            case "dictionary":
                setTabIconAndText(R.id.iconDictionary, R.id.textDictionary, R.drawable.ic_dictionary_red, activeColor);
                break;
            case "profile":
                setTabIconAndText(R.id.iconProfile, R.id.textProfile, R.drawable.ic_profile_red, activeColor);
                break;
        }
    }

    private void setTabIconAndText(int iconId, int textId, int drawableResId, int color) {
        ImageView icon = findViewById(iconId);
        TextView text = findViewById(textId);

        if (icon != null) {
            icon.setImageResource(drawableResId);
            icon.setImageTintList(null);
        }
        if (text != null) {
            text.setTextColor(color);
        }
    }

    private void loadStatistics() {
        SharedPreferences progressPrefs = getSharedPreferences("progress_prefs", MODE_PRIVATE);
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        String userId = userPrefs.getString("user_id", "guest");

        String historyKey = "stats_history_" + userId;
        String historyJson = progressPrefs.getString(historyKey, "[]");

        int totalLessons = 0;
        int totalCorrect = 0;
        int totalWrong = 0;
        int weekLessons = 0;
        Map<String, Integer> wordCounts = new HashMap<>();

        long weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);

        try {
            JSONArray historyArray = new JSONArray(historyJson);
            totalLessons = historyArray.length();

            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject record = historyArray.getJSONObject(i);
                long date = record.optLong("date", 0);
                int correct = record.optInt("correct", 0);
                int wrong = record.optInt("wrong", 0);

                totalCorrect += correct;
                totalWrong += wrong;

                if (date > weekAgo) weekLessons++;

                JSONArray wordsArray = record.optJSONArray("words");
                if (wordsArray != null) {
                    for (int j = 0; j < wordsArray.length(); j++) {
                        String word = wordsArray.getString(j);
                        wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextView tvTotalLessons = findViewById(R.id.tvTotalLessons);
        TextView tvTotalCorrect = findViewById(R.id.tvTotalCorrect);
        TextView tvTotalWrong = findViewById(R.id.tvTotalWrong);
        TextView tvWeekLessons = findViewById(R.id.tvWeekLessons);
        TextView tvTopWords = findViewById(R.id.tvTopWords);

        if (tvTotalLessons != null) tvTotalLessons.setText(String.valueOf(totalLessons));
        if (tvTotalCorrect != null) tvTotalCorrect.setText(String.valueOf(totalCorrect));
        if (tvTotalWrong != null) tvTotalWrong.setText(String.valueOf(totalWrong));
        if (tvWeekLessons != null) tvWeekLessons.setText("Пройдено уроков: " + weekLessons);

        if (tvTopWords != null) {
            if (wordCounts.isEmpty()) {
                tvTopWords.setText("Пока нет данных");
            } else {
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(wordCounts.entrySet());
                Collections.sort(sorted, (a, b) -> b.getValue().compareTo(a.getValue()));

                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (Map.Entry<String, Integer> entry : sorted) {
                    sb.append("• ").append(entry.getKey()).append(" — ").append(entry.getValue()).append(" раз\n");
                    count++;
                    if (count >= 5) break;
                }
                tvTopWords.setText(sb.toString().trim());
            }
        }
    }

    private void showEditNameDialog(TextView tvName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить имя");

        final EditText input = new EditText(this);
        input.setText(prefs.getString("name", ""));
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                prefs.edit().putString("name", newName).apply();
                tvName.setText(newName);

                String token = prefs.getString("access_token", "");
                String userId = prefs.getString("user_id", "");
                if (!token.isEmpty() && !userId.isEmpty()) {
                    SupabaseAuth.updateUserName(token, userId, newName, new SupabaseAuth.AuthCallback() {
                        @Override
                        public void onSuccess(String message) {
                            runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Имя обновлено", Toast.LENGTH_SHORT).show());
                        }
                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Ошибка: " + message, Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            }
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void loadProfileImage() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "guest");

        File file = new File(getFilesDir(), "profile_avatar_" + userId + ".jpg");

        if (file.exists() && ivProfileAvatar != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            ivProfileAvatar.setImageBitmap(bitmap);
            ivProfileAvatar.setColorFilter(null);
            ivProfileAvatar.setImageTintList(null);
        } else {
            // Если у этого пользователя еще нет аватарки, ставим стандартную
            if (ivProfileAvatar != null) {
                ivProfileAvatar.setImageResource(R.drawable.ic_profile_red);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                if (bitmap == null) {
                    Toast.makeText(this, "Не удалось прочитать картинку", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                String userId = userPrefs.getString("user_id", "guest");
                File file = new File(getFilesDir(), "profile_avatar_" + userId + ".jpg");

                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();

                if (ivProfileAvatar != null) {
                    ivProfileAvatar.setImageBitmap(bitmap);
                    ivProfileAvatar.setColorFilter(null);
                    ivProfileAvatar.setImageTintList(null);
                }

                Toast.makeText(this, "Аватарка обновлена!", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
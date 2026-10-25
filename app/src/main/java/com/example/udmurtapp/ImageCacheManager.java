package com.example.udmurtapp;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageCacheManager {
    private static final String TAG = "IMAGE_CACHE";

    // получаем локальный файл для картинки
    public static File getLocalFile(Context context, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        File cacheDir = new File(context.getFilesDir(), "images");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        return new File(cacheDir, fileName);
    }

    // скачиваем и сохраняем картинку
    public static void downloadImage(Context context, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        File localFile = getLocalFile(context, imageUrl);
        if (localFile.exists()) return;

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(localFile);

            byte[] data = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
            Log.d(TAG, "Скачано: " + localFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка скачивания картинки: " + e.getMessage());
        }
    }
}
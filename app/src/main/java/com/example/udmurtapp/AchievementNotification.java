package com.example.udmurtapp;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class AchievementNotification {

    public static void show(Context context, String title, String description) {
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(40, 0, 40, 0);
        container.setLayoutParams(params);

        androidx.cardview.widget.CardView card = new androidx.cardview.widget.CardView(context);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardParams);
        card.setRadius(24f);
        card.setCardElevation(12f);
        card.setCardBackgroundColor(Color.parseColor("#FFFFFF"));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(context);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setPadding(40, 30, 40, 30);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_achivka);
        android.widget.LinearLayout.LayoutParams iconParams = new android.widget.LinearLayout.LayoutParams(
                dp(context, 32),
                dp(context, 32)
        );
        iconParams.setMargins(0, 0, dp(context, 16), 0);
        icon.setLayoutParams(iconParams);
        layout.addView(icon);

        android.widget.LinearLayout textLayout = new android.widget.LinearLayout(context);
        textLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView tvTitle = new TextView(context);
        tvTitle.setText("Новое достижение!");
        tvTitle.setTextSize(14);
        tvTitle.setTextColor(Color.parseColor("#E53935"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        textLayout.addView(tvTitle);

        TextView tvDesc = new TextView(context);
        tvDesc.setText(title + ": " + description);
        tvDesc.setTextSize(16);
        tvDesc.setTextColor(Color.parseColor("#333333"));
        tvDesc.setPadding(0, 8, 0, 0);
        textLayout.addView(tvDesc);

        layout.addView(textLayout);
        card.addView(layout);
        container.addView(card);
        toast.setView(container);

        toast.show();
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
package com.example.udmurtapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LineOverlayView extends View {

    public static class Connection {
        public float startX, startY, endX, endY;
        public Connection(float startX, float startY, float endX, float endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }
    }

    private final List<Connection> connections = new ArrayList<>();
    private final Paint linePaint;

    public LineOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setClickable(false);
        setFocusable(false);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#D32F2F"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void addConnection(float sx, float sy, float ex, float ey) {
        connections.add(new Connection(sx, sy, ex, ey));
        invalidate();
    }
    public void clearConnections() {
        connections.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Connection c : connections) {
            Path path = new Path();

            float dx = Math.abs(c.endX - c.startX);
            float offset = dx * 0.4f;

            path.moveTo(c.startX, c.startY);

            path.cubicTo(
                    c.startX + offset, c.startY,
                    c.endX - offset, c.endY,
                    c.endX, c.endY
            );

            canvas.drawPath(path, linePaint);
        }
    }
}
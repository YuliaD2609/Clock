package com.example.clock.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    private Paint paint;
    private OnColorSelectedListener listener;
    private int centerX;
    private int centerY;
    private float radius;

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radius = Math.min(centerX, centerY) * 0.9f;

        int[] colors = new int[] {
                Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN,
                Color.GREEN, Color.YELLOW, Color.RED
        };
        SweepGradient sweepGradient = new SweepGradient(centerX, centerY, colors, null);
        paint.setShader(sweepGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX() - centerX;
            float y = event.getY() - centerY;
            double angle = Math.atan2(y, x);
            if (angle < 0)
                angle += 2 * Math.PI;

            // Map angle to color
            // Simple approach: standard HSV mapping
            float hue = (float) (angle / (2 * Math.PI)) * 360f;
            int color = Color.HSVToColor(new float[] { hue, 1f, 1f });

            if (listener != null) {
                listener.onColorSelected(color);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}

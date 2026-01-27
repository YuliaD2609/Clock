package com.example.clock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class ThemeHelper {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_ACCENT_COLOR = "accent_color";

    // Default Purple from Android basic theme or our accent color
    public static final int DEFAULT_COLOR = Color.parseColor("#FF6200EE");

    public static void saveAccentColor(Context context, int color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
    }

    public static int getAccentColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_COLOR);
    }

    public static void resetToDefault(Context context) {
        saveAccentColor(context, DEFAULT_COLOR);
    }

    public static List<Integer> getPresetColors() {
        List<Integer> colors = new ArrayList<>();
        colors.add(DEFAULT_COLOR); // Purple (Default)
        colors.add(Color.parseColor("#FF03DAC5")); // Teal
        colors.add(Color.parseColor("#FFFF5722")); // Orange
        colors.add(Color.parseColor("#FFFFC107")); // Amber
        colors.add(Color.parseColor("#FF4CAF50")); // Green
        colors.add(Color.parseColor("#FF2196F3")); // Blue
        colors.add(Color.parseColor("#FFE91E63")); // Pink
        colors.add(Color.parseColor("#FFFFFF")); // White
        colors.add(Color.parseColor("#F44336")); // Red
        return colors;
    }
}

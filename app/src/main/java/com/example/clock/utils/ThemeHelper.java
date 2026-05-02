package com.example.clock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.List;

public class ThemeHelper {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_NIGHT_MODE = "night_mode";

    // Default Blue
    public static final int DEFAULT_COLOR = Color.parseColor("#FF2196F3");

    // Mode constants
    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    public static void saveAccentColor(Context context, int color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply();
    }

    public static int getAccentColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_COLOR);
    }

    public static void saveNightMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
        applyNightMode(mode);
    }

    public static int getNightMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, MODE_SYSTEM);
    }

    public static void applyNightMode(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static void init(Context context) {
        applyNightMode(getNightMode(context));
    }

    public static void resetToDefault(Context context) {
        saveAccentColor(context, DEFAULT_COLOR);
    }

    public static List<Integer> getPresetColors() {
        List<Integer> colors = new ArrayList<>();
        colors.add(DEFAULT_COLOR);
        colors.add(Color.parseColor("#FF03DAC5"));
        colors.add(Color.parseColor("#FFFF5722"));
        colors.add(Color.parseColor("#FFFFC107"));
        colors.add(Color.parseColor("#FF4CAF50"));
        colors.add(Color.parseColor("#FFE91E63"));
        colors.add(Color.parseColor("#FFFFFF"));
        colors.add(Color.parseColor("#F44336"));
        return colors;
    }
}

package com.example.clock.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import com.example.clock.MainActivity;
import com.example.clock.R;
import com.example.clock.model.Event;
import com.google.gson.Gson;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CountdownWidgetProvider extends AppWidgetProvider {

    private static final String PREF_NAME = "widget_prefs";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Load event for this widget ID / Global
        Event event = loadEventPref(context, appWidgetId);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_countdown);

        if (event != null) {
            views.setTextViewText(R.id.widget_event_name, event.getName());

            long diff = event.getTimestamp() - System.currentTimeMillis();
            if (diff < 0) {
                views.setTextViewText(R.id.widget_time_main, "Done");
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

                String timeString;
                if (days > 0) {
                    timeString = String.format(Locale.getDefault(), "%02dd %02dh", days, hours);
                } else {
                    timeString = String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes);
                }
                views.setTextViewText(R.id.widget_time_main, timeString);
            }
        } else {
            views.setTextViewText(R.id.widget_event_name, "No Event");
            views.setTextViewText(R.id.widget_time_main, "--");
        }

        // Clicking widget opens App
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    // Helper to save which event is associated with which widget
    public static void saveEventPref(Context context, int appWidgetId, Event event) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREF_NAME, 0).edit();
        Gson gson = new Gson();
        String json = gson.toJson(event);
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, json);
        prefs.apply();
    }

    // Helper to delete pref
    public static void deleteEventPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREF_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    // Helper to load event
    public static Event loadEventPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, 0);
        String json = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (json == null) {
            json = prefs.getString("global_widget_event", null);
        }
        if (json != null) {
            return new Gson().fromJson(json, Event.class);
        }
        return null;
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            deleteEventPref(context, appWidgetId);
        }
    }
}

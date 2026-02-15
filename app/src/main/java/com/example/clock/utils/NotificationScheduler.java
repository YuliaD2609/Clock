package com.example.clock.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.example.clock.model.Event;
import com.example.clock.receiver.NotificationReceiver;

public class NotificationScheduler {

    public static final String CHANNEL_ID = "event_channel";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Event Notifications";
            String description = "Notifications for upcoming events";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void scheduleNotification(Context context, Event event) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Permission not granted, cannot schedule exact alarms.
                // In a real app, we should handle this gracefully or ask for permission.
                return;
            }
        }

        // Schedule 10-minute notification
        scheduleAlarm(context, alarmManager, event, 10, "In 10 minutes at " + event.getPlace());

        // Schedule 30-minute notification
        scheduleAlarm(context, alarmManager, event, 30, "In 30 minutes at " + event.getPlace());
    }

    private static void scheduleAlarm(Context context, AlarmManager alarmManager, Event event, int minutesBefore,
            String message) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EVENT_ID, event.getId());
        intent.putExtra(NotificationReceiver.EVENT_NAME, event.getName());
        intent.putExtra(NotificationReceiver.EVENT_PLACE, event.getPlace());
        intent.putExtra(NotificationReceiver.EVENT_MESSAGE, message);

        // Unique data URI for this specific notification time
        intent.setData(Uri.parse("event://" + event.getId() + "/" + minutesBefore));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                minutesBefore, // Use minutesBefore as requestCode to distinguish locally if needed
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = event.getTimestamp() - (long) minutesBefore * 60 * 1000;

        if (triggerTime > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    public static void cancelNotification(Context context, Event event) {
        cancelAlarm(context, event, 10);
        cancelAlarm(context, event, 30);
    }

    private static void cancelAlarm(Context context, Event event, int minutesBefore) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.setData(Uri.parse("event://" + event.getId() + "/" + minutesBefore));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                minutesBefore,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);

        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}

package com.example.clock.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;

import com.example.clock.MainActivity;
import com.example.clock.R;

public class NotificationReceiver extends BroadcastReceiver {

    public static final String EVENT_ID = "event_id";
    public static final String EVENT_NAME = "event_name";
    public static final String EVENT_PLACE = "event_place";
    public static final String EVENT_MESSAGE = "event_message";

    @Override
    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(EVENT_ID);
        String eventName = intent.getStringExtra(EVENT_NAME);
        // String eventPlace = intent.getStringExtra(EVENT_PLACE); // Unused for now if
        // message is provided
        String eventMessage = intent.getStringExtra(EVENT_MESSAGE);

        if (eventName == null)
            return;

        if (eventMessage == null) {
            // Fallback for old pending intents if any
            eventMessage = "Upcoming event";
        }

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "event_channel")
                .setSmallIcon(R.drawable.ic_launcher) // Make sure this resource exists, or use a default one
                .setContentTitle("Upcoming Event: " + eventName)
                .setContentText(eventMessage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Use a unique ID based on event hash or similar to allow multiple
        // notifications
        int notificationId = eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }
}

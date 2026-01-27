package com.example.clock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clock.model.Event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DetailActivity extends AppCompatActivity {

    private TextView nameText;
    private TextView infoText;
    private TextView daysText, hoursText, minutesText, secondsText;
    private android.widget.ImageView pinWidgetBtn;

    private Event event;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_detail);

        nameText = findViewById(R.id.detail_event_name);
        infoText = findViewById(R.id.detail_event_info);
        daysText = findViewById(R.id.countdown_days);
        hoursText = findViewById(R.id.countdown_hours);
        minutesText = findViewById(R.id.countdown_minutes);
        secondsText = findViewById(R.id.countdown_seconds);

        if (getIntent().hasExtra("event")) {
            event = (Event) getIntent().getSerializableExtra("event");
        }

        if (event != null) {
            setupUI();
            startCountdown();
        }

        pinWidgetBtn = findViewById(R.id.btn_pin_widget);
        pinWidgetBtn.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                requestPinWidget();
            }
        });
    }

    private void requestPinWidget() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.appwidget.AppWidgetManager appWidgetManager = getSystemService(
                    android.appwidget.AppWidgetManager.class);
            android.content.ComponentName myProvider = new android.content.ComponentName(this,
                    com.example.clock.widget.CountdownWidgetProvider.class);

            if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                saveEventForWidgets(event);
                appWidgetManager.requestPinAppWidget(myProvider, null, null);
                android.widget.Toast.makeText(DetailActivity.this, "Pinning widget for: " + event.getName(),
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        } else {
            android.widget.Toast.makeText(this, "Widget pinning not supported on this device version",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void saveEventForWidgets(Event event) {
        android.content.SharedPreferences prefs = getSharedPreferences("widget_prefs", 0);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(event);
        prefs.edit().putString("global_widget_event", json).apply();

        android.content.Intent intent = new android.content.Intent(this,
                com.example.clock.widget.CountdownWidgetProvider.class);
        intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = android.appwidget.AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new android.content.ComponentName(getApplication(),
                        com.example.clock.widget.CountdownWidgetProvider.class));
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void setupUI() {
        nameText.setText(event.getName());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        String dateString = sdf.format(new Date(event.getTimestamp()));
        String info = String.format("%s\n%s", event.getPlace(), dateString);
        infoText.setText(info);
    }

    private void startCountdown() {
        handler = new Handler(Looper.getMainLooper());
        runnable = new Runnable() {
            @Override
            public void run() {
                updateTimer();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private void updateTimer() {
        long diff = event.getTimestamp() - System.currentTimeMillis();

        if (diff < 0) {
            daysText.setText("00");
            hoursText.setText("00");
            minutesText.setText("00");
            secondsText.setText("00");
            // Optional: Show "Event Started" or similar
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;

            daysText.setText(String.format(Locale.getDefault(), "%02d", days));
            hoursText.setText(String.format(Locale.getDefault(), "%02d", hours));
            minutesText.setText(String.format(Locale.getDefault(), "%02d", minutes));
            secondsText.setText(String.format(Locale.getDefault(), "%02d", seconds));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}

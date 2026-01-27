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

    private Event event;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

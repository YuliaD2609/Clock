package com.example.clock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clock.adapter.EventAdapter;
import com.example.clock.data.EventRepository;
import com.example.clock.model.Event;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private EventRepository repository;
    private FloatingActionButton fab;

    private android.widget.ImageView historyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        com.example.clock.utils.NotificationScheduler.createNotificationChannel(this);
        checkPermissions();

        repository = new EventRepository(this);

        recyclerView = findViewById(R.id.recycler_view_events);
        fab = findViewById(R.id.fab_add_event);
        historyBtn = findViewById(R.id.btn_history);

        adapter = new EventAdapter();
        // User requested: "events should start from top left" -> Grid with 2 columns
        // seems appropriate or just vertical list.
        // Request also mentioned "horizontal tabs" previously but now wants "start from
        // top left".
        // Let's use GridLayoutManager with 2 columns as planned.
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnEventClickListener(new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("event", event);
                startActivity(intent);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddEventActivity.class));
            }
        });

        historyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistoryActivity.class));
            }
        });

        android.widget.ImageView paletteBtn = findViewById(R.id.btn_palette);
        paletteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker();
            }
        });

        performAppMaintenance();
    }

    private void performAppMaintenance() {
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean migrated = prefs.getBoolean("notifs_migrated_30_min_v2", false);

        List<Event> allEvents = repository.getEvents();
        long now = System.currentTimeMillis();

        // 1. Notification Migration (keep existing logic)
        if (!migrated) {
            for (Event event : allEvents) {
                if (event.getTimestamp() > now) {
                    com.example.clock.utils.NotificationScheduler.scheduleNotification(this, event);
                }
            }
            prefs.edit().putBoolean("notifs_migrated_30_min_v2", true).apply();
            android.widget.Toast
                    .makeText(this, "Notifications updated for all events", android.widget.Toast.LENGTH_SHORT).show();
        }

        // 2. Cleanup old events (> 7 days old)
        // 7 days in ms = 7 * 24 * 60 * 60 * 1000 = 604800000
        long sevenDaysAgo = now - 604800000L;
        // Optimization: check if we need to reload events after cleanup
        int initialSize = allEvents.size();
        repository.deleteEventsOlderThan(sevenDaysAgo);

        // Reload if events might have been deleted to keep memory consistent
        allEvents = repository.getEvents();
        if (allEvents.size() != initialSize) {
            adapter.setEvents(getFutureEvents(allEvents));
        }

        // 3. Link existing events to Calendar if permission granted
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            boolean eventsUpdated = false;
            for (Event event : allEvents) {
                // If event is in future (or recent) and has no ID, try to find it
                if (event.getCalendarEventId() == null && event.getTimestamp() > sevenDaysAgo) {
                    long calId = com.example.clock.utils.CalendarUtils.getCalendarEventId(this, event);
                    if (calId != -1) {
                        event.setCalendarEventId(calId);
                        repository.addEvent(event); // Update repository
                        eventsUpdated = true;
                    }
                }
            }
            if (eventsUpdated) {
                // Refresh list if needed, though addEvent mainly updates backing store
                // Adapter already holds reference to objects? No, adapter has its own list.
                // But we reloaded `allEvents` from repo.
                // Better to just refresh adapter to be sure.
                adapter.setEvents(getFutureEvents(repository.getEvents()));
            }
        }
    }

    private List<Event> getFutureEvents(List<Event> source) {
        List<Event> future = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        for (Event e : source) {
            if (e.getTimestamp() > now) {
                future.add(e);
            }
        }
        return future;
    }

    private void checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = {
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.READ_CALENDAR,
                    android.Manifest.permission.WRITE_CALENDAR
            };

            java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
            for (String permission : permissions) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                        permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
            }
        } else {
            // For older Android versions, just check Calendar permissions
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(new String[] { android.Manifest.permission.READ_CALENDAR,
                        android.Manifest.permission.WRITE_CALENDAR });
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(
                    android.content.Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // ... (existing code)
            }
        }
    }

    private final androidx.activity.result.ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean postNotifsGranted = result.getOrDefault(android.Manifest.permission.POST_NOTIFICATIONS, false);
                Boolean writeCalendarGranted = result.getOrDefault(android.Manifest.permission.WRITE_CALENDAR, false);

                if (Boolean.TRUE.equals(writeCalendarGranted)) {
                    // Permission granted, trigger sync if needed, or just let next launch handle it
                    // Ideally, we reformulate checkAndMigrate to run after permission grant
                    performAppMaintenance();
                }
            });

    private void showColorPicker() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        com.example.clock.utils.ColorWheelView colorWheel = dialogView.findViewById(R.id.color_wheel);

        final androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                this)
                .setTitle("Choose Accent Color")
                .setView(dialogView)
                .setNeutralButton("Reset Default", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        com.example.clock.utils.ThemeHelper.resetToDefault(MainActivity.this);
                        applyTheme();
                        if (adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        colorWheel.setOnColorSelectedListener(new com.example.clock.utils.ColorWheelView.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                com.example.clock.utils.ThemeHelper.saveAccentColor(MainActivity.this, color);
                applyTheme();
                if (adapter != null)
                    adapter.notifyDataSetChanged();
            }
        });

        dialog.show();
    }

    private void applyTheme() {
        int color = com.example.clock.utils.ThemeHelper.getAccentColor(this);
        fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        fab.setColorFilter(android.graphics.Color.WHITE); // Ensure the + icon is white

        android.widget.ImageView paletteBtn = findViewById(R.id.btn_palette);
        if (paletteBtn != null)
            paletteBtn.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

        android.widget.ImageView historyBtn = findViewById(R.id.btn_history);
        if (historyBtn != null)
            historyBtn.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
        performAppMaintenance(); // Check for cleanup/linking on return
        loadEvents();
    }

    private void loadEvents() {
        List<Event> allEvents = repository.getEvents();
        List<Event> futureEvents = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();

        for (Event e : allEvents) {
            if (e.getTimestamp() > now) {
                futureEvents.add(e);
            }
        }
        adapter.setEvents(futureEvents);
    }
}

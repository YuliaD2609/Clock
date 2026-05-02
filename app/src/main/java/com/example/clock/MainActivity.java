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
        com.example.clock.utils.ThemeHelper.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        com.example.clock.utils.NotificationScheduler.createNotificationChannel(this);
        checkPermissions();

        repository = new EventRepository(this);

        // Accesso nascosto per l'admin: pressione prolungata sul titolo
        findViewById(R.id.text_app_title).setOnLongClickListener(v -> {
            if (com.example.clock.utils.PremiumHelper.isAdminUnlocked(this)) {
                startActivity(new android.content.Intent(this, AdminActivity.class));
            } else {
                showAdminLogin();
            }
            return true;
        });

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
                // Conteggio solo eventi attivi (futuri)
                int activeEventsCount = 0;
                long now = System.currentTimeMillis();
                for (com.example.clock.model.Event e : repository.getEvents()) {
                    if (e.getTimestamp() > now) {
                        activeEventsCount++;
                    }
                }

                if (activeEventsCount >= 6
                        && !com.example.clock.utils.PremiumHelper.isEventsUnlocked(MainActivity.this)) {
                    showPremiumDialog("Eventi Illimitati", "1.00€", "https://paypal.me/yulia2609/1.00", "EV");
                } else {
                    startActivity(new Intent(MainActivity.this, AddEventActivity.class));
                }
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
                if (!com.example.clock.utils.PremiumHelper.isColorsUnlocked(MainActivity.this)) {
                    showPremiumDialog("Personalizzazione Colori", "0.50€", "https://paypal.me/yulia2609/0.50", "CO");
                } else {
                    showColorPicker();
                }
            }
        });

        android.widget.ImageView syncBtn = findViewById(R.id.btn_sync);
        if (syncBtn != null) {
            syncBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    syncCalendar();
                }
            });
        }

        android.widget.ImageView donationBtn = findViewById(R.id.btn_donation);
        if (donationBtn != null) {
            donationBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDonationDialog();
                }
            });
        }

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

    private void showDonationDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_donation);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.Button supportButton = dialog.findViewById(R.id.button_support);
        supportButton.setOnClickListener(v -> {
            String paypalLink = "https://paypal.me/yulia2609";
            try {
                android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(paypalLink));
                startActivity(browserIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showPremiumDialog(String featureName, String price, String paypalUrl, String featureType) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_premium);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.TextView titleTv = dialog.findViewById(R.id.premium_title);
        android.widget.TextView descTv = dialog.findViewById(R.id.premium_description);
        android.widget.Button paypalBtn = dialog.findViewById(R.id.btn_paypal_pay);
        android.widget.EditText codeInput = dialog.findViewById(R.id.edit_unlock_code);
        android.widget.Button verifyBtn = dialog.findViewById(R.id.btn_verify_code);
        android.view.View themeLayout = dialog.findViewById(R.id.layout_theme_toggle);
        com.google.android.material.switchmaterial.SwitchMaterial themeSwitch = dialog.findViewById(R.id.switch_theme_mode);
        android.view.View themeSystemBtn = dialog.findViewById(R.id.btn_theme_system);

        titleTv.setText("Sblocca " + featureName);
        descTv.setText("Questa funzione richiede lo sblocco Premium.\nPrezzo: " + price
                + "\nRiceverai il codice via email subito dopo il pagamento.");

        // Mostra lo switch del tema se stiamo parlando della tavolozza (Colori)
        if ("CO".equals(featureType)) {
            themeLayout.setVisibility(android.view.View.VISIBLE);
            
            // Imposta lo stato iniziale basato sul tema attuale
            int currentMode = com.example.clock.utils.ThemeHelper.getNightMode(this);
            themeSystemBtn.setVisibility(currentMode == com.example.clock.utils.ThemeHelper.MODE_SYSTEM ? android.view.View.GONE : android.view.View.VISIBLE);

            if (currentMode == com.example.clock.utils.ThemeHelper.MODE_SYSTEM) {
                int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                themeSwitch.setChecked(nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
            } else {
                themeSwitch.setChecked(currentMode == com.example.clock.utils.ThemeHelper.MODE_DARK);
            }

            themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                com.example.clock.utils.ThemeHelper.saveNightMode(this, isChecked ? 
                    com.example.clock.utils.ThemeHelper.MODE_DARK : com.example.clock.utils.ThemeHelper.MODE_LIGHT);
                themeSystemBtn.setVisibility(android.view.View.VISIBLE);
            });

            themeSystemBtn.setOnClickListener(v -> {
                com.example.clock.utils.ThemeHelper.saveNightMode(this, com.example.clock.utils.ThemeHelper.MODE_SYSTEM);
                themeSystemBtn.setVisibility(android.view.View.GONE);
                // Aggiorna lo switch in base al sistema
                int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                themeSwitch.setChecked(nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
            });
        }

        paypalBtn.setText("Paga ora con PayPal");
        paypalBtn.setOnClickListener(v -> {
            try {
                Intent intentPay = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(paypalUrl));
                startActivity(intentPay);
                android.widget.Toast
                        .makeText(this, "Grazie! Invia una mail a yulia2609@gmail.com per ricevere il codice.",
                                android.widget.Toast.LENGTH_LONG)
                        .show();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "Errore nell'apertura di PayPal", android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
        });

        verifyBtn.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();

            // Se è il codice MASTER, sblocca tutto permanentemente e apri il pannello segreto
            if (code.equalsIgnoreCase(com.example.clock.utils.PremiumHelper.MASTER_ADMIN_CODE)) {
                com.example.clock.utils.PremiumHelper.verifyAndUnlock(this, code, "ADMIN"); // Sblocca tutto
                startActivity(new Intent(this, AdminActivity.class));
                dialog.dismiss();
                return;
            }

            if (com.example.clock.utils.PremiumHelper.verifyAndUnlock(this, code, featureType)) {
                android.widget.Toast
                        .makeText(this, "Funzionalità sbloccata con successo!", android.widget.Toast.LENGTH_LONG)
                        .show();
                applyTheme(); // Refresh UI colors if unlocked colors
                dialog.dismiss();
            } else {
                android.widget.Toast.makeText(this, "Codice non valido per questa funzione", android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
        });

        dialog.show();
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
        com.google.android.material.switchmaterial.SwitchMaterial themeSwitch = dialogView.findViewById(R.id.switch_theme_mode);
        android.view.View themeSystemBtn = dialogView.findViewById(R.id.btn_theme_system);

        // Imposta lo stato iniziale
        int currentMode = com.example.clock.utils.ThemeHelper.getNightMode(this);
        themeSystemBtn.setVisibility(currentMode == com.example.clock.utils.ThemeHelper.MODE_SYSTEM ? android.view.View.GONE : android.view.View.VISIBLE);

        if (currentMode == com.example.clock.utils.ThemeHelper.MODE_SYSTEM) {
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            themeSwitch.setChecked(nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        } else {
            themeSwitch.setChecked(currentMode == com.example.clock.utils.ThemeHelper.MODE_DARK);
        }

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.example.clock.utils.ThemeHelper.saveNightMode(this, isChecked ? 
                com.example.clock.utils.ThemeHelper.MODE_DARK : com.example.clock.utils.ThemeHelper.MODE_LIGHT);
            themeSystemBtn.setVisibility(android.view.View.VISIBLE);
        });

        themeSystemBtn.setOnClickListener(v -> {
            com.example.clock.utils.ThemeHelper.saveNightMode(this, com.example.clock.utils.ThemeHelper.MODE_SYSTEM);
            themeSystemBtn.setVisibility(android.view.View.GONE);
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            themeSwitch.setChecked(nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        });

        final int originalColor = com.example.clock.utils.ThemeHelper.getAccentColor(this);
        final boolean[] isApplied = { false };

        final androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                this)
                .setTitle("Choose Accent Color")
                .setView(dialogView)
                .setNeutralButton("Default", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        isApplied[0] = true;
                        com.example.clock.utils.ThemeHelper.resetToDefault(MainActivity.this);
                        applyTheme();
                        if (adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                })
                .setPositiveButton("Apply", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        isApplied[0] = true;
                    }
                })
                .setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(android.content.DialogInterface dialog) {
                        if (!isApplied[0]) {
                            com.example.clock.utils.ThemeHelper.saveAccentColor(MainActivity.this, originalColor);
                            applyTheme();
                            if (adapter != null)
                                adapter.notifyDataSetChanged();
                        }
                    }
                })
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
        android.widget.ImageView syncBtn = findViewById(R.id.btn_sync);
        if (syncBtn != null)
            syncBtn.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);

        android.widget.ImageView donationBtn = findViewById(R.id.btn_donation);
        if (donationBtn != null)
            donationBtn.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void syncCalendar() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[] { android.Manifest.permission.READ_CALENDAR,
                    android.Manifest.permission.WRITE_CALENDAR });
            return;
        }

        android.widget.Toast.makeText(this, "Syncing events...", android.widget.Toast.LENGTH_SHORT).show();
        List<Event> allEvents = repository.getEvents();
        long now = System.currentTimeMillis();
        int count = 0;

        for (Event event : allEvents) {
            if (event.getTimestamp() > now) {
                boolean success;
                if (event.getCalendarEventId() != null) {
                    success = com.example.clock.utils.CalendarUtils.updateEventInCalendar(this,
                            event.getCalendarEventId(), event);
                    // If update failed (maybe deleted from calendar), try to re-add
                    if (!success) {
                        long newId = com.example.clock.utils.CalendarUtils.addEventToCalendar(this, event);
                        if (newId != -1) {
                            event.setCalendarEventId(newId);
                            repository.addEvent(event);
                            success = true;
                        }
                    }
                } else {
                    long newId = com.example.clock.utils.CalendarUtils.addEventToCalendar(this, event);
                    if (newId != -1) {
                        event.setCalendarEventId(newId);
                        repository.addEvent(event);
                        success = true;
                    } else {
                        success = false;
                    }
                }

                if (success)
                    count++;
            }
        }

        android.widget.Toast
                .makeText(this, "Synced " + count + " events to Calendar", android.widget.Toast.LENGTH_SHORT).show();
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

    private void showAdminLogin() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Password Admin");
        input.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Accesso Riservato")
                .setView(input)
                .setPositiveButton("Entra", (dialog, which) -> {
                String pass = input.getText().toString().trim();
                if (pass.equalsIgnoreCase(com.example.clock.utils.PremiumHelper.MASTER_ADMIN_CODE)) {
                    com.example.clock.utils.PremiumHelper.verifyAndUnlock(this, pass, "ADMIN"); // Sblocca permanentemente
                    startActivity(new android.content.Intent(this, AdminActivity.class));
                } else {
                        android.widget.Toast.makeText(this, "Password errata", android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }
}

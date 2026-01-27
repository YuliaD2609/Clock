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
    }

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

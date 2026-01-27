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
        final java.util.List<Integer> colors = com.example.clock.utils.ThemeHelper.getPresetColors();
        String[] colorNames = new String[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            if (colors.get(i) == com.example.clock.utils.ThemeHelper.DEFAULT_COLOR)
                colorNames[i] = "Default (Purple)";
            else
                colorNames[i] = String.format("#%06X", (0xFFFFFF & colors.get(i)));
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Choose Accent Color")
                .setItems(colorNames, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        com.example.clock.utils.ThemeHelper.saveAccentColor(MainActivity.this, colors.get(which));
                        applyTheme();
                        if (adapter != null)
                            adapter.notifyDataSetChanged(); // triggers re-bind with new color
                    }
                })
                .setNeutralButton("Reset Default", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        com.example.clock.utils.ThemeHelper.resetToDefault(MainActivity.this);
                        applyTheme();
                        if (adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                })
                .show();
    }

    private void applyTheme() {
        int color = com.example.clock.utils.ThemeHelper.getAccentColor(this);
        fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        // Also update adapter color? Adapter needs to fetch color in onBindViewHolder
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

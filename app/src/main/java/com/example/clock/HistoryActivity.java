package com.example.clock;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clock.adapter.EventAdapter;
import com.example.clock.data.EventRepository;
import com.example.clock.model.Event;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private EventRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        repository = new EventRepository(this);
        recyclerView = findViewById(R.id.recycler_view_history);
        adapter = new EventAdapter();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // On history click, maybe just show detail (countdown will be negative/done)
        adapter.setOnEventClickListener(new EventAdapter.OnEventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Intent intent = new Intent(HistoryActivity.this, DetailActivity.class);
                intent.putExtra("event", event);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryEvents();
    }

    private void loadHistoryEvents() {
        List<Event> allEvents = repository.getEvents();
        List<Event> pastEvents = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Event e : allEvents) {
            if (e.getTimestamp() <= now) {
                pastEvents.add(e);
            }
        }
        adapter.setEvents(pastEvents);
    }
}

package com.example.clock.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.clock.model.Event;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventRepository {
    private static final String PREF_NAME = "clock_prefs";
    private static final String KEY_EVENTS = "events";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public EventRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Event> getEvents() {
        String json = sharedPreferences.getString(KEY_EVENTS, null);
        List<Event> events = new ArrayList<>();
        if (json != null) {
            Type type = new TypeToken<List<Event>>() {
            }.getType();
            events = gson.fromJson(json, type);
        }
        // Always return sorted
        Collections.sort(events);
        return events;
    }

    public void addEvent(Event event) {
        List<Event> events = getEvents();
        boolean found = false;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) {
                events.set(i, event);
                found = true;
                break;
            }
        }
        if (!found) {
            events.add(event);
        }
        saveEvents(events);
    }

    public void deleteEvent(Event event) {
        List<Event> events = getEvents();
        // Remove based on ID logic if we had equals implemented, or just naive generic
        // remove for now
        // Implementing naive remove by ID
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(event.getId())) {
                events.remove(i);
                break;
            }
        }
        saveEvents(events);
    }

    private void saveEvents(List<Event> events) {
        Collections.sort(events);
        String json = gson.toJson(events);
        sharedPreferences.edit().putString(KEY_EVENTS, json).apply();
    }
}

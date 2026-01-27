package com.example.clock.model;

import java.io.Serializable;
import java.util.UUID;

public class Event implements Serializable, Comparable<Event> {
    private String id;
    private String name;
    private String place;
    private long timestamp; // Milliseconds since epoch

    public Event(String name, String place, long timestamp) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.place = place;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlace() {
        return place;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Event o) {
        // Sort by proximity in time (ascending timestamp)
        return Long.compare(this.timestamp, o.timestamp);
    }
}

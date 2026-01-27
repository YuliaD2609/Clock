package com.example.clock.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clock.R;
import com.example.clock.model.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView countdownText;
        private android.widget.ImageView clockIcon;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_event_name);
            countdownText = itemView.findViewById(R.id.text_event_countdown);
            clockIcon = itemView.findViewById(R.id.image_clock);
        }

        public void bind(final Event event, final OnEventClickListener listener) {
            // Apply Theme
            int color = com.example.clock.utils.ThemeHelper.getAccentColor(itemView.getContext());
            if (clockIcon != null) {
                clockIcon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }

            nameText.setText(event.getName());

            long diff = event.getTimestamp() - System.currentTimeMillis();
            if (diff < 0) {
                countdownText.setText("Done");
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(diff);
                long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

                String timeString = String.format(Locale.getDefault(), "%02dd %02dh %02dm", days, hours, minutes);
                countdownText.setText(timeString);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onEventClick(event);
                    }
                }
            });
        }
    }
}

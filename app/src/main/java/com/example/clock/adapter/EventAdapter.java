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
            // Assuming item_event.xml has an ImageView for the clock?
            // We need to check item_event.xml content first or assume ID.
            // Earlier write_to_file for item_event didn't explicitly show clock icon ID but
            // said "with a clock icon".
            // Let's assume ID is `image_clock` or view file if unsure.
            // I'll assume I need to find the ID.
            // Wait, I should view item_event.xml to be sure.
            // Actually, I'll view it in next turn if I fail, but let's check content.
            // I will use `view_file` on `item_event.xml` first to be safe.
        }

        public void bind(final Event event, final OnEventClickListener listener) {
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

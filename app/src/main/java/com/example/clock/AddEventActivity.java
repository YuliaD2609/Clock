package com.example.clock;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clock.data.EventRepository;
import com.example.clock.model.Event;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText placeInput;
    private Button dateButton;
    private Button timeButton;
    private Button saveButton;

    private Calendar selectedCalendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    private Event eventToEdit;

    private android.widget.CheckBox temporaryCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        nameInput = findViewById(R.id.edit_event_name);
        placeInput = findViewById(R.id.edit_event_place);
        temporaryCheckbox = findViewById(R.id.checkbox_temporary);
        dateButton = findViewById(R.id.btn_pick_date);
        timeButton = findViewById(R.id.btn_pick_time);
        saveButton = findViewById(R.id.btn_save_event);

        selectedCalendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (getIntent().hasExtra("event")) {
            eventToEdit = (Event) getIntent().getSerializableExtra("event");
            nameInput.setText(eventToEdit.getName());
            placeInput.setText(eventToEdit.getPlace());
            temporaryCheckbox.setChecked(eventToEdit.isTemporary());
            selectedCalendar.setTimeInMillis(eventToEdit.getTimestamp());
            saveButton.setText("Update");
        }

        updateDateButton();
        updateTimeButton();

        applyTheme();

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEvent();
            }
        });
    }

    private void applyTheme() {
        int color = com.example.clock.utils.ThemeHelper.getAccentColor(this);
        saveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        dateButton.setTextColor(color);
        timeButton.setTextColor(color);
        temporaryCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(color));
        // Also could tint the input underlines or cursor if possible, but buttons are
        // main thing.
    }

    private void showDatePicker() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateButton();
            }
        }, selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedCalendar.set(Calendar.MINUTE, minute);
                updateTimeButton();
            }
        }, selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE), true).show();
    }

    private void updateDateButton() {
        dateButton.setText(dateFormat.format(selectedCalendar.getTime()));
    }

    private void updateTimeButton() {
        timeButton.setText(timeFormat.format(selectedCalendar.getTime()));
    }

    private void saveEvent() {
        String name = nameInput.getText().toString().trim();
        String place = placeInput.getText().toString().trim();
        boolean isTemporary = temporaryCheckbox.isChecked();

        if (name.isEmpty()) {
            nameInput.setError("Name required");
            return;
        }

        EventRepository repository = new EventRepository(this);

        boolean calendarSuccess = false;
        boolean syncAttempted = false;

        // Logic:
        // Use eventToEdit if it exists, else new Event.
        // If event has calendarEventId, try UPDATE.
        // If update fails or ID is null, try INSERT.
        // If INSERT succeeds, update the ID in the event.

        Event eventWorkingCopy;
        if (eventToEdit != null) {
            eventToEdit.setName(name);
            eventToEdit.setPlace(place);
            eventToEdit.setTimestamp(selectedCalendar.getTimeInMillis());
            eventToEdit.setTemporary(isTemporary);
            eventWorkingCopy = eventToEdit;
        } else {
            eventWorkingCopy = new Event(name, place, selectedCalendar.getTimeInMillis());
            eventWorkingCopy.setTemporary(isTemporary);
        }

        // Calendar Sync Logic
        if (!isTemporary) {
            syncAttempted = true;
            if (eventWorkingCopy.getCalendarEventId() != null) {
                boolean updated = com.example.clock.utils.CalendarUtils.updateEventInCalendar(this,
                        eventWorkingCopy.getCalendarEventId(), eventWorkingCopy);
                if (updated) {
                    calendarSuccess = true;
                } else {
                    // Maybe it was deleted? Try to re-add?
                    // Let's try to add as new if update failed
                    long newId = com.example.clock.utils.CalendarUtils.addEventToCalendar(this, eventWorkingCopy);
                    if (newId != -1) {
                        eventWorkingCopy.setCalendarEventId(newId);
                        calendarSuccess = true;
                    }
                }
            } else {
                long newId = com.example.clock.utils.CalendarUtils.addEventToCalendar(this, eventWorkingCopy);
                if (newId != -1) {
                    eventWorkingCopy.setCalendarEventId(newId);
                    calendarSuccess = true;
                }
            }
        } else {
            // It is temporary, so no sync.
            // Requirement check: "not from the calendar".
            // If it WAS in the calendar (calendarEventId != null), we might want to leave
            // it or remove it?
            // The request says "if is checked the event is not added to the calendar".
            // It implies for NEW events or updates where we don't want it synced.
            // For now, we just skip the sync call.
        }

        // Save to repo (including new/updated ID and temporary flag)
        repository.addEvent(eventWorkingCopy);
        com.example.clock.utils.NotificationScheduler.scheduleNotification(this, eventWorkingCopy);

        if (syncAttempted) {
            if (calendarSuccess) {
                Toast.makeText(this, "Event synced to Calendar", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Saved to app, but failed to sync to Calendar", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Temporary event saved (App only)", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}

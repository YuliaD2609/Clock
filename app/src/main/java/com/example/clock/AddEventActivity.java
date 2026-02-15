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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        nameInput = findViewById(R.id.edit_event_name);
        placeInput = findViewById(R.id.edit_event_place);
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

        if (name.isEmpty()) {
            nameInput.setError("Name required");
            return;
        }

        EventRepository repository = new EventRepository(this);

        if (eventToEdit != null) {
            eventToEdit.setName(name);
            eventToEdit.setPlace(place);
            eventToEdit.setTimestamp(selectedCalendar.getTimeInMillis());
            repository.addEvent(eventToEdit); // This will update because ID matches
            com.example.clock.utils.NotificationScheduler.scheduleNotification(this, eventToEdit);
            com.example.clock.utils.CalendarUtils.addEventToCalendar(this, eventToEdit);
        } else {
            Event event = new Event(name, place, selectedCalendar.getTimeInMillis());
            repository.addEvent(event);
            com.example.clock.utils.NotificationScheduler.scheduleNotification(this, event);
            com.example.clock.utils.CalendarUtils.addEventToCalendar(this, event);
        }

        finish();
    }
}

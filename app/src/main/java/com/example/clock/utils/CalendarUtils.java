package com.example.clock.utils;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import androidx.core.content.ContextCompat;

import com.example.clock.model.Event;

import java.util.Calendar;
import java.util.TimeZone;

public class CalendarUtils {

    public static long addEventToCalendar(Context context, Event event) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }

        long calID = getPrimaryCalendarId(context);
        if (calID == -1) {
            calID = getAnyWritableCalendarId(context);
            if (calID == -1)
                return -1;
        }

        // Prevent duplicates: Check if event already exists
        long existingId = getCalendarEventId(context, event);
        if (existingId != -1) {
            return existingId; // Already exists, return its ID
        }

        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, event.getTimestamp());
        values.put(CalendarContract.Events.DTEND, event.getTimestamp() + 60 * 60 * 1000); // Assume 1 hour duration
        values.put(CalendarContract.Events.TITLE, event.getName());
        values.put(CalendarContract.Events.DESCRIPTION, "Added from Clock App");
        values.put(CalendarContract.Events.EVENT_LOCATION, event.getPlace());
        values.put(CalendarContract.Events.CALENDAR_ID, calID);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

        try {
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            if (uri != null) {
                return Long.parseLong(uri.getLastPathSegment());
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean updateEventInCalendar(Context context, long eventId, Event event) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, event.getTimestamp());
        values.put(CalendarContract.Events.DTEND, event.getTimestamp() + 60 * 60 * 1000);
        values.put(CalendarContract.Events.TITLE, event.getName());
        values.put(CalendarContract.Events.EVENT_LOCATION, event.getPlace());

        Uri updateUri = android.content.ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);

        try {
            int rows = cr.update(updateUri, values, null, null);
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static long getCalendarEventId(Context context, Event event) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }

        // We need a calendar ID to check against, or we check all?
        // Best to check all or the one we would write to.
        // Let's check the one we would write to first.
        long calID = getPrimaryCalendarId(context);
        if (calID == -1)
            calID = getAnyWritableCalendarId(context);
        if (calID == -1)
            return -1;

        String[] projection = new String[] { CalendarContract.Events._ID };
        String selection = CalendarContract.Events.CALENDAR_ID + " = ? AND " +
                CalendarContract.Events.TITLE + " = ? AND " +
                CalendarContract.Events.DTSTART + " = ?";
        String[] selectionArgs = new String[] {
                String.valueOf(calID),
                event.getName(),
                String.valueOf(event.getTimestamp())
        };

        try (Cursor cursor = context.getContentResolver().query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static long getPrimaryCalendarId(Context context) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }

        String[] projection = new String[] {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.IS_PRIMARY
        };

        // Try to find the primary calendar first
        try (Cursor cursor = context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.IS_PRIMARY + " = 1 AND " +
                        CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= "
                        + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static long getAnyWritableCalendarId(Context context) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }

        String[] projection = new String[] { CalendarContract.Calendars._ID };

        try (Cursor cursor = context.getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= "
                        + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                // Just use the first writable calendar found
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}

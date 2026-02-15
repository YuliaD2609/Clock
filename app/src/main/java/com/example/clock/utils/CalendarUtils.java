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

    public static void addEventToCalendar(Context context, Event event) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        long calID = getPrimaryCalendarId(context);
        if (calID == -1) {
            // No primary calendar found, maybe fallback to first available or error?
            // For now, let's try to find ANY writable calendar
            calID = getAnyWritableCalendarId(context);
            if (calID == -1)
                return;
        }

        // Prevent duplicates: Check if event already exists
        if (eventExists(context, calID, event)) {
            return;
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
            // long eventID = Long.parseLong(uri.getLastPathSegment());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private static boolean eventExists(Context context, long calID, Event event) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

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
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
                        CalendarContract.Calendars.ACCESS_LEVEL + " >= "
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
                CalendarContract.Calendars.ACCESS_LEVEL + " >= " + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
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

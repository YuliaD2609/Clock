package com.example.clock.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    public static void init(Context context) {
        String deviceId = PremiumHelper.getDeviceId(context);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(deviceId);

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean eventsUnlocked = snapshot.child("unlocked_events").getValue(Boolean.class) != null && 
                                            snapshot.child("unlocked_events").getValue(Boolean.class);
                    boolean colorsUnlocked = snapshot.child("unlocked_colors").getValue(Boolean.class) != null && 
                                            snapshot.child("unlocked_colors").getValue(Boolean.class);

                    // Sincronizziamo con lo stato locale in PremiumHelper (SharedPreferences)
                    syncLocalStatus(context, eventsUnlocked, colorsUnlocked);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Firebase Load Error", error.toException());
            }
        });
    }

    private static void syncLocalStatus(Context context, boolean events, boolean colors) {
        // Se Firebase dice che è sbloccato, aggiorniamo il locale
        if (events) {
            context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("events_unlocked", true).apply();
        }
        if (colors) {
            context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean("colors_unlocked", true).apply();
        }
    }
}

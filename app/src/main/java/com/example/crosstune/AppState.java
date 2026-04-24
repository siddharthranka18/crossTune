package com.example.crosstune;

import android.content.Context;

public class AppState {
    // Static variables belong to the app, not just one screen
    // This will stay 'true' until the user fully closes the app
    public static boolean isPlaylistDataFetched = false;
    public static SessionManager sessionManager;
    public static void init(Context context) {
        if (sessionManager == null) {
            sessionManager = new SessionManager(context);
        }
    }
}
package com.example.crosstune;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "crosstune_session";

    // Keys
    private static final String KEY_GOOGLE_NAME = "google_name";
    private static final String KEY_GOOGLE_EMAIL = "google_email";
    private static final String KEY_GOOGLE_UID = "google_uid";
    private static final String KEY_SPOTIFY_TOKEN = "spotify_token";
    private static final String KEY_SPOTIFY_TOKEN_EXPIRY = "spotify_expiry";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- GOOGLE SETTERS & GETTERS ---

    public void saveGoogleUser(String name, String email, String uid) {
        prefs.edit()
                .putString(KEY_GOOGLE_NAME, name)
                .putString(KEY_GOOGLE_EMAIL, email)
                .putString(KEY_GOOGLE_UID, uid)
                .apply();
    }

    public String getGoogleName() {
        return prefs.getString(KEY_GOOGLE_NAME, "User");
    }

    public String getGoogleEmail() {
        return prefs.getString(KEY_GOOGLE_EMAIL, "");
    }

    public String getGoogleUid() {
        return prefs.getString(KEY_GOOGLE_UID, "");
    }

    // --- SPOTIFY SETTERS & GETTERS ---

    public void saveSpotifyToken(String token, int expiresInSeconds) {
        // Calculate exact time it expires
        long expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000L);

        prefs.edit()
                .putString(KEY_SPOTIFY_TOKEN, token)
                .putLong(KEY_SPOTIFY_TOKEN_EXPIRY, expiryTime)
                .apply();
    }

    public String getSpotifyToken() {
        return prefs.getString(KEY_SPOTIFY_TOKEN, null);
    }

    // Simple check to see if we have a token that hasn't expired yet
    public boolean isSpotifyConnected() {
        String token = getSpotifyToken();
        long expiry = prefs.getLong(KEY_SPOTIFY_TOKEN_EXPIRY, 0L);
        return token != null && System.currentTimeMillis() < expiry;
    }

    // --- UTILITY ---

    public void clearAll() {
        prefs.edit().clear().apply();
        // Also reset the global flag we talked about
        AppState.isPlaylistDataFetched = false;
    }
}
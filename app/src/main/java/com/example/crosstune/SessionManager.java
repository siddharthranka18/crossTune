package com.example.crosstune;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "crosstune_session";

    private static final String KEY_GOOGLE_NAME = "google_name";
    private static final String KEY_GOOGLE_EMAIL = "google_email";
    private static final String KEY_GOOGLE_UID = "google_uid";

    private static final String KEY_SPOTIFY_TOKEN = "spotify_token";
    private static final String KEY_SPOTIFY_TOKEN_EXPIRY_MS = "spotify_token_expiry_ms";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveGoogleUser(String name, String email, String uid) {
        prefs.edit()
                .putString(KEY_GOOGLE_NAME, sanitize(name, "Demo User"))
                .putString(KEY_GOOGLE_EMAIL, sanitize(email, "demo@crosstune.com"))
                .putString(KEY_GOOGLE_UID, sanitize(uid, ""))
                .apply();
    }

    public void saveSpotifyToken(String accessToken, int expiresInSeconds) {
        long expiryMs = System.currentTimeMillis() + (expiresInSeconds * 1000L);
        prefs.edit()
                .putString(KEY_SPOTIFY_TOKEN, sanitize(accessToken, ""))
                .putLong(KEY_SPOTIFY_TOKEN_EXPIRY_MS, expiryMs)
                .apply();
    }

    public String getGoogleName() {
        return prefs.getString(KEY_GOOGLE_NAME, "Demo User");
    }

    public String getGoogleEmail() {
        return prefs.getString(KEY_GOOGLE_EMAIL, "demo@crosstune.com");
    }

    public String getGoogleUid() {
        return prefs.getString(KEY_GOOGLE_UID, "");
    }

    public String getSpotifyToken() {
        return prefs.getString(KEY_SPOTIFY_TOKEN, "");
    }

    public boolean hasValidSpotifyToken() {
        String token = getSpotifyToken();
        long expiry = prefs.getLong(KEY_SPOTIFY_TOKEN_EXPIRY_MS, 0L);
        return token != null && !token.isEmpty() && System.currentTimeMillis() < expiry;
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    public ProfileUiState getProfileUiState() {
        return new ProfileUiState(getGoogleName(), getGoogleEmail(), hasValidSpotifyToken());
    }

    private String sanitize(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static class ProfileUiState {
        public final String name;
        public final String email;
        public final boolean spotifyConnected;

        public ProfileUiState(String name, String email, boolean spotifyConnected) {
            this.name = name;
            this.email = email;
            this.spotifyConnected = spotifyConnected;
        }
    }
}
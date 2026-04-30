package com.example.crossTune;

import androidx.annotation.Nullable;
import java.util.Objects;

public class Song {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String thumbnailUrl;

    // Playback & Offline State
    private String streamUrl; // Caches the direct 320kbps JioSaavn URL for zero-delay playback
    private String localPath; // For Offline Downloads

    // Metadata
    private long duration; // Stored in seconds.

    public Song(String id, String title, String artist, String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = "Unknown Album"; // Default fallback
        this.thumbnailUrl = thumbnailUrl;
        this.streamUrl = null;
        this.localPath = null;
        this.duration = 0;
    }

    // Overloaded constructor for the full JioSaavn API data
    public Song(String id, String title, String artist, String album, String thumbnailUrl, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.streamUrl = null;
        this.localPath = null;
    }

    // --- GETTERS & SETTERS ---

    public String getId() { return id; }

    // Alias to prevent the rest of the app from crashing until we update the other files
    public String getVideoId() { return id; }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getThumbnailUrl() { return thumbnailUrl; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    @Nullable
    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    @Nullable
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    // --- HELPERS ---

    // Allows the Queue Engine to perfectly match songs
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return id.equals(song.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

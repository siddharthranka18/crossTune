package com.example.crosstune;

public class Playlist {
    public String id;
    public String name;
    public PlaylistImage[] images;

    // In 2026, track count is found under 'item_count'
    public TracksInfo tracks;

    public static class PlaylistImage {
        public String url;
    }

    public static class TracksInfo {
        public int total;
    }
}

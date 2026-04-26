package com.example.crossTune;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private boolean isDeletable;
    private List<Song> songs;

    public Playlist(String id, String name, boolean isDeletable) {
        this.id = id;
        this.name = name;
        this.isDeletable = isDeletable;
        this.songs = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isDeletable() { return isDeletable; }
    public List<Song> getSongs() { return songs; }
    public void setSongs(List<Song> songs) { this.songs = songs; }
}
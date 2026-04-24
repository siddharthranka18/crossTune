package com.example.crosstune;

public class PlaylistModel {
    private String image;
    private String title;
    private String artist;

    public PlaylistModel(String image, String title, String artist) {
        this.image = image;
        this.title = title;
        this.artist = artist;
    }

    // These are the "Getters" your adapter is looking for:
    public String getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }
}
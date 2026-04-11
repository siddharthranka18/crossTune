package com.example.crosstune;

public class LibraryModel {
    private String title;
    private String artist;
    private int imageResource;

    public LibraryModel(String title, String artist, int imageResource) {
        this.title = title;
        this.artist = artist;
        this.imageResource = imageResource;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public int getImageResource() { return imageResource; }
}
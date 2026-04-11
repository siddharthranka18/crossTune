package com.example.crosstune;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NowPlayingActivity extends AppCompatActivity {

    public static final String EXTRA_SONG_TITLE = "extra_song_title";
    public static final String EXTRA_ARTIST_NAME = "extra_artist_name";
    public static final String EXTRA_ALBUM_IMAGE = "extra_album_image";

    private ImageView imgAlbumArt, btnBack, btnPlayPause, btnPrevious, btnNext, btnFavourite;
    private ImageView imgAlbumLeft, imgAlbumRight;
    private TextView tvSongTitle, tvArtistName, tvCurrentTime, tvRemainingTime;
    private CurvedSeekBar curvedSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        // Bind views
        btnBack = findViewById(R.id.btnBack);
        imgAlbumArt = findViewById(R.id.imgAlbumArt);
        imgAlbumLeft = findViewById(R.id.imgAlbumLeft);
        imgAlbumRight = findViewById(R.id.imgAlbumRight);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvArtistName = findViewById(R.id.tvArtistName);
        btnFavourite = findViewById(R.id.btnFavourite);
        curvedSeekBar = findViewById(R.id.curvedSeekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvRemainingTime = findViewById(R.id.tvRemainingTime);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);

        // Get data from intent
        String songTitle = getIntent().getStringExtra(EXTRA_SONG_TITLE);
        String artistName = getIntent().getStringExtra(EXTRA_ARTIST_NAME);
        int albumImage = getIntent().getIntExtra(EXTRA_ALBUM_IMAGE, 0);

        // Set song info
        if (songTitle != null) {
            tvSongTitle.setText(songTitle);
        }
        if (artistName != null) {
            tvArtistName.setText(artistName);
        }

        // Set album art
        if (albumImage != 0) {
            imgAlbumArt.setImageResource(albumImage);
        }

        // Set default seekbar progress
        curvedSeekBar.setMax(100);
        curvedSeekBar.setProgress(35);

        // Back button closes this activity
        btnBack.setOnClickListener(v -> finish());

        // TODO: Wire up playback controls (play/pause, next, previous)
        // TODO: Wire up seekbar listener for actual song progress
        // TODO: Set left/right album images for carousel effect
    }
}

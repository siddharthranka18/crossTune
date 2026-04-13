package com.example.crosstune;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class NowPlayingActivity extends AppCompatActivity {

    public static final String EXTRA_SONG_TITLE = "extra_song_title";
    public static final String EXTRA_ARTIST_NAME = "extra_artist_name";
    public static final String EXTRA_ALBUM_IMAGE = "extra_album_image";

    private ImageView imgAlbumArt, btnPlayPause, btnBack, btnPrevious, btnNext;
    private TextView tvSongTitle, tvArtistName;
    private boolean isPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        imgAlbumArt = findViewById(R.id.imgAlbumArt);
        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvArtistName = findViewById(R.id.tvArtistName);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnBack = findViewById(R.id.btnBack);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        String title = getIntent().getStringExtra(EXTRA_SONG_TITLE);
        String artist = getIntent().getStringExtra(EXTRA_ARTIST_NAME);
        int imageRes = getIntent().getIntExtra(EXTRA_ALBUM_IMAGE, 0);

        tvSongTitle.setText(title);
        tvArtistName.setText(artist);
        if (imageRes != 0) {
            imgAlbumArt.setImageResource(imageRes);
        }

        btnBack.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                // Assuming you have an ic_pause drawable, if not use ic_play for now or find it
                btnPlayPause.setImageResource(R.drawable.ic_play); 
            }
            isPlaying = !isPlaying;
        });
    }
}

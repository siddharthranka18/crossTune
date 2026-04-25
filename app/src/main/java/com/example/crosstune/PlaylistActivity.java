package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class PlaylistActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        // 1. Get Data from Intent
        String title = getIntent().getStringExtra("playlist_title");
        String image = getIntent().getStringExtra("playlist_image");

        // 2. Bind UI elements
        TextView tvTitle = findViewById(R.id.tvPlaylistTitle);
        ImageView heroImage = findViewById(R.id.playlistHeroImage);
        ImageView btnBack = findViewById(R.id.btnBack);

        // 3. Set Data
        if (title != null) {
            tvTitle.setText(title);
        }
        
        if (image != null && !image.isEmpty()) {
            Glide.with(this)
                    .load(image)
                    .placeholder(R.drawable.playlist_hero)
                    .into(heroImage);
        }

        // 4. Back Button Logic
        btnBack.setOnClickListener(v -> finish());

        // 5. Home Navigation (if exists in bottom nav include)
        View btnHome = findViewById(R.id.btnHome);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(PlaylistActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }
}
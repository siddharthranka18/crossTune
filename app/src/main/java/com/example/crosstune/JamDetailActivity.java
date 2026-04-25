package com.example.crosstune;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class JamDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jam_detail);

        // 1. Get Data from Intent
        String jamTitle = getIntent().getStringExtra("jam_title");
        String jamArtist = getIntent().getStringExtra("jam_artist");
        int jamImage = getIntent().getIntExtra("jam_image", R.drawable.ic_login_hero);

        // 2. Bind UI
        TextView tvTitle = findViewById(R.id.tvJamTitle);
        TextView tvSub = findViewById(R.id.tvJamSub);
        ImageView heroImage = findViewById(R.id.jamHeroImage);
        ImageView btnBack = findViewById(R.id.btnBack);

        if (jamTitle != null) tvTitle.setText(jamTitle);
        if (jamArtist != null) tvSub.setText("Curated by " + jamArtist);
        heroImage.setImageResource(jamImage);

        // 3. Back Button
        btnBack.setOnClickListener(v -> finish());
    }
}
package com.example.crosstune;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private boolean isPlatformExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Back Button Logic
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 2. Platform Selection Logic
        RelativeLayout btnChoosePlatform = findViewById(R.id.btnChoosePlatform);
        LinearLayout platformOptionsContainer = findViewById(R.id.platformOptionsContainer);
        ImageView ivPlatformArrow = findViewById(R.id.ivPlatformArrow);
        RelativeLayout optionSpotify = findViewById(R.id.optionSpotify);

        btnChoosePlatform.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlatformExpanded) {
                    platformOptionsContainer.setVisibility(View.GONE);
                    ivPlatformArrow.animate().rotation(0).setDuration(200).start();
                } else {
                    platformOptionsContainer.setVisibility(View.VISIBLE);
                    ivPlatformArrow.animate().rotation(90).setDuration(200).start();
                }
                isPlatformExpanded = !isPlatformExpanded;
            }
        });

        optionSpotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this, "Spotify Selected", Toast.LENGTH_SHORT).show();
                // Add your Spotify integration logic here
            }
        });

        // 3. Logout Logic
        TextView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this, "LOGGED OUT SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
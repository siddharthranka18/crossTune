package com.example.crosstune;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Normal Sign In
        findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            startMainActivity("Logged in via Email");
        });

        // 2. Spotify (Temporary placeholder)
        findViewById(R.id.btnSpotifyDummy).setOnClickListener(v -> {
            startMainActivity("Logged in via Spotify");
        });

        // 3. YouTube (Temporary placeholder)
        findViewById(R.id.btnYTDummy).setOnClickListener(v -> {
            startMainActivity("Logged in via YT Music");
        });
    }

    private void startMainActivity(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish(); // Closes Login screen so they can't go back
    }
}
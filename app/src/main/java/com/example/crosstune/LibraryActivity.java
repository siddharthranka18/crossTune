package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        // 1. Navigation
       // findViewById(R.id.ic_back).setOnClickListener(v -> finish());
        
        View homeBtn = findViewById(R.id.btn_home_nav);
        if (homeBtn != null) {
            homeBtn.setOnClickListener(v -> {
                Intent intent = new Intent(LibraryActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish(); // Close LibraryActivity as we go back home
            });
        }

        // 2. Setup Recyclers
        setupRecentPlaylists();
        setupJamGroups();
        setupFriends();
    }

    private void setupRecentPlaylists() {
        RecyclerView rvPlaylists = findViewById(R.id.rvLibraryPlaylists);
        List<LibraryModel> playlistData = new ArrayList<>();
        playlistData.add(new LibraryModel("After Hours", "The Weeknd", R.drawable.after_hours));
        playlistData.add(new LibraryModel("Radical Optimism", "Dua Lipa", R.drawable.radical_optimism));
        playlistData.add(new LibraryModel("TTPD", "Taylor Swift", R.drawable.tortured));
        playlistData.add(new LibraryModel("Truly Yours", "Eric Bellinger", R.drawable.trulyyours));

        LibraryAdapter adapter = new LibraryAdapter(playlistData);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPlaylists.setAdapter(adapter);
    }

    private void setupJamGroups() {
        RecyclerView rvJam = findViewById(R.id.rvJamGroups);
        List<LibraryModel> jamList = new ArrayList<>();
        jamList.add(new LibraryModel("Taylor Swift", "Artist", R.drawable.taylorswift));
        jamList.add(new LibraryModel("Eminem", "Artist", R.drawable.eminem));
        jamList.add(new LibraryModel("The Weeknd", "Artist", R.drawable.after_hours));
        jamList.add(new LibraryModel("Dua Lipa", "Artist", R.drawable.radical_optimism));
        jamList.add(new LibraryModel("Shivam", "Artist", R.drawable.mybeat));
        jamList.add(new LibraryModel("Gunna", "Artist", R.drawable.carsick));

        CircleArtistAdapter adapter = new CircleArtistAdapter(jamList);
        rvJam.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvJam.setAdapter(adapter);
    }

    private void setupFriends() {
        RecyclerView rvFriends = findViewById(R.id.rvFriends);
        List<LibraryModel> friendsList = new ArrayList<>();
        friendsList.add(new LibraryModel("The Weeknd", "Friend", R.drawable.after_hours));
        friendsList.add(new LibraryModel("Taylor Swift", "Friend", R.drawable.taylorswift));
        friendsList.add(new LibraryModel("Dua Lipa", "Friend", R.drawable.radical_optimism));
        friendsList.add(new LibraryModel("Eminem", "Friend", R.drawable.eminem));
        friendsList.add(new LibraryModel("Gunna", "Friend", R.drawable.carsick));
        friendsList.add(new LibraryModel("Shivam", "Friend", R.drawable.mybeat));

        CircleArtistAdapter adapter = new CircleArtistAdapter(friendsList);
        rvFriends.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFriends.setAdapter(adapter);
    }
}
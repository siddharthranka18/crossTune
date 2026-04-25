package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";

    private PlaylistAdapter playlistAdapter;
    private List<PlaylistModel> playlistList = new ArrayList<>();
    private SpotifyApiService apiService;

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        AppState.init(this);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(SpotifyApiService.class);

        setupPlaylistRecycler();
        setupJamRecycler();

        findViewById(R.id.ic_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.btn_library_nav).setOnClickListener(v -> {
            startActivity(new Intent(this, LibraryActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        String token = AppState.sessionManager.getSpotifyToken();

        if (token != null && !token.isEmpty()) {
            fetchSpotifyPlaylists(token);
        } else {
            addPlaceholderPlaylists();
        }
    }

    private void addPlaceholderPlaylists() {
        playlistList.clear();
        playlistList.add(new PlaylistModel("https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17", "Chill Beats", "Connect Spotify to see yours"));
        playlistList.add(new PlaylistModel("https://images.unsplash.com/photo-1493225255756-d9584f8606e9", "Top Charts", "CrossTune Original"));
        playlistAdapter.notifyDataSetChanged();
    }

    private void fetchSpotifyPlaylists(String token) {
        apiService.getCurrentUserPlaylists("Bearer " + token, 10, 0).enqueue(new Callback<PlaylistResponse>() {
            @Override
            public void onResponse(Call<PlaylistResponse> call, Response<PlaylistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Playlist> items = response.body().items;
                    if (items != null && !items.isEmpty()) {
                        playlistList.clear();
                        for (Playlist p : items) {
                            String imgUrl = (p.images != null && p.images.length > 0) ? p.images[0].url : "";
                            playlistList.add(new PlaylistModel(imgUrl, p.name, "Spotify Playlist"));
                        }
                        playlistAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Spotify playlists loaded.");
                    } else {
                        Log.d(TAG, "Spotify returned 0 playlists.");
                        addPlaceholderPlaylists();
                    }
                } else {
                    Log.e(TAG, "Spotify API Error: " + response.code());
                    Toast.makeText(HomeActivity.this, "Spotify Error: " + response.code(), Toast.LENGTH_LONG).show();
                    addPlaceholderPlaylists();
                }
            }

            @Override
            public void onFailure(Call<PlaylistResponse> call, Throwable t) {
                Log.e(TAG, "Network Failure", t);
                Toast.makeText(HomeActivity.this, "Network error fetching Spotify data", Toast.LENGTH_SHORT).show();
                addPlaceholderPlaylists();
            }
        });
    }

    private void setupPlaylistRecycler() {
        RecyclerView recyclerView = findViewById(R.id.playlistRecycler);
        playlistAdapter = new PlaylistAdapter(this, playlistList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(playlistAdapter);
        
        if (recyclerView.getOnFlingListener() == null) {
            new PagerSnapHelper().attachToRecyclerView(recyclerView);
        }
    }

    private void setupJamRecycler() {
        RecyclerView recyclerView = findViewById(R.id.jamRecycler);
        if (recyclerView == null) return;

        List<JamModel> list = new ArrayList<>();
        list.add(new JamModel(R.drawable.mybeat, "MY BEAT", "shivam"));
        list.add(new JamModel(R.drawable.trulyyours, "TRULY YOURSS", "ERRIC BELLINGER"));

        JamAdapter adapter = new JamAdapter(this, list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
    }
}
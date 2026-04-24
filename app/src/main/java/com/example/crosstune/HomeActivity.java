package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeActivity extends AppCompatActivity {

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
        // 1. Initialize SessionManager

        // 2. Initialize Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(SpotifyApiService.class);

        // 3. Setup UI
        setupPlaylistRecycler();
        setupJamRecycler();

        // 4. Navigation
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

        // 1. Get token from the "Disk" storage
        String token = AppState.sessionManager.getSpotifyToken();

        // 2. Check the "Global" flag
//        if (token != null && !AppState.isPlaylistDataFetched) {
//
//        }
        fetchSpotifyPlaylists(token);
    }

    private void fetchSpotifyPlaylists(String token) {
        apiService.getCurrentUserPlaylists("Bearer " + token, 10, 0).enqueue(new Callback<PlaylistResponse>() {
            @Override
            public void onResponse(Call<PlaylistResponse> call, Response<PlaylistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    playlistList.clear();
                    for (Playlist p : response.body().items) {
                        String imgUrl = (p.images != null && p.images.length > 0) ? p.images[0].url : "";
                        playlistList.add(new PlaylistModel(imgUrl, p.name, "Spotify Playlist"));
                    }
                    playlistAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(HomeActivity.this, "Session Expired", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PlaylistResponse> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPlaylistRecycler() {
        RecyclerView recyclerView = findViewById(R.id.playlistRecycler);
        playlistAdapter = new PlaylistAdapter(this, playlistList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(playlistAdapter);

        new PagerSnapHelper().attachToRecyclerView(recyclerView);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int center = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    int childCenter = (child.getLeft() + child.getRight()) / 2;
                    float distance = Math.abs(center - childCenter);
                    float factor = Math.max(0.92f, 1.02f - (distance / center) * 0.15f);
                    child.setScaleX(factor); child.setScaleY(factor);
                    child.setAlpha(Math.max(0.8f, factor));
                }
            }
        });
    }

    private void setupJamRecycler() {
        RecyclerView recyclerView = findViewById(R.id.jamRecycler);
        if (recyclerView == null) return;

        List<JamModel> list = new ArrayList<>();
        list.add(new JamModel(R.drawable.mybeat, "MY BEAT", "shivam"));
        list.add(new JamModel(R.drawable.trulyyours, "TRULY YOURSS", "ERRIC BELLINGER"));

        // Use JamAdapter which does NOT have a click listener to PlaylistActivity
        JamAdapter adapter = new JamAdapter(this, list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int center = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    int childCenter = (child.getLeft() + child.getRight()) / 2;
                    float distance = Math.abs(center - childCenter);
                    float factor = Math.max(0.95f, 1.02f - (distance / center) * 0.1f);
                    child.setScaleX(factor);
                    child.setScaleY(factor);
                    child.setAlpha(Math.max(0.85f, factor));
                }
            }
        });
    }
}
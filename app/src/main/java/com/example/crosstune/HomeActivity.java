package com.example.crosstune;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Initialize Recyclers
        setupPlaylistRecycler();
        setupJamRecycler();

        // 2. Profile Navigation
        ImageView profileIcon = findViewById(R.id.ic_profile);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // 3. Library Navigation (Fixed ID)
        ImageView libraryBtn = findViewById(R.id.btn_library_nav);
        if (libraryBtn != null) {
            libraryBtn.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, LibraryActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupPlaylistRecycler() {
        RecyclerView recyclerView = findViewById(R.id.playlistRecycler);
        if (recyclerView == null) return;

        List<PlaylistModel> list = new ArrayList<>();
        list.add(new PlaylistModel(R.drawable.trulyyours, "Truly Yours", "Eric Bellinger"));
        list.add(new PlaylistModel(R.drawable.dollaz_on_my_head, "Dollaz on my head", "Gunna"));
        list.add(new PlaylistModel(R.drawable.carsick, "Car sick", "Gunna"));
        list.add(new PlaylistModel(R.drawable.mybeat, "My Beat", "Shivam"));

        // Use standard adapter that navigates to PlaylistActivity
        PlaylistAdapter adapter = new PlaylistAdapter(this, list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        SnapHelper snapHelper = new PagerSnapHelper();
        if (recyclerView.getOnFlingListener() == null) {
            snapHelper.attachToRecyclerView(recyclerView);
        }

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int center = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    int childCenter = (child.getLeft() + child.getRight()) / 2;
                    float distance = Math.abs(center - childCenter);
                    float factor = Math.max(0.92f, 1.02f - (distance / center) * 0.15f);
                    child.setScaleX(factor);
                    child.setScaleY(factor);
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
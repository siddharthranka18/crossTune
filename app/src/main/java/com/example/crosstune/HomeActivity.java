package com.example.crosstune;

import android.os.Bundle;
import android.view.View;

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

        setupPlaylistRecycler();
        setupJamRecycler();
    }

    private void setupPlaylistRecycler() {
        RecyclerView recyclerView = findViewById(R.id.playlistRecycler);
        List<PlaylistModel> list = new ArrayList<>();
        list.add(new PlaylistModel(R.drawable.trulyyours, "Truly Yours", "Eric Bellinger"));
        list.add(new PlaylistModel(R.drawable.dollaz_on_my_head, "Dollaz on my head", "Gunna"));
        list.add(new PlaylistModel(R.drawable.carsick, "Car sick", "Gunna"));
        list.add(new PlaylistModel(R.drawable.mybeat, "My Beat", "Shivam"));

        PlaylistAdapter adapter = new PlaylistAdapter(this, list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int center = recyclerView.getWidth() / 2;
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    int childCenter = (child.getLeft() + child.getRight()) / 2;
                    float distance = Math.abs(center - childCenter);

                    // Keep scaling subtle so side cards stay visible and images don't over-expand.
                    float factor = Math.max(0.92f, 1.02f - (distance / center) * 0.15f);

                    child.setScaleX(factor);
                    child.setScaleY(factor);

                    float translationY = -((factor - 0.92f) / 0.1f) * 8f;
                    child.setTranslationY(translationY);

                    child.setAlpha(Math.max(0.8f, factor));
                }
            }
        });
    }

    private void setupJamRecycler() {
        RecyclerView recyclerView = findViewById(R.id.jamRecycler);
        List<PlaylistModel> list = new ArrayList<>();
        list.add(new PlaylistModel(R.drawable.mybeat, "MY BEAT", "shivam"));
        list.add(new PlaylistModel(R.drawable.trulyyours, "TRULY YOURSS", "ERRIC BELLINGER"));

        PlaylistAdapter adapter = new PlaylistAdapter(this, list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
    }
}

package com.example.crosstune;

import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        RecyclerView recyclerView= findViewById(R.id.playlistRecycler);
        List<PlaylistModel> list = new ArrayList<>();
        list.add(new PlaylistModel(R.drawable.dollaz_on_my_head,"Dollaz on my head","Gunna"));
        list.add(new PlaylistModel(R.drawable.carsick,"Car sick","Gunna"));
        list.add(new PlaylistModel(R.drawable.mybeat,"My Beat","Shivam"));
        list.add(new PlaylistModel(R.drawable.trulyyours,"Truly Yours","Eric Bellinger"));
        PlaylistAdapter adapter= new PlaylistAdapter(this,list);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        );
        recyclerView.setAdapter(adapter);
    }
}